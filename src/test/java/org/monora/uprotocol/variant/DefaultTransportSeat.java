package org.monora.uprotocol.variant;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.monora.uprotocol.core.CommunicationBridge;
import org.monora.uprotocol.core.TransportSeat;
import org.monora.uprotocol.core.protocol.Client;
import org.monora.uprotocol.core.protocol.ClientAddress;
import org.monora.uprotocol.core.protocol.ClipboardType;
import org.monora.uprotocol.core.protocol.Direction;
import org.monora.uprotocol.core.protocol.communication.ProtocolException;
import org.monora.uprotocol.core.transfer.MetaTransferItem;
import org.monora.uprotocol.core.transfer.TransferItem;
import org.monora.uprotocol.core.transfer.TransferOperation;
import org.monora.uprotocol.core.transfer.Transfers;
import org.monora.uprotocol.variant.holder.ClipboardHolder;
import org.monora.uprotocol.variant.holder.TransferRequestHolder;
import org.monora.uprotocol.variant.persistence.BasePersistenceProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DefaultTransportSeat implements TransportSeat
{
    public final @NotNull BasePersistenceProvider persistenceProvider;

    public boolean autoAcceptNewKeys;

    public boolean startTransferByDefault = false;

    public @Nullable TransferRequestHolder transferRequestOnGuidance = null;

    public @Nullable ClipboardHolder requestedClipboard = null;

    public DefaultTransportSeat(@NotNull BasePersistenceProvider persistenceProvider)
    {
        this.persistenceProvider = persistenceProvider;
    }

    @Override
    public void beginFileTransfer(@NotNull CommunicationBridge bridge, @NotNull Client client, long groupId,
                                  @NotNull Direction direction)
    {
        TransferOperation transferOperation = new DefaultTransferOperation(persistenceProvider,
                client.getClientUid(), groupId);

        if (direction.equals(Direction.Incoming)) {
            Transfers.receive(bridge, transferOperation);
        } else if (direction.equals(Direction.Outgoing)) {
            Transfers.send(bridge, transferOperation);
        }
    }

    @Override
    public void handleGuidanceRequest(@NotNull CommunicationBridge bridge, @NotNull Client client,
                                      @NotNull ClientAddress clientAddress, @NotNull Direction direction)
            throws IOException
    {
        final @Nullable TransferRequestHolder holder = transferRequestOnGuidance;
        if (holder != null) {
            try {
                if (bridge.requestFileTransfer(holder.groupId, holder.list, null)) {
                    beginFileTransfer(bridge, client, holder.groupId, Direction.Outgoing);
                }
            } catch (ProtocolException e) {
                e.printStackTrace();
            }
        } else {
            bridge.send(false);
        }
    }

    @Override
    public boolean handleFileTransferRequest(@NotNull Client client, boolean hasPin, long groupId,
                                             @NotNull String jsonArray)
    {
        List<MetaTransferItem> metaList = Transfers.toTransferItemList(jsonArray);
        List<TransferItem> transferItemList = new ArrayList<>(metaList.size());

        for (MetaTransferItem metaItem : metaList) {
            TransferItem item = persistenceProvider.createTransferItemFor(groupId, metaItem.id, metaItem.name,
                    metaItem.mimeType, metaItem.size, metaItem.directory, Direction.Incoming);
            transferItemList.add(item);
        }

        persistenceProvider.persist(client.getClientUid(), transferItemList);

        return startTransferByDefault;
    }

    @Override
    public boolean handleFileTransferRejection(@NotNull Client client, long groupId)
    {
        return persistenceProvider.removeTransfer(client, groupId);
    }

    @Override
    public boolean handleClipboardRequest(@NotNull Client client, @NotNull String content, @NotNull ClipboardType type)
    {
        this.requestedClipboard = new ClipboardHolder(content, type);
        return true;
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
}
