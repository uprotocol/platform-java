package org.monora.uprotocol.core.persistence;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.monora.uprotocol.core.CommunicationBridge;
import org.monora.uprotocol.core.TransportSession;
import org.monora.uprotocol.core.io.StreamDescriptor;
import org.monora.uprotocol.core.protocol.Client;
import org.monora.uprotocol.core.protocol.ClientAddress;
import org.monora.uprotocol.core.spec.v1.Keyword;
import org.monora.uprotocol.core.transfer.TransferItem;

import javax.activation.MimetypesFileTypeMap;
import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * This class is responsible for making changes persistent.
 */
public interface PersistenceProvider
{
    /**
     * The item is in pending state. It can also have a temporary location.
     * <p>
     * In the case of incoming files, if you force set this state, keep the temporary file location as we can later
     * restart this item, resuming from where it was left.
     * <p>
     * This is the only state that will feed the {@link #getFirstReceivableItem(long)} invocations.
     */
    int STATE_PENDING = 0;

    /**
     * The item is invalidated temporarily. The reason for that may be an unexpected connection that could not be
     * recovered.
     * <p>
     * The user can reset this state to {@link #STATE_PENDING}.
     */
    int STATE_INVALIDATED_TEMPORARILY = 1;

    /**
     * The item is invalidated indefinitely because its length has changed or the file no longer exits.
     * <p>
     * The user should <b>NOT</b> be able to remove this flag, setting a valid state such as {@link #STATE_PENDING}.
     */
    int STATE_INVALIDATED_STICKY = 2;

    /**
     * The item is in progress, that is, it is either being received or sent.
     * <p>
     * The user can reset this state to {@link #STATE_PENDING} as we may not have a chance to do it ourselves in the
     * case of a crash.
     */
    int STATE_IN_PROGRESS = 3;

    /**
     * The item is done.
     */
    int STATE_DONE = 4;

    /**
     * Type map provides the mime-type for filenames and file objects.
     */
    MimetypesFileTypeMap typeMap = new MimetypesFileTypeMap();

    /**
     * Accept the request for invalidation of a client's credentials.
     * <p>
     * When implementing, removal of the given client's certificate should be enough.
     *
     * @param client Of whose keys will be approved.
     * @return True if there were a request and now approved, or false there were no request.
     * @see Client#getClientCertificate()
     * @see #hasRequestForInvalidationOfCredentials(String)
     * @see #saveRequestForInvalidationOfCredentials(String)
     */
    boolean approveInvalidationOfCredentials(Client client);

    /**
     * Broadcast the awaiting operation reports.
     * <p>
     * This will be invoked whenever there is a change in the persistence database.
     * <p>
     * Notice that you should not report every change as soon as it happens. That can lead to performance drawbacks.
     * We'd like to do them whenever necessary or whenever the right amount of time passes.
     * <p>
     * If your implementation has a built-in support for notifying listeners, then you can skip using method
     * altogether.
     */
    void broadcast();

    /**
     * Convert this client into {@link JSONObject}.
     * <p>
     * This should only be invoked when communicating with remote.
     * <p>
     * Because this is either invoked by the {@link TransportSession} or {@link CommunicationBridge}, you should already
     * know the client that you are talking to.
     * <p>
     * The PIN will usually be unavailable, so it is okay to provide '0'. It should be available through connectionless
     * means like QR Code or manual-entry. It is used to bypass the security mechanisms so the communication can happen
     * seamlessly.
     * <p>
     * The remote client can gather the PIN from {@link PersistenceProvider#getNetworkPin()} which should be the
     * same until the remote invokes {@link PersistenceProvider#revokeNetworkPin()}, corresponding you sending the
     * right PIN and consuming it.
     *
     * @param pin The PIN to bypass errors like not matching keys. This will also flag this client as trusted.
     * @return The JSON object
     * @throws JSONException If the creation of the JSON object fails for some reason.
     */
    default JSONObject clientAsJson(int pin) throws JSONException
    {
        Client client = getClient();
        JSONObject object = new JSONObject()
                .put(Keyword.CLIENT_UID, client.getClientUid())
                .put(Keyword.CLIENT_MANUFACTURER, client.getClientManufacturer())
                .put(Keyword.CLIENT_PRODUCT, client.getClientProduct())
                .put(Keyword.CLIENT_NICKNAME, client.getClientNickname())
                .put(Keyword.CLIENT_TYPE, client.getClientType())
                .put(Keyword.CLIENT_VERSION_CODE, client.getClientVersionCode())
                .put(Keyword.CLIENT_VERSION_NAME, client.getClientVersionName())
                .put(Keyword.CLIENT_PROTOCOL_VERSION, client.getClientProtocolVersion())
                .put(Keyword.CLIENT_PROTOCOL_VERSION_MIN, client.getClientProtocolVersionMin())
                .put(Keyword.CLIENT_PIN, pin);

        byte[] clientAvatar = getClientPicture();
        if (clientAvatar.length > 0)
            object.put(Keyword.CLIENT_PICTURE, Base64.getEncoder().encodeToString(clientAvatar));

        return object;
    }

