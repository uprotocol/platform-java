package org.monora.uprotocol.variant;

import org.monora.uprotocol.core.CommunicationBridge;
import org.monora.uprotocol.core.TransportSeat;
import org.monora.uprotocol.core.network.Device;
import org.monora.uprotocol.core.network.DeviceAddress;
import org.monora.uprotocol.core.network.TransferItem;
import org.monora.uprotocol.core.persistence.PersistenceException;
import org.monora.uprotocol.core.persistence.PersistenceProvider;
import org.monora.uprotocol.core.protocol.communication.CommunicationException;

import java.util.List;

public class DefaultTransportSeat implements TransportSeat
{
    public final PersistenceProvider persistenceProvider;

    private boolean autoAcceptNewKeys;

    public DefaultTransportSeat(PersistenceProvider persistenceProvider)
    {
        this.persistenceProvider = persistenceProvider;
    }

    @Override
    public void beginFileTransfer(CommunicationBridge bridge, Device device, long transferId, TransferItem.Type type)
            throws PersistenceException, CommunicationException
    {
        if (type.equals(TransferItem.Type.INCOMING))
            receiveFiles(bridge, transferId);
        else if (type.equals(TransferItem.Type.OUTGOING))
            sendFiles(bridge, transferId);
    }

    @Override
    public boolean handleAcquaintanceRequest(Device device, DeviceAddress deviceAddress)
    {
        return true;
    }

    @Override
    public void handleFileTransferRequest(Device device, boolean hasPin, long transferId, String jsonArray)
            throws PersistenceException, CommunicationException
    {
        List<TransferItem> itemList = persistenceProvider.toTransferItemList(transferId, jsonArray);
        persistenceProvider.save(device.uid, itemList);
    }

    @Override
    public void handleFileTransferState(Device device, long transferId, boolean isAccepted)
    {

    }

    @Override
    public void handleTextTransfer(Device device, String text)
    {
        System.out.println("Text received: " + text);
    }

    @Override
    public boolean hasOngoingTransferFor(long transferId, String deviceUid, TransferItem.Type type)
    {
        return false;
    }

    @Override
    public boolean hasOngoingIndexingFor(long transferId)
    {
        return false;
    }

    @Override
    public void notifyDeviceKeyChanged(Device device, int receiverKey, int senderKey)
    {
        if (autoAcceptNewKeys) {
            persistenceProvider.approveKeyInvalidationRequest(device);
        }
    }

    public void setAutoAcceptNewKeys(boolean autoAcceptNewKeys)
    {
        this.autoAcceptNewKeys = autoAcceptNewKeys;
    }
}
