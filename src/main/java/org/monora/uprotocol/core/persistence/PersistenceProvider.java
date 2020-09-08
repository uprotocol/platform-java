package org.monora.uprotocol.core.persistence;

import org.json.JSONException;
import org.json.JSONObject;
import org.monora.uprotocol.core.network.Device;
import org.monora.uprotocol.core.network.DeviceAddress;
import org.monora.uprotocol.core.spec.alpha.Keyword;

import java.net.InetAddress;
import java.util.Base64;

/**
 * This class provides the previous data exchanged between devices or waiting to be exchanged.
 */
public interface PersistenceProvider
{
    /**
     * Create device address instance.
     *
     * @param
     * @param address
     * @return
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
     * The code changes after it is used by the remote.
     * <p>
     * You could put a QR code that contains the PIN, and when the remote sends that PIN, we would know that the remote
     * device cooperating with this client and thus, it can be trusted.
     *
     * @return the PIN that will change after revoked.
     * @see #revokeNetworkPin()
     */
    int getNetworkPin();

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
     *
     * @param device        That owns the given address.
     * @param deviceAddress To save.
     */
    void save(Device device, DeviceAddress deviceAddress);

    /**
     * Save the connection address for the given device.
     * <p>
     * The database should be checked in order for the removal of the duplicates.
     *
     * @param device      That owns the given address.
     * @param inetAddress that the remote device connected to us over.
     */
    void save(Device device, InetAddress inetAddress);

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
     * @throws PersistenceException When there is no device associated with the unique identifier.
     */
    void sync(Device device) throws PersistenceException;

    /**
     * Convert this device into {@link JSONObject}.
     * <p>
     * This should only be invoked when communicating with remote. Excluding the {@link PersistenceProvider}, the rest
     * of values concerns the remote, which means those values are special to the remote and no other device should be
     * using them.
     * <p>
     * The send key should be gathered from the persistence database and assigned to {@link Device#sendKey}.
     * <p>
     * Keys should be persistent. They should never be random, and saving them as '0' should result in "error". These
     * keys are mutually held by you and the remote. Here is a graph representing how the keys correspond to each other:
     * <p>
     * Us {@link Device#receiveKey} == Remote {@link Device#sendKey}
     * Us {@link Device#sendKey} == Remote {@link Device#receiveKey}
     * <p>
     * The PIN will usually be unavailable, so it is okay to provide '0'. It should be available through connectionless
     * means like QR Code or manual-entry. It is used to bypass the security mechanisms so the communication can happen
     * seamlessly.
     *
     * @param persistenceProvider Which will provide the avatar and other database related fields.
     * @param sendKey             The key which is known by the remote as {@link Device#receiveKey}.
     * @param pin                 The PIN to bypass errors like not matching keys. This will also flag this client as
     *                            trusted using the {@link Device#isTrusted} field.
     * @return The JSON object
     * @throws JSONException If the creation of the JSON object fails for some reason.
     */
    static JSONObject toJson(PersistenceProvider persistenceProvider, int sendKey, int pin)
            throws JSONException
    {
        Device device = persistenceProvider.getDevice();
        JSONObject object = new JSONObject()
                .put(Keyword.DEVICE_UID, device.uid)
                .put(Keyword.DEVICE_BRAND, device.brand)
                .put(Keyword.DEVICE_MODEL, device.model)
                .put(Keyword.DEVICE_USERNAME, device.username)
                .put(Keyword.DEVICE_VERSION_CODE, device.versionCode)
                .put(Keyword.DEVICE_VERSION_NAME, device.versionName)
                .put(Keyword.DEVICE_PROTOCOL_VERSION, device.protocolVersion)
                .put(Keyword.DEVICE_PROTOCOL_VERSION_MIN, device.protocolVersionMin)
                .put(Keyword.DEVICE_KEY, sendKey)
                .put(Keyword.DEVICE_PIN, pin);

        byte[] deviceAvatar = persistenceProvider.getAvatar();
        if (deviceAvatar.length > 0)
            object.put(Keyword.DEVICE_AVATAR, Base64.getEncoder().encodeToString(deviceAvatar));

        return object;
    }

}
