package org.monora.uprotocol.core.persistence;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.monora.uprotocol.core.CommunicationBridge;
import org.monora.uprotocol.core.network.Device;
import org.monora.uprotocol.core.network.DeviceAddress;
import org.monora.uprotocol.core.network.TransferItem;
import org.monora.uprotocol.core.spec.alpha.Keyword;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * This class is responsible for making changes persistent.
 */
public interface PersistenceProvider
{
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
     * Create device address instance.
     *
     * @param address At which the device address will be pointing.
     * @return The device address instance.
     */
    DeviceAddress createDeviceAddressFor(InetAddress address);

    /**
     * Request from the factory to create an empty {@link Device} instance.
     *
     * @return The device instance.
     */
    Device createDevice();

    /**
     * Create a device instance using the given unique identifier.
     * <p>
     * The resulting {@link Device} instance is not ready for use. To make it so, call {@link #sync(Device)}.
     *
     * @param uid The unique identifier for the device.
     * @return The device instance.
     */
    Device createDeviceFor(String uid);

    /**
     * Create a transfer item instance for the given parameters.
     *
     * @param transferId Points to {@link TransferItem#transferId}.
     * @param id         Points to {@link TransferItem#id}.
     * @param name       Points to {@link TransferItem#name}.
     * @param mimeType   Points to {@link TransferItem#mimeType}.
     * @param size       Points to {@link TransferItem#size}.
     * @param file       Points to {@link TransferItem#file}.
     * @param directory  Points to {@link TransferItem#directory}.
     * @param type       Points to {@link TransferItem#type}
     * @return The transfer item instance.
     */
    TransferItem createTransferItemFor(long transferId, long id, String name, String mimeType, long size, String file,
                                       String directory, TransferItem.Type type);

    /**
     * Generate a unique 32-bit signed integer to use as a key.
     *
     * @return The generated key.
     */
    int generateKey();

    /**
     * Returns the avatar for this client.
     *
     * @return The bitmap data for the avatar if exists, or zero-length byte array if it doesn't.
     */
    byte[] getAvatar();

    /**
     * Returns the avatar for the given device.
     * <p>
     * If the given device's {@link Device#uid} is equal to {@link #getDeviceUid()}, this should return the avatar
     * for this client.
     *
     * @param device For which the avatar will be provided.
     * @return The bitmap data for the avatar if exists, or zero-length byte array if it doesn't.
     */
    byte[] getAvatarFor(Device device);

    /**
     * This should return the unique identifier for this client. It should be both unique and persistent.
     * <p>
     * It is not meant to change.
     *
     * @return The unique identifier for this client.
     */
    String getDeviceUid();

    /**
     * This will return the {@link Device} instance representing this client.
     *
     * @return The device instance.
     */
    Device getDevice();

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

    /**
     * Get the temporary file format which will be used for the incoming files until they are saved to their original
     * paths.
     * <p>
     * As an example, let us say that this method returns 'tmp' and the transfer item id is 42424242. The resulting
     * file name will be ".42424242.tmp".
     *
     * @return The temporary file format.
     */
    String getTemporaryFileFormat();

    /**
     * This method is invoked after the PIN is used by a device.
     *
     * @see #getNetworkPin()
     */
    void revokeNetworkPin();

    /**
     * Save this device in the persistence database.
     * <p>
     * Do not hold any duplicates, and verify it using the {@link Device#uid} field.
     *
     * @param device To save.
     */
    void save(Device device);

    /**
     * Save this device address in the persistence database.
     * <p>
     * Doing so will allow you to connect to a device later.
     *
     * @param deviceAddress To save.
     */
    void save(DeviceAddress deviceAddress);

    /**
     * Save the avatar for the given device.
     * <p>
     * This will be invoked both when the device has an avatar and when it doesn't.
     *
     * @param device The device that the avatar belongs to.
     * @param bitmap The bitmap data for the avatar.
     */
    void saveAvatar(Device device, byte[] bitmap);

    /**
     * Sync the device with the persistence database.
     *
     * @param device To sync.
     * @throws PersistenceException When there is no device associated with the unique identifier {@link Device#uid}.
     */
    void sync(Device device) throws PersistenceException;

    /**
     * Convert this device into {@link JSONObject}.
     * <p>
     * This should only be invoked when communicating with remote. Excluding the {@link PersistenceProvider}, the rest
     * of values concerns the remote, which means those values are special to it and no other device should be
     * using them.
     * <p>
     * The sender key should be gathered from the persistence database and assigned to {@link Device#senderKey}.
     * <p>
     * Keys should be persistent. They should never be random, and saving them as '0' should result in "error". These
     * keys are mutually held by you and the remote. Here is a graph representing how the keys correspond to each other:
     * <p>
     * Us {@link Device#receiverKey} == Remote {@link Device#senderKey}
     * Us {@link Device#senderKey} == Remote {@link Device#receiverKey}
     * <p>
     * The PIN will usually be unavailable, so it is okay to provide '0'. It should be available through connectionless
     * means like QR Code or manual-entry. It is used to bypass the security mechanisms so the communication can happen
     * seamlessly.
     *
     * @param senderKey The key which is known by the remote as {@link Device#receiverKey}.
     * @param pin       The PIN to bypass errors like not matching keys. This will also flag this client as trusted.
     * @return The JSON object
     * @throws JSONException If the creation of the JSON object fails for some reason.
     */
    default JSONObject toJson(int senderKey, int pin) throws JSONException
    {
        Device device = getDevice();
        JSONObject object = new JSONObject()
                .put(Keyword.DEVICE_UID, device.uid)
                .put(Keyword.DEVICE_BRAND, device.brand)
                .put(Keyword.DEVICE_MODEL, device.model)
                .put(Keyword.DEVICE_USERNAME, device.username)
                .put(Keyword.DEVICE_CLIENT_TYPE, device.clientType)
                .put(Keyword.DEVICE_VERSION_CODE, device.versionCode)
                .put(Keyword.DEVICE_VERSION_NAME, device.versionName)
                .put(Keyword.DEVICE_PROTOCOL_VERSION, device.protocolVersion)
                .put(Keyword.DEVICE_PROTOCOL_VERSION_MIN, device.protocolVersionMin)
                .put(Keyword.DEVICE_KEY, senderKey)
                .put(Keyword.DEVICE_PIN, pin);

        byte[] deviceAvatar = getAvatar();
        if (deviceAvatar.length > 0)
            object.put(Keyword.DEVICE_AVATAR, Base64.getEncoder().encodeToString(deviceAvatar));

        return object;
    }

    /**
     * Transform a given {@link TransferItem} list into its {@link JSONArray} equivalent.
     * <p>
     * The resulting {@link JSONArray} can be fed to {@link CommunicationBridge#requestFileTransfer(long, JSONArray)},
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
                long itemId = jsonObject.getLong(Keyword.TRANSFER_ITEM_ID);
                String directory = jsonObject.has(Keyword.INDEX_DIRECTORY)
                        ? jsonObject.getString(Keyword.INDEX_DIRECTORY) : null;
                String file = "." + itemId + "." + getTemporaryFileFormat();
                transferItemList.add(createTransferItemFor(transferId, itemId,
                        jsonObject.getString(Keyword.INDEX_FILE_NAME), jsonObject.getString(Keyword.INDEX_FILE_MIME),
                        jsonObject.getLong(Keyword.INDEX_FILE_SIZE), file, directory, TransferItem.Type.INCOMING));
            }
        }

        return transferItemList;
    }
}
