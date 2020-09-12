package org.monora.uprotocol.variant;

import org.monora.uprotocol.core.CommunicationBridge;
import org.monora.uprotocol.core.TransportSeat;
import org.monora.uprotocol.core.network.Device;
import org.monora.uprotocol.core.network.DeviceAddress;
import org.monora.uprotocol.core.network.TransferItem;
import org.monora.uprotocol.core.persistence.PersistenceException;
import org.monora.uprotocol.core.persistence.PersistenceProvider;
import org.monora.uprotocol.core.protocol.communication.CommunicationException;

public class DefaultTransportSeat implements TransportSeat
{
    public final PersistenceProvider persistenceProvider;

    public DefaultTransportSeat(PersistenceProvider persistenceProvider)
    {
        this.persistenceProvider = persistenceProvider;
    }

    @Override
    public void beginFileTransfer(CommunicationBridge bridge, Device device, long transferId, TransferItem.Type type)
            throws PersistenceException, CommunicationException
    {

    }

    @Override
    public void handleAcquaintanceRequest(Device device, DeviceAddress deviceAddress)
    {

    }

    @Override
    public void handleFileTransferRequest(Device device, boolean hasPin, long transferId, String jsonArray)
            throws PersistenceException, CommunicationException
    {

    }

    @Override
    public void handleFileTransferState(Device device, long transferId, boolean isAccepted)
    {

    }

    @Override
    public void handleTextTransfer(Device device, String text)
    {

    }

    @Override
    public boolean hasTransferFor(long transferId, String deviceUid, TransferItem.Type type)
    {
        return false;
    }

    @Override
    public boolean hasTransferIndexingFor(long transferId)
    {
        return false;
    }

    @Override
    public void notifyDeviceKeyChanged(Device device, int receiverKey, int senderKey)
    {

    }
}
