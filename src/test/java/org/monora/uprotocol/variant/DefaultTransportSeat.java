package org.monora.uprotocol.variant;

import org.monora.uprotocol.core.CommunicationBridge;
import org.monora.uprotocol.core.TransportSeat;
import org.monora.uprotocol.core.network.Client;
import org.monora.uprotocol.core.network.ClientAddress;
import org.monora.uprotocol.core.network.TransferItem;
import org.monora.uprotocol.core.persistence.PersistenceException;
import org.monora.uprotocol.core.persistence.PersistenceProvider;
import org.monora.uprotocol.core.protocol.communication.ProtocolException;

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
    public void beginFileTransfer(CommunicationBridge bridge, Client client, long transferId, TransferItem.Type type)
            throws PersistenceException, ProtocolException
    {
        if (type.equals(TransferItem.Type.INCOMING))
            receiveFiles(bridge, transferId);
        else if (type.equals(TransferItem.Type.OUTGOING))
            sendFiles(bridge, transferId);
    }

    @Override
    public boolean handleAcquaintanceRequest(Client client, ClientAddress clientAddress)
    {
        return true;
    }

    @Override
    public void handleFileTransferRequest(Client client, boolean hasPin, long transferId, String jsonArray)
            throws PersistenceException, ProtocolException
    {
        List<TransferItem> itemList = persistenceProvider.toTransferItemList(transferId, jsonArray);
        persistenceProvider.save(client.uid, itemList);
    }

    @Override
    public void handleFileTransferState(Client client, long transferId, boolean isAccepted)
    {

    }

    @Override
    public void handleTextTransfer(Client client, String text)
    {
        System.out.println("Text received: " + text);
    }

    @Override
    public boolean hasOngoingTransferFor(long transferId, String clientUid, TransferItem.Type type)
    {
        return false;
    }

    @Override
    public boolean hasOngoingIndexingFor(long transferId)
    {
        return false;
    }

    @Override
    public void notifyClientCredentialsChanged(Client client)
    {
        if (autoAcceptNewKeys) {
            persistenceProvider.approveInvalidationOfCredentials(client);
        }
    }

    public void setAutoInvalidationOfCredentials(boolean autoAcceptNewKeys)
    {
        this.autoAcceptNewKeys = autoAcceptNewKeys;
    }
}
