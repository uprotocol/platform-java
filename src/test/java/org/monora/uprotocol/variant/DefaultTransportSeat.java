package org.monora.uprotocol.variant;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.monora.uprotocol.core.CommunicationBridge;
import org.monora.uprotocol.core.TransportSeat;
import org.monora.uprotocol.core.persistence.PersistenceException;
import org.monora.uprotocol.core.protocol.Client;
import org.monora.uprotocol.core.protocol.ClientAddress;
import org.monora.uprotocol.core.protocol.Direction;
import org.monora.uprotocol.core.protocol.communication.ProtocolException;
import org.monora.uprotocol.core.transfer.MetaTransferItem;
import org.monora.uprotocol.core.transfer.TransferItem;
import org.monora.uprotocol.core.transfer.TransferOperation;
import org.monora.uprotocol.core.transfer.Transfers;
import org.monora.uprotocol.variant.persistence.BasePersistenceProvider;

import java.util.ArrayList;
import java.util.List;

public class DefaultTransportSeat implements TransportSeat
{
    public final BasePersistenceProvider persistenceProvider;

    public final TransferOperation transferOperation;

    private boolean autoAcceptNewKeys;

    public boolean replyToAcquaintanceRequest = false;

    private @Nullable Direction requestedAcquaintanceDirection = null;

    public DefaultTransportSeat(@NotNull BasePersistenceProvider persistenceProvider,
                                @NotNull TransferOperation transferOperation)
    {
        this.persistenceProvider = persistenceProvider;
        this.transferOperation = transferOperation;
    }

    public @Nullable Direction getRequestedAcquaintanceDirection() {
        return requestedAcquaintanceDirection;
    }

    @Override
    public void beginFileTransfer(@NotNull CommunicationBridge bridge, @NotNull Client client, long groupId,
                                  @NotNull Direction direction)
            throws PersistenceException, ProtocolException
    {
        if (direction.equals(Direction.Incoming)) {
            Transfers.receive(bridge, transferOperation, groupId);
        } else if (direction.equals(Direction.Outgoing)) {
            Transfers.send(bridge, transferOperation, groupId);
        }
    }

    @Override
    public boolean handleAcquaintanceRequest(@NotNull Client client, @NotNull ClientAddress clientAddress,
                                             @NotNull Direction direction)
    {
        requestedAcquaintanceDirection = direction;
        return replyToAcquaintanceRequest;
    }

    @Override
    public void handleFileTransferRequest(@NotNull Client client, boolean hasPin, long groupId, @NotNull String jsonArray)
            throws PersistenceException, ProtocolException
    {
        List<MetaTransferItem> metaList = Transfers.toTransferItemList(jsonArray);
        List<TransferItem> transferItemList = new ArrayList<>(metaList.size());

        for (MetaTransferItem metaItem : metaList) {
            TransferItem item = persistenceProvider.createTransferItemFor(groupId, metaItem.id, metaItem.name,
                    metaItem.mimeType, metaItem.size, metaItem.directory, Direction.Incoming);
            transferItemList.add(item);
        }

        persistenceProvider.persist(client.getClientUid(), transferItemList);
    }

    @Override
    public boolean handleFileTransferRejection(@NotNull Client client, long groupId)
    {
        return persistenceProvider.removeTransfer(client, groupId);
    }

    @Override
    public void handleTextTransfer(@NotNull Client client, @NotNull String text)
    {
        System.out.println("Text received: " + text);
    }

    @Override
    public boolean hasOngoingTransferFor(long groupId, @NotNull String clientUid, @NotNull Direction direction)
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
