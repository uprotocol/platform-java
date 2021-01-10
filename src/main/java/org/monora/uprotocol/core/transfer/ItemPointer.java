package org.monora.uprotocol.core.transfer;

/**
 * Holds the details for a requested {@link Transfer}.
 */
public class ItemPointer
{
    /**
     * Corresponds to {@link Transfer#getTransferId()}.
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
