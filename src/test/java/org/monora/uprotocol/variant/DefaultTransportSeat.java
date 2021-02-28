package org.monora.uprotocol.variant;

import org.jetbrains.annotations.NotNull;
import org.monora.uprotocol.core.CommunicationBridge;
import org.monora.uprotocol.core.TransportSeat;
import org.monora.uprotocol.core.persistence.PersistenceException;
import org.monora.uprotocol.core.persistence.PersistenceProvider;
import org.monora.uprotocol.core.protocol.Client;
import org.monora.uprotocol.core.protocol.ClientAddress;
import org.monora.uprotocol.core.protocol.communication.ProtocolException;
import org.monora.uprotocol.core.transfer.TransferItem;
import org.monora.uprotocol.core.transfer.TransferOperation;
import org.monora.uprotocol.core.transfer.Transfers;

import java.util.List;

public class DefaultTransportSeat implements TransportSeat
{
    public final PersistenceProvider persistenceProvider;

    public final TransferOperation transferOperation;

    private boolean autoAcceptNewKeys;

    public DefaultTransportSeat(@NotNull PersistenceProvider persistenceProvider,
                                @NotNull TransferOperation transferOperation)
    {
        this.persistenceProvider = persistenceProvider;
        this.transferOperation = transferOperation;
    }

    @Override
    public void beginFileTransfer(@NotNull CommunicationBridge bridge, @NotNull Client client, long groupId,
                                  @NotNull TransferItem.Type type)
            throws PersistenceException, ProtocolException
    {
        if (type.equals(TransferItem.Type.Incoming))
            Transfers.receive(bridge, transferOperation, groupId);
        else if (type.equals(TransferItem.Type.Outgoing))
            Transfers.send(bridge, transferOperation, groupId);
    }

    @Override
    public boolean handleAcquaintanceRequest(@NotNull Client client, @NotNull ClientAddress clientAddress)
    {
        return true;
    }

    @Override
    public void handleFileTransferRequest(@NotNull Client client, boolean hasPin, long groupId, @NotNull String jsonArray)
            throws PersistenceException, ProtocolException
    {
        List<TransferItem> transferItemList = persistenceProvider.toTransferItemList(groupId, jsonArray);
        persistenceProvider.persist(client.getClientUid(), transferItemList);
    }

    @Override
    public void handleFileTransferState(@NotNull Client client, long groupId, boolean isAccepted)
    {

    }

    @Override
    public void handleTextTransfer(@NotNull Client client, @NotNull String text)
    {
        System.out.println("Text received: " + text);
    }

    @Override
    public boolean hasOngoingTransferFor(long groupId, @NotNull String clientUid, @NotNull TransferItem.Type type)
    {
        return false;
    }

    @Override
    public boolean hasOngoingIndexingFor(long groupId)
    {
        return false;
    }

    @Override
    public void notifyClientCredentialsChanged(@NotNull Client client)
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
