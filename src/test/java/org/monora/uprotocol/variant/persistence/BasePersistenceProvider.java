package org.monora.uprotocol.variant.persistence;

import org.monora.uprotocol.core.network.Device;
import org.monora.uprotocol.core.network.DeviceAddress;
import org.monora.uprotocol.core.network.TransferItem;
import org.monora.uprotocol.core.persistence.PersistenceException;
import org.monora.uprotocol.core.persistence.PersistenceProvider;
import org.monora.uprotocol.core.persistence.StreamDescriptor;
import org.monora.uprotocol.variant.DefaultDevice;
import org.monora.uprotocol.variant.DefaultDeviceAddress;
import org.monora.uprotocol.variant.DefaultTransferItem;
import org.monora.uprotocol.variant.holder.Avatar;
import org.monora.uprotocol.variant.holder.MemoryStreamDescriptor;
import org.monora.uprotocol.variant.holder.OwnedTransferHolder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides some level of "persistence" on the level for testing purposes.
 * <p>
 * This will be slow and is not meant to be used in production.
 */
public abstract class BasePersistenceProvider implements PersistenceProvider
{
    private final List<Device> deviceList = new ArrayList<>();
    private final List<DeviceAddress> deviceAddressList = new ArrayList<>();
    private final List<OwnedTransferHolder> transferHolderList = new ArrayList<>();
    private final List<Avatar> avatarList = new ArrayList<>();
    private final List<MemoryStreamDescriptor> streamDescriptorList = new ArrayList<>();

    private int networkPin;

    @Override
    public void broadcast()
    {

    }

    @Override
    public boolean containsTransfer(long transferId)
    {
        synchronized (transferHolderList) {
            for (OwnedTransferHolder holder : transferHolderList) {
                if (holder.item.transferId == transferId)
                    return true;
            }
        }

        return false;
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
                                              String directory, TransferItem.Type type)
    {
        return new DefaultTransferItem(transferId, id, name, mimeType, size, directory, type);
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
    public StreamDescriptor getDescriptorFor(TransferItem transferItem)
    {
        synchronized (streamDescriptorList) {
            for (MemoryStreamDescriptor streamDescriptor : streamDescriptorList) {
                if (streamDescriptor.transferItem.equals(transferItem))
                    return streamDescriptor;
            }

            MemoryStreamDescriptor descriptor = MemoryStreamDescriptor.newInstance(transferItem);
            streamDescriptorList.add(descriptor);
            return descriptor;
        }
    }

    @Override
    public TransferItem getFirstReceivableItem(long transferId)
    {
        synchronized (transferHolderList) {
            for (OwnedTransferHolder holder : transferHolderList) {
                if (TransferItem.Type.INCOMING.equals(holder.item.type) && holder.item.transferId == transferId
                        && holder.state == STATE_PENDING)
                    return holder.item;
            }
        }
        return null;
    }

    @Override
    public int getNetworkPin()
    {
        if (networkPin == 0)
            networkPin = generateKey();
        return networkPin;
    }

    @Override
    public TransferItem loadTransferItem(String deviceId, long transferId, long id, TransferItem.Type type)
            throws PersistenceException
    {
        synchronized (transferHolderList) {
            for (OwnedTransferHolder holder : transferHolderList) {
                if (holder.item.transferId == transferId && holder.item.id == id && holder.item.type.equals(type)
                        && holder.deviceId.equals(deviceId))
                    return holder.item;
            }
        }

        throw new PersistenceException("There is no transfer data matching the given parameters.");
    }

    @Override
    public InputStream openInputStream(StreamDescriptor descriptor) throws IOException
    {
        if (descriptor instanceof MemoryStreamDescriptor)
            return new ByteArrayInputStream(((MemoryStreamDescriptor) descriptor).data.toByteArray());

        throw new IOException("Unknown descriptor type");
    }

    @Override
    public OutputStream openOutputStream(StreamDescriptor descriptor) throws IOException
    {
        if (descriptor instanceof MemoryStreamDescriptor)
            return ((MemoryStreamDescriptor) descriptor).data;

        throw new IOException("Unknown descriptor type");
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
    public void save(DeviceAddress deviceAddress)
    {
        synchronized (deviceAddressList) {
            deviceAddressList.add(deviceAddress);
        }
    }

    @Override
    public void save(String deviceId, TransferItem item)
    {
        synchronized (transferHolderList) {
            for (OwnedTransferHolder holder : transferHolderList) {
                if (holder.item.equals(item)) {
                    holder.item = item;
                    holder.deviceId = deviceId;
                    return;
                }
            }

            transferHolderList.add(new OwnedTransferHolder(item, deviceId));
        }
    }

    @Override
    public void save(String deviceId, List<? extends TransferItem> itemList)
    {
        for (TransferItem item : itemList) {
            save(deviceId, item);
        }
    }

    @Override
    public void saveAvatar(String deviceId, byte[] bitmap)
    {
        synchronized (avatarList) {
            avatarList.add(new Avatar(deviceId, bitmap));
        }
    }

    @Override
    public void setState(String deviceId, TransferItem item, int state, Exception e)
    {
        synchronized (transferHolderList) {
            for (OwnedTransferHolder holder : transferHolderList) {
                if (deviceId.equals(holder.deviceId) && item.equals(holder.item))
                    holder.state = state;
            }
        }
    }

    @Override
    public void sync(Device device) throws PersistenceException
    {
        synchronized (deviceList) {
            for (Device persistentDevice : deviceList) {
                if (device.equals(persistentDevice)) {
                    device.from(persistentDevice);
                    return;
                }
            }
        }

        throw new PersistenceException("The requested device did not exist and failed to sync.");
    }
}
