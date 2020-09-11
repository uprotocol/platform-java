package org.monora.uprotocol.core;

import org.json.JSONException;
import org.json.JSONObject;
import org.monora.uprotocol.core.network.Device;
import org.monora.uprotocol.core.network.DeviceAddress;
import org.monora.uprotocol.core.persistence.PersistenceException;
import org.monora.uprotocol.core.persistence.PersistenceProvider;
import org.monora.uprotocol.core.protocol.*;
import org.monora.uprotocol.core.spec.alpha.Keyword;

import java.net.InetAddress;
import java.util.Base64;

import static org.monora.uprotocol.core.spec.alpha.Config.LENGTH_DEVICE_USERNAME;

public class DeviceLoader
{
    public static void loadAsClient(PersistenceProvider persistenceProvider, JSONObject object, Device device)
            throws JSONException
    {
        device.isBlocked = false;
        device.receiverKey = object.getInt(Keyword.DEVICE_KEY);
        loadFrom(persistenceProvider, object, device);
    }

    public static void loadAsServer(PersistenceProvider persistenceProvider, JSONObject object, Device device,
                                    boolean hasPin) throws JSONException, DeviceInsecureException
    {
        device.uid = object.getString(Keyword.DEVICE_UID);
        int receiverKey = object.getInt(Keyword.DEVICE_KEY);
        if (hasPin)
            device.isTrusted = true;

        try {
            try {
                persistenceProvider.sync(device);

                if (hasPin && receiverKey != device.receiverKey)
                    throw new PersistenceException("Generate new keys.");
            } catch (PersistenceException e) {
                device.receiverKey = receiverKey;
                device.senderKey = persistenceProvider.generateKey();
            }

            if (hasPin) {
                device.isBlocked = false;
            } else if (device.isBlocked)
                throw new DeviceBlockedException("The device is blocked.", device);
            else if (receiverKey != device.receiverKey) {
                device.isBlocked = true;
                throw new DeviceVerificationException("The device receiver key is different.", device, receiverKey);
            }
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

        persistenceProvider.saveAvatar(device, deviceAvatar);
    }

    public static void load(ConnectionProvider connectionProvider, PersistenceProvider persistenceProvider,
                            InetAddress address, OnDeviceResolvedListener listener)
    {
        new Thread(() -> {
            try (CommunicationBridge bridge = CommunicationBridge.connect(connectionProvider, persistenceProvider,
                    persistenceProvider.createDeviceAddressFor(address), null, 0)) {
                if (listener != null)
                    listener.onDeviceResolved(bridge.getDevice(), bridge.getDeviceAddress());
            } catch (Exception ignored) {
            }
        }).start();
    }

    public interface OnDeviceResolvedListener
    {
        void onDeviceResolved(Device device, DeviceAddress address);
    }
}