    /**
     * Check whether the transfer is known to us.
     *
     * @param groupId To check.
     * @return True there is matching data for the given transfer id.
     */
    boolean containsTransfer(long groupId);

    /**
     * Create client address instance.
     *
     * @param address To which this will be pointing.
     * @return The client address instance.
     */
    ClientAddress createClientAddressFor(InetAddress address);

    /**
     * Request from the factory to create an empty {@link Client} instance.
     *
     * @return The client instance.
     */
    Client createClient();

    /**
     * Create a client instance using the given unique identifier.
     * <p>
     * The resulting {@link Client} instance is not ready for use. To make it so, call {@link #sync(Client)}.
     *
     * @param uid The client's unique identifier.
     * @return The client instance.
     */
    Client createClientFor(String uid);

    /**
     * Create a transfer item instance for the given parameters.
     *
     * @param groupId   Points to {@link TransferItem#getItemGroupId()}.
     * @param id        Points to {@link TransferItem#getItemId()}.
     * @param name      Points to {@link TransferItem#getItemName()}.
     * @param mimeType  Points to {@link TransferItem#getItemMimeType()}.
     * @param size      Points to {@link TransferItem#getItemSize()}.
     * @param directory Points to {@link TransferItem#getItemDirectory()}.
     * @param type      Points to {@link TransferItem#getItemType()}
     * @return The transfer item instance.
     */
    TransferItem createTransferItemFor(long groupId, long id, String name, String mimeType, long size, String directory,
                                       TransferItem.Type type);

    /**
     * Returns this client's certificate.
     * <p>
     * The returned value should stay persistent.
     *
     * @return This client's certificate.
     */
    X509Certificate getCertificate();

    /**
     * This will return the {@link Client} instance representing this client.
     *
     * @return The client instance.
     */
    Client getClient();

    /**
     * This client's user-friendly optional nickname.
     *
     * @return The nickname.
     */
    String getClientNickname();

    /**
     * Returns the avatar for this client.
     *
     * @return The bitmap data for the avatar if exists, or zero-length byte array if it doesn't.
     */
    byte[] getClientPicture();

    /**
     * Returns the given client's picture.
     * <p>
     * If the given client's {@link Client#getClientUid()} is equal to {@link #getClientUid()}, this should return this
     * client's picture.
     *
     * @param client For which the avatar will be provided.
     * @return The bitmap data for the avatar if exists, or zero-length byte array if it doesn't.
     */
    byte[] getClientPictureFor(Client client);

    /**
     * This should return the unique identifier for this client. It should be both unique and persistent.
     * <p>
     * It is not meant to change.
     *
     * @return The unique identifier for this client.
     */
    String getClientUid();

    /**
     * This will return the descriptor that points to the file that is received or sent.
     * <p>
     * For instance, this can be a file descriptor or a network stream of which only the name, size and location are
     * known.
     *
     * @param transferItem For which the descriptor will be generated.
     * @return The generated descriptor.
     * @see #openInputStream(StreamDescriptor)
     * @see #openOutputStream(StreamDescriptor)
     */
    StreamDescriptor getDescriptorFor(TransferItem transferItem);

    /**
     * This will return the first valid item that that this side can receive.
     *
     * @param groupId Points to {@link TransferItem#getItemGroupId()}.
     * @return The transfer receivable item or null if there are none.
     */
    TransferItem getFirstReceivableItem(long groupId);

    /**
     * This method is invoked when there is a new connection to the server.
     * <p>
     * This provides the PIN which may be delivered to the remote client via a QR code, or by other
     * means to allow it to have instant access to this client.
     * <p>
     * The code should persist until {@link #revokeNetworkPin()} is invoked.
     *
     * @return the PIN that will change after being revoked.
     * @see #revokeNetworkPin()
     */
    int getNetworkPin();

