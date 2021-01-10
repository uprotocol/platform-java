package org.monora.uprotocol.variant;

import org.monora.uprotocol.core.CommunicationBridge;
import org.monora.uprotocol.core.TransportSeat;
import org.monora.uprotocol.core.network.Client;
import org.monora.uprotocol.core.network.ClientAddress;
import org.monora.uprotocol.core.transfer.Transfer;
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
    public void beginFileTransfer(CommunicationBridge bridge, Client client, long groupId, Transfer.Type type)
            throws PersistenceException, ProtocolException
    {
        if (type.equals(Transfer.Type.INCOMING))
            receiveFiles(bridge, groupId);
        else if (type.equals(Transfer.Type.OUTGOING))
            sendFiles(bridge, groupId);
    }

    @Override
    public boolean handleAcquaintanceRequest(Client client, ClientAddress clientAddress)
    {
        return true;
    }

    @Override
    public void handleFileTransferRequest(Client client, boolean hasPin, long groupId, String jsonArray)
            throws PersistenceException, ProtocolException
    {
        List<Transfer> transferList = persistenceProvider.toTransferList(groupId, jsonArray);
        persistenceProvider.save(client.getClientUid(), transferList);
    }

    @Override
    public void handleFileTransferState(Client client, long groupId, boolean isAccepted)
    {

    }

    @Override
    public void handleTextTransfer(Client client, String text)
    {
        System.out.println("Text received: " + text);
    }

    @Override
    public boolean hasOngoingTransferFor(long groupId, String clientUid, Transfer.Type type)
    {
        return false;
    }

    @Override
    public boolean hasOngoingIndexingFor(long groupId)
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
