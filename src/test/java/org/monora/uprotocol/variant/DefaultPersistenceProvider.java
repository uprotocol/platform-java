package org.monora.uprotocol.variant;

import org.monora.uprotocol.core.network.Device;
import org.monora.uprotocol.core.network.DeviceAddress;
import org.monora.uprotocol.core.network.TransferItem;
import org.monora.uprotocol.core.persistence.PersistenceException;
import org.monora.uprotocol.core.persistence.PersistenceProvider;
import org.monora.uprotocol.variant.holder.Avatar;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class DefaultPersistenceProvider implements PersistenceProvider
{
    private final List<Device> deviceList = new ArrayList<>();
    private final List<DeviceAddress> deviceAddressList = new ArrayList<>();
    private final List<TransferItem> transferItemsList = new ArrayList<>();
    private final List<Avatar> avatarList = new ArrayList<>();

    private int networkPin;

    @Override
    public void broadcast()
    {

    }

    @Override
    public DeviceAddress createDeviceAddressFor(InetAddress address)
    {
        return new DefaultDeviceAddress(address);
    }

    @Override
    public DefaultDevice createDevice()
    {
        return new DefaultDevice();
    }

    @Override
    public DefaultDevice createDeviceFor(String uid)
    {
        return new DefaultDevice(uid);
    }

    @Override
    public TransferItem createTransferItemFor(long transferId, long id, String name, String mimeType, long size,
                                              String file, String directory, TransferItem.Type type)
    {
        return new DefaultTransferItem(transferId, id, name, mimeType, size, file, directory, type);
    }

    @Override
    public int generateKey()
    {
        return (int) (Integer.MAX_VALUE * Math.random());
    }

    @Override
    public byte[] getAvatar()
    {
        return new byte[0];
    }

    @Override
    public byte[] getAvatarFor(Device device)
    {
        synchronized (avatarList) {
            for (Avatar avatar : avatarList) {
                if (avatar.deviceUid.equals(device.uid))
                    return avatar.data;
            }
        }
        return new byte[0];
    }

    @Override
    public String getDeviceUid()
    {
        return "fdsfgffgdfgsf";
    }

    @Override
    public DefaultDevice getDevice()
    {
        DefaultDevice defaultDevice = new DefaultDevice(getDeviceUid(), "uprotocol", 100,
                100, "uwu", "wuw");
        defaultDevice.isLocal = true;
        return defaultDevice;
    }

    @Override
    public int getNetworkPin()
    {
        if (networkPin == 0)
            networkPin = generateKey();
        return networkPin;
    }

    @Override
    public String getTemporaryFileFormat()
    {
        return ".tmp";
    }

    @Override
    public void revokeNetworkPin()
    {
        networkPin = 0;
    }

    @Override
    public void save(Device device)
    {
        synchronized (deviceList) {
            deviceList.add(device);
        }
    }

    @Override
    public void save(Device device, DeviceAddress deviceAddress)
    {
        synchronized (deviceAddressList) {
            deviceAddressList.add(deviceAddress);
        }
    }

    @Override
    public void save(Device device, InetAddress inetAddress)
    {
        save(device, new DefaultDeviceAddress(inetAddress, device.uid, System.currentTimeMillis()));
    }

    @Override
    public void saveAvatar(Device device, byte[] bitmap)
    {
        synchronized (avatarList) {
            avatarList.add(new Avatar(device.uid, bitmap));
        }
    }

    @Override
    public void sync(Device device) throws PersistenceException
    {
        synchronized (deviceList) {
            for (Device persistentDevice : deviceList) {
                if (device.equals(persistentDevice)) {
                    device.from(persistentDevice);
                    break;
                }
            }
        }

        throw new PersistenceException("I am breathing stereo.");
    }
}