    /**
     * The private key that belongs to this client.
     *
     * @return The private key.
     */
    PrivateKey getPrivateKey();

    /**
     * The public key that belongs to this client.
     *
     * @return The public key.
     */
    PublicKey getPublicKey();

    /**
     * Generates a cryptographically strong random number.
     * <p>
     * This method will be invoked by the default {@link #getSSLContextFor(Client)} method.
     *
     * @return The secure random number instance.
     * @see #getSSLContextFor(Client)
     */
    default SecureRandom getSecureRandom()
    {
        return new SecureRandom();
    }

    /**
     * Creates the SSL context for the given client.
     * <p>
     * If the certificate does not exist, a custom TrustStore will be generated.
     *
     * @param client For which the context will be generated.
     * @return The SSL context.
     */
    default SSLContext getSSLContextFor(Client client)
    {
        try {
            // Get this client's private key
            PrivateKey privateKey = getPrivateKey();
            char[] password = new char[0];

            // Setup keystore
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setKeyEntry("key", privateKey, password, new Certificate[]{getCertificate()});

            if (client.getClientCertificate() != null)
                keyStore.setCertificateEntry(client.getClientUid(), client.getClientCertificate());

            // Setup key manager factory
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, password);

            TrustManager[] trustManagers;

            if (client.getClientCertificate() == null) {
                // Set up custom trust manager if we don't have the certificate for the peer.
                X509TrustManager trustManager = new X509TrustManager()
                {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers()
                    {
                        return new X509Certificate[0];
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] certs, String authType)
                    {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType)
                    {
                    }
                };

                trustManagers = new TrustManager[]{trustManager};
            } else {
                // Set up the default trust manager if we already have the certificate for the peer.
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                        TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(keyStore);

                trustManagers = trustManagerFactory.getTrustManagers();
            }

            // Newer TLS versions are only supported on API 16+
            SSLContext tlsContext = SSLContext.getInstance("TLSv1");
            tlsContext.init(keyManagerFactory.getKeyManagers(), trustManagers, getSecureRandom());

