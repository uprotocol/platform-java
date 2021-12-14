package org.monora.uprotocol.variant;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.monora.uprotocol.core.io.StreamDescriptor;
import org.monora.uprotocol.core.persistence.PersistenceException;
import org.monora.uprotocol.core.protocol.Direction;
import org.monora.uprotocol.core.transfer.TransferItem;
import org.monora.uprotocol.core.transfer.TransferOperation;
import org.monora.uprotocol.variant.holder.MemoryStreamDescriptor;
import org.monora.uprotocol.variant.holder.TransferHolder;
import org.monora.uprotocol.variant.persistence.BasePersistenceProvider;

import java.util.List;

public class DefaultTransferOperation implements TransferOperation
{
    private final @NotNull BasePersistenceProvider persistenceProvider;

    private final @NotNull String clientUid;

    private final long groupId;

    private @Nullable TransferItem transferItem;

    private long bytesOngoing;

    private long bytesTotal;

    private int count;

    // TODO: 12/14/21 Add the test for debugCancelled variable.
    private boolean debugCancelled = false;

    public DefaultTransferOperation(@NotNull BasePersistenceProvider persistenceProvider,
                                    @NotNull String clientUid, long groupId)
    {
        this.persistenceProvider = persistenceProvider;
        this.clientUid = clientUid;
        this.groupId = groupId;
    }

    @Override
    public void clearBytesOngoing()
    {
        bytesOngoing = 0;
    }

    @Override
    public void clearOngoing()
    {
        transferItem = null;
    }

    @Override
    public void finishOperation()
    {

    }

    @Override
    public long getBytesOngoing()
    {
        return bytesOngoing;
    }

    @Override
    public long getBytesTotal()
    {
        return bytesTotal;
    }

    @Override
    public int getCount()
    {
        return count;
    }

    @Override
    public @NotNull StreamDescriptor getDescriptorFor(@NotNull TransferItem transferItem)
    {
        return persistenceProvider.getOrInitializeDescriptorFor(transferItem);
    }

    @Override
    public @Nullable TransferItem getFirstReceivableItem()
    {
        List<TransferHolder> holderList = persistenceProvider.getTransferHolderList();

        for (TransferHolder holder : holderList) {
            if (Direction.Incoming.equals(holder.item.getItemDirection())
                    && holder.item.getItemGroupId() == groupId
                    && TransferItem.State.Pending.equals(holder.state)) {
                return holder.item;
            }
        }
        return null;
    }

    @Override
    public @Nullable TransferItem getOngoing()
    {
        return transferItem;
    }

    @Override
    public void installReceivedContent(@NotNull StreamDescriptor descriptor)
    {

    }

    @Override
    public @NotNull TransferItem loadTransferItem(long id, @NotNull Direction direction) throws PersistenceException
    {
        List<TransferHolder> holderList = persistenceProvider.getTransferHolderList();

        for (TransferHolder holder : holderList) {
            if (holder.item.getItemGroupId() == groupId && holder.item.getItemId() == id
                    && holder.item.getItemDirection().equals(direction) && holder.clientUid.equals(clientUid))
                return holder.item;
        }

        throw new PersistenceException("There is no transfer data matching the given parameters.");
    }

    @Override
    public void onCancelOperation()
    {
        debugCancelled = true;
    }

    @Override
    public void onUnhandledException(@NotNull Exception e)
    {

    }

    @Override
    public void publishProgress()
    {

    }

    @Override
    public void setBytesOngoing(long bytes, long bytesIncrease)
    {
        bytesOngoing = bytes;
    }

    @Override
    public void setBytesTotal(long bytes)
    {
        bytesTotal = bytes;
    }

    @Override
    public void setCount(int count)
    {
        this.count = count;
    }

    @Override
    public void setOngoing(@NotNull TransferItem transferItem)
    {
        this.transferItem = transferItem;
    }

    @Override
    public void setState(@NotNull TransferItem item, @NotNull TransferItem.State state, @Nullable Exception e)
    {
        List<TransferHolder> holderList = persistenceProvider.getTransferHolderList();

        for (TransferHolder holder : holderList) {
            if (clientUid.equals(holder.clientUid) && item.equals(holder.item)) {
                holder.state = state;
            }
        }
    }
}
