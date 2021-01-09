package org.monora.uprotocol.core.persistence;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.monora.uprotocol.core.CommunicationBridge;
import org.monora.uprotocol.core.TransportSession;
import org.monora.uprotocol.core.network.Client;
import org.monora.uprotocol.core.network.DeviceAddress;
import org.monora.uprotocol.core.network.TransferItem;
import org.monora.uprotocol.core.spec.alpha.Keyword;

import javax.activation.MimetypesFileTypeMap;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.security.PrivateKey;
import java.security.PublicKey;
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
     * The item is done, that is, it has been received or sent for a given device.
     */
    int STATE_DONE = 4;

    /**
     * Type map provides the mime-type for filenames and file objects.
     */
    MimetypesFileTypeMap typeMap = new MimetypesFileTypeMap();

    /**
     * Accept the request for invalidation of credentials of a device.
     * <p>
     * Removing the certificate for the given device should be enough.
     *
     * @param client Of whose keys will be approved.
     * @return True if there were a request and now approved, or false there were no request.
     * @see Client#certificate
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
     * Check whether the transfer is known to us.
     *
     * @param transferId To check.
     * @return True there is matching data for the given transfer id.
     */
    boolean containsTransfer(long transferId);

    /**
     * Create device address instance.
     *
     * @param address At which the device address will be pointing.
     * @return The device address instance.
     */
    DeviceAddress createDeviceAddressFor(InetAddress address);

    /**
     * Request from the factory to create an empty {@link Client} instance.
     *
     * @return The device instance.
     */
    Client createDevice();

    /**
     * Create a device instance using the given unique identifier.
     * <p>
     * The resulting {@link Client} instance is not ready for use. To make it so, call {@link #sync(Client)}.
     *
     * @param uid The unique identifier for the device.
     * @return The device instance.
     */
    Client createDeviceFor(String uid);

    /**
     * Create a transfer item instance for the given parameters.
     *
     * @param transferId Points to {@link TransferItem#transferId}.
     * @param id         Points to {@link TransferItem#id}.
     * @param name       Points to {@link TransferItem#name}.
     * @param mimeType   Points to {@link TransferItem#mimeType}.
     * @param size       Points to {@link TransferItem#size}..
     * @param directory  Points to {@link TransferItem#directory}.
     * @param type       Points to {@link TransferItem#type}
     * @return The transfer item instance.
     */
    TransferItem createTransferItemFor(long transferId, long id, String name, String mimeType, long size,
                                       String directory, TransferItem.Type type);

    /**
     * Convert this device into {@link JSONObject}.
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
    default JSONObject deviceAsJson(int pin) throws JSONException
    {
        Client client = getDevice();
        JSONObject object = new JSONObject()
                .put(Keyword.DEVICE_UID, client.uid)
                .put(Keyword.DEVICE_BRAND, client.brand)
                .put(Keyword.DEVICE_MODEL, client.model)
                .put(Keyword.DEVICE_USERNAME, client.username)
                .put(Keyword.DEVICE_CLIENT_TYPE, client.clientType)
                .put(Keyword.DEVICE_VERSION_CODE, client.versionCode)
                .put(Keyword.DEVICE_VERSION_NAME, client.versionName)
                .put(Keyword.DEVICE_PROTOCOL_VERSION, client.protocolVersion)
                .put(Keyword.DEVICE_PROTOCOL_VERSION_MIN, client.protocolVersionMin)
                .put(Keyword.DEVICE_PIN, pin);

        byte[] deviceAvatar = getAvatar();
        if (deviceAvatar.length > 0)
            object.put(Keyword.DEVICE_AVATAR, Base64.getEncoder().encodeToString(deviceAvatar));

        return object;
    }

    /**
     * Returns the avatar for this client.
     *
     * @return The bitmap data for the avatar if exists, or zero-length byte array if it doesn't.
     */
    byte[] getAvatar();

    /**
     * Returns the avatar for the given device.
     * <p>
     * If the given device's {@link Client#uid} is equal to {@link #getDeviceUid()}, this should return the avatar
     * for this client.
     *
     * @param client For which the avatar will be provided.
     * @return The bitmap data for the avatar if exists, or zero-length byte array if it doesn't.
     */
    byte[] getAvatarFor(Client client);

    /**
     * Returns this client's certificate.
     * <p>
     * The returned value should stay persistent.
     *
     * @return This client's certificate.
     */
    X509Certificate getCertificate();

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
     * This should return the unique identifier for this client. It should be both unique and persistent.
     * <p>
     * It is not meant to change.
     *
     * @return The unique identifier for this client.
     */
    String getDeviceUid();

    /**
     * This will return the {@link Client} instance representing this client.
     *
     * @return The device instance.
     */
    Client getDevice();

    /**
     * This will return the first valid item that that this side can receive.
     *
     * @param transferId Points to {@link TransferItem#transferId}.
     * @return The transfer receivable item or null if there are none.
     */
    TransferItem getFirstReceivableItem(long transferId);

    /**
     * This method is invoked when there is a new connection to the server.
     * <p>
     * This is used to provide the access PIN which may be delivered to the remote device via a QR code, or by other
     * means to allow it to have instant access to this client.
     * <p>
     * The code should persist until {@link #revokeNetworkPin()} is invoked.
     *
     * @return the PIN that will change after revoked.
     * @see #revokeNetworkPin()
     */
    int getNetworkPin();

    PrivateKey getPrivateKey();

    PublicKey getPublicKey();

    SSLContext getSSLContextFor(Client client);

    /**
     * Check whether the given device had already sent a wrong key and has a pending key request to be approved.
     *
     * @param deviceUid That sent the request.
     * @return True if there is a pending request.
     * @see #saveRequestForInvalidationOfCredentials(String)
     * @see #approveInvalidationOfCredentials(Client)
     */
    boolean hasRequestForInvalidationOfCredentials(String deviceUid);

    /**
     * Load transfer item for the given parameters.
     *
     * @param deviceUid   Owning the item.
     * @param transferId Points to {@link TransferItem#transferId}
     * @param id         Points to {@link TransferItem#id}.
     * @param type       Specifying whether this is an incoming or outgoing operation.
     * @return The transfer item that points to the given parameters or null if there is no match.
     * @throws PersistenceException When the given parameters don't point to a valid item.
     */
    TransferItem loadTransferItem(String deviceUid, long transferId, long id, TransferItem.Type type)
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
     * This method is invoked after the PIN is used by a device.
     *
     * @see #getNetworkPin()
     */
    void revokeNetworkPin();

    /**
     * Save this device in the persistence database.
     * <p>
     * Do not hold any duplicates, and verify it using the {@link Client#uid} field.
     *
     * @param client To save.
     */
    void save(Client client);

    /**
     * Save this device address in the persistence database.
     * <p>
     * Doing so will allow you to connect to a device later.
     *
     * @param deviceAddress To save.
     */
    void save(DeviceAddress deviceAddress);

    /**
     * Save this transfer item in the persistence database.
     * <p>
     * Note: ensure there are no duplicates.
     *
     * @param deviceUid That owns the item.
     * @param item     To save.
     */
    void save(String deviceUid, TransferItem item);

    /**
     * Save all the items in the given list.
     *
     * @param deviceUid That owns the items.
     * @param itemList To save.
     */
    void save(String deviceUid, List<? extends TransferItem> itemList);

    /**
     * Save the avatar for the given device.
     * <p>
     * This will be invoked both when the device has an avatar and when it doesn't.
     *
     * @param deviceUid The device that the avatar belongs to.
     * @param bitmap   The bitmap data for the avatar.
     */
    void saveAvatar(String deviceUid, byte[] bitmap);

    /**
     * This method call happens when a known device sends different credentials and now cannot connect.
     * <p>
     * You can show the error to the user so that they can decide for themselves.
     * <p>
     * At the end, you should remove the existing certificate for the given device id.
     *
     * @param deviceUid That wants key invalidation.
     * @see #hasRequestForInvalidationOfCredentials(String)
     * @see #approveInvalidationOfCredentials(Client)
     */
    void saveRequestForInvalidationOfCredentials(String deviceUid);

    /**
     * Change the state of the given item.
     * <p>
     * Note: this should set the state but should not save it since saving it is spared for
     * {@link #save(String, TransferItem)}.
     *
     * @param deviceUid That owns the copy of the 'item'.
     * @param item     Of which the given state will be applied.
     * @param state    The level of invalidation.
     * @param e        The nullable additional exception cause this state.
     * @see #STATE_PENDING
     * @see #STATE_INVALIDATED_TEMPORARILY
     * @see #STATE_INVALIDATED_STICKY
     * @see #STATE_IN_PROGRESS
     * @see #STATE_DONE
     */
    void setState(String deviceUid, TransferItem item, int state, Exception e);

    /**
     * Sync the device with the persistence database.
     *
     * @param client To sync.
     * @throws PersistenceException When there is no device associated with the unique identifier {@link Client#uid}.
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
                    .put(Keyword.TRANSFER_ITEM_ID, transferItem.id)
                    .put(Keyword.INDEX_FILE_NAME, transferItem.name)
                    .put(Keyword.INDEX_FILE_SIZE, transferItem.size)
                    .put(Keyword.INDEX_FILE_MIME, transferItem.mimeType);

            if (transferItem.directory != null)
                json.put(Keyword.INDEX_DIRECTORY, transferItem.directory);

            jsonArray.put(json);
        }

        return jsonArray;
    }

    /**
     * This will inflate the given JSON data that was received from the remote to make it consumable as a collection.
     *
     * @param transferId That the JSON data contains items of.
     * @param jsonArray  Th transfer items that is going to inflated.
     * @return The list of items inflated from the JSON data.
     * @throws JSONException If the JSON data is corrupted or has missing/mismatch values.
     */
    default List<TransferItem> toTransferItemList(long transferId, String jsonArray) throws JSONException
    {
        JSONArray json = new JSONArray(jsonArray);
        List<TransferItem> transferItemList = new ArrayList<>();

        if (json.length() > 0) {
            for (int i = 0; i < json.length(); i++) {
                JSONObject jsonObject = json.getJSONObject(i);
                String directory = jsonObject.has(Keyword.INDEX_DIRECTORY)
                        ? jsonObject.getString(Keyword.INDEX_DIRECTORY) : null;
                transferItemList.add(createTransferItemFor(transferId, jsonObject.getLong(Keyword.TRANSFER_ITEM_ID),
                        jsonObject.getString(Keyword.INDEX_FILE_NAME), jsonObject.getString(Keyword.INDEX_FILE_MIME),
                        jsonObject.getLong(Keyword.INDEX_FILE_SIZE), directory, TransferItem.Type.INCOMING));
            }
        }

        return transferItemList;
    }
}