            return tlsContext;
        } catch (Exception e) {
            // TODO: 1/7/21 Should this throw custom exceptions?
            throw new RuntimeException("Could not create a secure socket context.");
        }
    }

    /**
     * Check whether the given client already has a request for invalidation.
     *
     * @param clientUid That sent the request.
     * @return True if there is a pending request.
     * @see #saveRequestForInvalidationOfCredentials(String)
     * @see #approveInvalidationOfCredentials(Client)
     */
    boolean hasRequestForInvalidationOfCredentials(String clientUid);

    /**
     * Load transfer item for the given parameters.
     *
     * @param clientUid Owning the item.
     * @param groupId   Points to {@link TransferItem#getItemGroupId()}
     * @param id        Points to {@link TransferItem#getItemId()}.
     * @param type      Specifying whether this is an incoming or outgoing operation.
     * @return The transfer item that points to the given parameters or null if there is no match.
     * @throws PersistenceException When the given parameters don't point to a valid item.
     */
    TransferItem loadTransferItem(String clientUid, long groupId, long id, TransferItem.Type type)
            throws PersistenceException;

    /**
     * Open the input stream for the given descriptor.
     *
     * @param descriptor Of which the input stream will be opened.
     * @return The open input stream.
     * @throws IOException If an IO error occurs.
     */
    InputStream openInputStream(StreamDescriptor descriptor) throws IOException;

    /**
     * Open the output stream for this descriptor.
     *
     * @param descriptor Of which the output stream will be opened.
     * @return The open output stream.
     * @throws IOException If an IO error occurs.
     */
    OutputStream openOutputStream(StreamDescriptor descriptor) throws IOException;

    /**
     * Revoke the current valid network PIN.
     *
     * @see #getNetworkPin()
     */
    void revokeNetworkPin();

    /**
     * Save this client in the persistence database.
     * <p>
     * Do not hold any duplicates, and verify it using the {@link Client#getClientUid()} field.
     *
     * @param client To save.
     */
    void save(Client client);

    /**
     * Save this client address in the persistence database.
     *
     * @param clientAddress To save.
     */
    void save(ClientAddress clientAddress);

    /**
     * Save this transfer item in the persistence database.
     * <p>
     * Note: ensure there are no duplicates.
     *
     * @param clientUid That owns the item.
     * @param item      To save.
     */
    void save(String clientUid, TransferItem item);

    /**
     * Save all the items in the given list.
     *
     * @param clientUid That owns the items.
     * @param itemList  To save.
     */
    void save(String clientUid, List<? extends TransferItem> itemList);

    /**
     * Save the client's picture.
     * <p>
     * This will always be invoked whether or not the bitmap is empty.
     *
     * @param clientUid The client that the picture belongs to.
     * @param bitmap    The bitmap data for the picture.
     */
    void saveClientPicture(String clientUid, byte[] bitmap);

    /**
     * Invoken when a known clients sends invalid credentials and now cannot connect.
     * <p>
     * You can show the error to the user so that they can decide for themselves.
     * <p>
     * Finally, you should remove the existing certificate for the given client uid.
     *
     * @param clientUid That wants key invalidation.
     * @see #hasRequestForInvalidationOfCredentials(String)
     * @see #approveInvalidationOfCredentials(Client)
     */
    void saveRequestForInvalidationOfCredentials(String clientUid);

    /**
     * Change the state of the given item.
     * <p>
     * Note: this should set the state but should not save it since saving it is spared for
     * {@link #save(String, TransferItem)}.
     *
     * @param clientUid That owns the copy of the 'item'.
     * @param item      Of which the given state will be applied.
     * @param state     The level of invalidation.
     * @param e         The nullable additional exception cause this state.
     * @see #STATE_PENDING
     * @see #STATE_INVALIDATED_TEMPORARILY
     * @see #STATE_INVALIDATED_STICKY
     * @see #STATE_IN_PROGRESS
     * @see #STATE_DONE
     */
    void setState(String clientUid, TransferItem item, int state, Exception e);

    /**
     * Sync the client with the persistence database.
     *
     * @param client To sync.
     * @throws PersistenceException When there is no client associated with the unique identifier
     *                              {@link Client#getClientUid()}.
     */
    void sync(Client client) throws PersistenceException;

    /**
     * Transform a given {@link TransferItem} list into its {@link JSONArray} equivalent.
     * <p>
     * The resulting {@link JSONArray} can be fed to {@link CommunicationBridge#requestFileTransfer(long, List)},
     * to start a file transfer operation.
     * <p>
     * You can have the same JSON data back using {@link #toTransferItemList(long, String)}.
     *
     * @param transferItemList To convert.
     * @return The JSON equivalent of the same list.
     */
    default JSONArray toJson(List<TransferItem> transferItemList)
    {
        JSONArray jsonArray = new JSONArray();

        for (TransferItem transferItem : transferItemList) {
            JSONObject json = new JSONObject()
                    .put(Keyword.TRANSFER_ID, transferItem.getItemId())
                    .put(Keyword.INDEX_FILE_NAME, transferItem.getItemName())
                    .put(Keyword.INDEX_FILE_SIZE, transferItem.getItemSize())
                    .put(Keyword.INDEX_FILE_MIME, transferItem.getItemMimeType());

            if (transferItem.getItemDirectory() != null)
                json.put(Keyword.INDEX_DIRECTORY, transferItem.getItemDirectory());

            jsonArray.put(json);
        }

        return jsonArray;
    }

    /**
     * This will inflate the given JSON data that was received from the remote to make it consumable as a collection.
     *
     * @param groupId   That the JSON data contains items of.
     * @param jsonArray The transfer list that is going to inflated.
     * @return The list of items inflated from the JSON data.
     * @throws JSONException If the JSON data is corrupted or has missing/mismatch values.
     */
    default List<TransferItem> toTransferItemList(long groupId, String jsonArray) throws JSONException
    {
        JSONArray json = new JSONArray(jsonArray);
        List<TransferItem> transferItemList = new ArrayList<>();

        if (json.length() > 0) {
            for (int i = 0; i < json.length(); i++) {
                JSONObject jsonObject = json.getJSONObject(i);
                String directory = jsonObject.has(Keyword.INDEX_DIRECTORY)
                        ? jsonObject.getString(Keyword.INDEX_DIRECTORY) : null;
                transferItemList.add(createTransferItemFor(groupId, jsonObject.getLong(Keyword.TRANSFER_ID),
                        jsonObject.getString(Keyword.INDEX_FILE_NAME), jsonObject.getString(Keyword.INDEX_FILE_MIME),
                        jsonObject.getLong(Keyword.INDEX_FILE_SIZE), directory, TransferItem.Type.INCOMING));
            }
        }

        return transferItemList;
    }
}
