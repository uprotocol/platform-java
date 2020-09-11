package org.monora.uprotocol.core.transfer;

import org.monora.uprotocol.core.network.TransferItem;

/**
 * Hols the details for a requested {@link TransferItem}.
 */
public class RequestedItem
{
    /**
     * Corresponds to {@link TransferItem#id}.
     */
    public final long itemId;

    /**
     * The position (skip bytes as many) to start when sending data for this item.
     * <p>
     * If skip fails, do not attempt to sending this item.
     */
    public final long currentBytes;

    RequestedItem(long itemId, long currentBytes)
    {
        this.itemId = itemId;
        this.currentBytes = currentBytes;
    }
}
