package org.monora.uprotocol.core.persistence;

import org.apache.commons.codec.binary.Base64;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.monora.uprotocol.core.CommunicationBridge;
import org.monora.uprotocol.core.TransportSession;
import org.monora.uprotocol.core.io.ClientPicture;
import org.monora.uprotocol.core.io.StreamDescriptor;
import org.monora.uprotocol.core.protocol.Client;
import org.monora.uprotocol.core.protocol.ClientAddress;
import org.monora.uprotocol.core.protocol.ClientType;
import org.monora.uprotocol.core.spec.v1.Keyword;
import org.monora.uprotocol.core.transfer.TransferItem;

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
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
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
    boolean approveInvalidationOfCredentials(@NotNull Client client);

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
    default @NotNull JSONObject clientAsJson(int pin) throws JSONException
    {
        Client client = getClient();
        JSONObject object = new JSONObject()
                .put(Keyword.CLIENT_UID, client.getClientUid())
                .put(Keyword.CLIENT_MANUFACTURER, client.getClientManufacturer())
                .put(Keyword.CLIENT_PRODUCT, client.getClientProduct())
                .put(Keyword.CLIENT_NICKNAME, client.getClientNickname())
                .put(Keyword.CLIENT_TYPE, client.getClientType().getProtocolValue())
                .put(Keyword.CLIENT_VERSION_CODE, client.getClientVersionCode())
                .put(Keyword.CLIENT_VERSION_NAME, client.getClientVersionName())
                .put(Keyword.CLIENT_PROTOCOL_VERSION, client.getClientProtocolVersion())
                .put(Keyword.CLIENT_PROTOCOL_VERSION_MIN, client.getClientProtocolVersionMin())
                .put(Keyword.CLIENT_PIN, pin);

        ClientPicture clientPicture = getClientPicture();
        if (clientPicture.hasPicture()) {
            object.put(Keyword.CLIENT_PICTURE, Base64.encodeBase64String(clientPicture.getPictureData()));
            object.put(Keyword.CLIENT_PICTURE_CHECKSUM, clientPicture.getPictureChecksum());
        }

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
     * @param address   To which this will be pointing.
     * @param clientUid That owns the address.
     * @return The client address instance.
     */
    @NotNull ClientAddress createClientAddressFor(@NotNull InetAddress address, @NotNull String clientUid);

    /**
     * Create a client instance for the given input.
     *
     * @param uid                Points to {@link Client#getClientUid()}.
     * @param nickname           Points to {@link Client#getClientNickname()}.
     * @param manufacturer       Points to {@link Client#getClientManufacturer()}.
     * @param product            Points to {@link Client#getClientProduct()}.
     * @param type               Points to {@link Client#getClientType()}.
     * @param versionName        Points to {@link Client#getClientVersionName()}
     * @param versionCode        Points to {@link Client#getClientVersionCode()}.
     * @param protocolVersion    Points to {@link Client#getClientProtocolVersion()}.
     * @param protocolVersionMin Points {@link Client#getClientProtocolVersionMin()}.
     * @return The client instance.
     * @see #persist(Client, boolean)
     * @see #getClientFor(String)
     */
    @NotNull Client createClientFor(@NotNull String uid, @NotNull String nickname, @NotNull String manufacturer,
                                    @NotNull String product, @NotNull ClientType type, @NotNull String versionName,
                                    int versionCode, int protocolVersion, int protocolVersionMin);

    /**
     * todo: Should this really exist? This user can avoid using it. The benefit may be to use it as a factory.
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
    @NotNull TransferItem createTransferItemFor(long groupId, long id, @NotNull String name, @NotNull String mimeType,
                                                long size, @Nullable String directory, @NotNull TransferItem.Type type);

    /**
     * Returns this client's certificate.
     * <p>
     * The returned value should stay persistent.
     *
     * @return This client's certificate.
     */
    @NotNull X509Certificate getCertificate();

    /**
     * This will return the {@link Client} instance representing this client.
     *
     * @return The client instance.
     */
    @NotNull Client getClient();

    /**
     * Finds and returns a known client using its unique identifier.
     *
     * @param uid To associate with the client.
     * @return The associated client or null if there weren't one.
     */
    @Nullable Client getClientFor(@NotNull String uid);

    /**
     * This client's user-friendly optional nickname.
     *
     * @return The nickname.
     */
    @NotNull String getClientNickname();

    /**
     * Returns the picture for this client.
     *
     * @return The picture object that contains the necessary info about the picture.
     */
    @NotNull ClientPicture getClientPicture();

    /**
     * Returns the non-null picture instance owned by the given client.
     *
     * If a picture doesn't exist, this should return an empty picture.
     *
     * @param clientUid For which the avatar will be provided.
     * @return The picture representing class.
     * @see ClientPicture#newEmptyInstance(String)
     */
    @NotNull ClientPicture getClientPictureFor(@NotNull String clientUid);

    /**
     * This should return the unique identifier for this client. It should be both unique and persistent.
     * <p>
     * It is not meant to change.
     *
     * @return The unique identifier for this client.
     */
    @NotNull String getClientUid();

    /**
     * This will return the descriptor that points to the file that is received or sent.
     * <p>
     * For instance, this can be a file descriptor or a network stream of which only the name, size and location are
     * known.
     *
     * @param transferItem For which the descriptor will be generated.
     * @return The generated descriptor.
     * @throws IOException When this fails to create a descriptor for this transfer item.
     * @see #openInputStream(StreamDescriptor)
     * @see #openOutputStream(StreamDescriptor)
     */
    @NotNull StreamDescriptor getDescriptorFor(@NotNull TransferItem transferItem) throws IOException;

    /**
     * This will return the first valid item that that this side can receive.
     *
     * @param groupId Points to {@link TransferItem#getItemGroupId()}.
     * @return The transfer receivable item or null if there are none.
     */
    @Nullable TransferItem getFirstReceivableItem(long groupId);

    /**
     * This method is invoked when there is a new connection to the server.
     * <p>
     * This provides the PIN which may be delivered to the remote client via a QR code, or by other
     * means to allow it to have instant access to this client.
     * <p>
     * The code should persist until {@link #revokeNetworkPin()} is invoked.
     *
     * @return The PIN that will change after being revoked.
     * @see #revokeNetworkPin()
     */
    int getNetworkPin();

    /**
     * The private key that belongs to this client.
     *
     * @return The private key.
     */
    @NotNull PrivateKey getPrivateKey();

    /**
     * The public key that belongs to this client.
     *
     * @return The public key.
     */
    @NotNull PublicKey getPublicKey();

    /**
     * Generates a cryptographically strong random number.
     * <p>
     * This method will be invoked by the default {@link #getSSLContextFor(Client)} method.
     *
     * @return The secure random number instance.
     * @see #getSSLContextFor(Client)
     */
    default @NotNull SecureRandom getSecureRandom()
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
     * @throws CertificateException If the generation of the context fails.
     */
    default @NotNull SSLContext getSSLContextFor(@NotNull Client client) throws CertificateException
    {
        try {
            // Get this client's private key
            PrivateKey privateKey = getPrivateKey();
            char[] password = new char[0];

            // Setup keystore
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setKeyEntry("key", privateKey, password, new Certificate[]{getCertificate()});

            if (client.getClientCertificate() != null) {
                keyStore.setCertificateEntry(client.getClientUid(), client.getClientCertificate());
            }

            // Setup key manager factory
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, password);

            TrustManager[] trustManagers;

            if (client.getClientCertificate() == null) {
                // Set up custom trust manager if we don't have the certificate for the peer.
                X509TrustManager trustManager = new X509TrustManager()
                {
                    public java.security.cert.X509Certificate @NotNull [] getAcceptedIssuers()
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
        } catch (CertificateException e) {
            throw e;
        } catch (Exception e) {
            throw new CertificateException("Could not create a secure socket context.", e);
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
    boolean hasRequestForInvalidationOfCredentials(@NotNull String clientUid);

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
    @NotNull TransferItem loadTransferItem(@NotNull String clientUid, long groupId, long id,
                                           @NotNull TransferItem.Type type) throws PersistenceException;

    /**
     * Open the input stream for the given descriptor.
     *
     * @param descriptor Of which the input stream will be opened.
     * @return The open input stream.
     * @throws IOException If an IO error occurs.
     */
    @NotNull InputStream openInputStream(@NotNull StreamDescriptor descriptor) throws IOException;

    /**
     * Open the output stream for this descriptor.
     *
     * @param descriptor Of which the output stream will be opened.
     * @return The open output stream.
     * @throws IOException If an IO error occurs.
     */
    @NotNull OutputStream openOutputStream(@NotNull StreamDescriptor descriptor) throws IOException;

    /**
     * Insert or update this client in the persistence database.
     * <p>
     * Do not hold any duplicates, and verify it using the {@link Client#getClientUid()} field.
     *
     * @param client   To save.
     * @param updating True if this should update the existing rows instead of inserting a new one.
     */
    void persist(@NotNull Client client, boolean updating);

    /**
     * Insert this client address in the persistence database, replacing the old instance if exists.
     * <p>
     * {@link ClientAddress} does not support updating. The idea is to replace it all the time. The reason is an
     * address may belong to different clients in a short amount of time.
     *
     * @param clientAddress To save.
     */
    void persist(@NotNull ClientAddress clientAddress);

    /**
     * Update this transfer item in the persistence database.
     * <p>
     * NOTE: This should only update and not try to insert a new row.
     *
     * @param clientUid That owns the item.
     * @param item      To save.
     */
    void persist(@NotNull String clientUid, @NotNull TransferItem item);

    /**
     * Insert all the items into the persistence database.
     * <p>
     * This should avoid updating objects.
     *
     * @param clientUid That owns the items.
     * @param itemList  To save.
     */
    void persist(@NotNull String clientUid, @NotNull List<? extends @NotNull TransferItem> itemList);

    /**
     * The latest picture that belongs to a client.
     * <p>
     * The invocation of this method mean the picture is new and should be saved.
     *
     * @param clientPicture That contains the latest picture details.
     */
    void persistClientPicture(@NotNull ClientPicture clientPicture);

    /**
     * Revoke the current valid network PIN.
     *
     * @see #getNetworkPin()
     */
    void revokeNetworkPin();

    /**
     * Invoke when a known clients sends invalid credentials and now cannot connect.
     * <p>
     * You can show the error to the user so that they can decide for themselves.
     * <p>
     * Finally, you should remove the existing certificate for the given client uid.
     *
     * @param clientUid That wants key invalidation.
     * @see #hasRequestForInvalidationOfCredentials(String)
     * @see #approveInvalidationOfCredentials(Client)
     */
    void saveRequestForInvalidationOfCredentials(@NotNull String clientUid);

    /**
     * Change the state of the given item.
     * <p>
     * Note: this should set the state but should not update it since saving it is spared for
     * {@link #persist(String, TransferItem)} unless the state is held on a different location.
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
    void setState(@NotNull String clientUid, @NotNull TransferItem item, int state, @Nullable Exception e);

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
    default @NotNull JSONArray toJson(@NotNull List<@NotNull TransferItem> transferItemList)
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
    default @NotNull List<@NotNull TransferItem> toTransferItemList(long groupId, @NotNull String jsonArray)
            throws JSONException
    {
        // TODO: 2/26/21 Should json array to transfer item list function exist?
        JSONArray json = new JSONArray(jsonArray);
        List<TransferItem> transferItemList = new ArrayList<>();

        if (json.length() > 0) {
            for (int i = 0; i < json.length(); i++) {
                JSONObject jsonObject = json.getJSONObject(i);
                String directory = jsonObject.has(Keyword.INDEX_DIRECTORY)
                        ? jsonObject.getString(Keyword.INDEX_DIRECTORY) : null;
                transferItemList.add(createTransferItemFor(groupId, jsonObject.getLong(Keyword.TRANSFER_ID),
                        jsonObject.getString(Keyword.INDEX_FILE_NAME), jsonObject.getString(Keyword.INDEX_FILE_MIME),
                        jsonObject.getLong(Keyword.INDEX_FILE_SIZE), directory, TransferItem.Type.Incoming));
            }
        }

        return transferItemList;
    }
}
