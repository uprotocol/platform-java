package org.monora.uprotocol.core.transfer;

import org.monora.uprotocol.core.network.TransferItem;

/**
 * Holds the details for a requested {@link TransferItem}.
 */
public class ItemPointer
{
    /**
     * Corresponds to {@link TransferItem#id}.
     */
    public final long itemId;

    /**
     * The position of bytes to start from when sending the data.
     * <p>
     * If skip fails, do not attempt to sending this item.
     */
    public final long position;

    ItemPointer(long itemId, long position)
    {
        this.itemId = itemId;
        this.position = position;
    }
}
