package org.monora.uprotocol.core;

import org.json.JSONException;
import org.json.JSONObject;
import org.monora.uprotocol.core.network.Device;
import org.monora.uprotocol.core.network.DeviceAddress;
import org.monora.uprotocol.core.persistence.PersistenceException;
import org.monora.uprotocol.core.persistence.PersistenceProvider;
import org.monora.uprotocol.core.protocol.ClientType;
import org.monora.uprotocol.core.protocol.ConnectionFactory;
import org.monora.uprotocol.core.protocol.communication.ClientBlockedException;
import org.monora.uprotocol.core.spec.alpha.Keyword;

import java.net.InetAddress;
import java.util.Base64;

import static org.monora.uprotocol.core.spec.alpha.Config.LENGTH_DEVICE_USERNAME;

/**
 * Handles loading details of remote.
 */
public class DeviceLoader
{
    /**
     * Load the device details as if you are connecting as a device.
     *
     * @param persistenceProvider That stores persistent data.
     * @param object              To load the details from.
     * @param device              To load into.
     * @throws JSONException If something goes wrong when inflating the JSON data.
     */
    public static void loadAsClient(PersistenceProvider persistenceProvider, JSONObject object, Device device)
            throws JSONException
    {
        device.isBlocked = false;
        loadFrom(persistenceProvider, object, device);
    }

    /**
     * Load the device details as if you are server.
     *
     * @param persistenceProvider That stores persistent data.
     * @param object              To load the details from.
     * @param device              To load into.
     * @param hasPin              True will mean this device has a valid PIN and this will escalate the privileges it
     *                            will have. For instance, it will be unblocked if blocked and it will be flagged as
     *                            trusted.
     * @throws JSONException          If something goes wrong when inflating the JSON data.* @throws DeviceInsecureException
     * @throws ClientBlockedException If remote is blocked and has no valid PIN.
     */
    public static void loadAsServer(PersistenceProvider persistenceProvider, JSONObject object, Device device,
                                    boolean hasPin) throws JSONException, ClientBlockedException
    {
        device.uid = object.getString(Keyword.DEVICE_UID);
        if (hasPin)
            device.isTrusted = true;

        try {
            try {
                persistenceProvider.sync(device);
            } catch (PersistenceException ignored) {
            }

            if (hasPin) {
                device.isBlocked = false;
            } else if (device.isBlocked)
                throw new ClientBlockedException(device);
        } finally {
            loadFrom(persistenceProvider, object, device);
        }
    }

    private static void loadFrom(PersistenceProvider persistenceProvider, JSONObject object, Device device)
            throws JSONException
    {
        device.isLocal = persistenceProvider.getDeviceUid().equals(device.uid);
        device.brand = object.getString(Keyword.DEVICE_BRAND);
        device.model = object.getString(Keyword.DEVICE_MODEL);
        device.username = object.getString(Keyword.DEVICE_USERNAME);
        device.clientType = object.getEnum(ClientType.class, Keyword.DEVICE_CLIENT_TYPE);
        device.lastUsageTime = System.currentTimeMillis();
        device.versionCode = object.getInt(Keyword.DEVICE_VERSION_CODE);
        device.versionName = object.getString(Keyword.DEVICE_VERSION_NAME);
        device.protocolVersion = object.getInt(Keyword.DEVICE_PROTOCOL_VERSION);
        device.protocolVersionMin = object.getInt(Keyword.DEVICE_PROTOCOL_VERSION_MIN);

        if (device.username.length() > LENGTH_DEVICE_USERNAME)
            device.username = device.username.substring(0, LENGTH_DEVICE_USERNAME);

        persistenceProvider.save(device);

        byte[] deviceAvatar;
        try {
            deviceAvatar = Base64.getDecoder().decode(object.getString(Keyword.DEVICE_AVATAR));
        } catch (Exception ignored) {
            deviceAvatar = new byte[0];
        }

        persistenceProvider.saveAvatar(device.uid, deviceAvatar);
    }

    /**
     * Load a device using an internet address.
     *
     * @param connectionFactory   That will set up the connection.
     * @param persistenceProvider That stores the persistent data.
     * @param address             To connect to.
     * @param listener            That listens for successful attempts. Pass it as 'null' if unneeded.
     */
    public static void load(ConnectionFactory connectionFactory, PersistenceProvider persistenceProvider,
                            InetAddress address, OnDeviceResolvedListener listener)
    {
        new Thread(() -> {
            try (CommunicationBridge bridge = CommunicationBridge.connect(connectionFactory, persistenceProvider,
                    persistenceProvider.createDeviceAddressFor(address), null, 0)) {
                bridge.sendResult(false);
                if (listener != null)
                    listener.onDeviceResolved(bridge.getDevice(), bridge.getDeviceAddress());
            } catch (Exception ignored) {
            }
        }).start();
    }

    /**
     * Callback for delivering the details for a client when successful.
     */
    public interface OnDeviceResolvedListener
    {
        /**
         * Called after the connection is successful.
         *
         * @param device  The devices that has been reached.
         * @param address The address that device was found at.
         */
        void onDeviceResolved(Device device, DeviceAddress address);
    }
}
