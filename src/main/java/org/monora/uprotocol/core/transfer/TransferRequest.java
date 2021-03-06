package org.monora.uprotocol.core.transfer;

/**
 * Holds the details for a requested {@link TransferItem}.
 */
public class TransferRequest
{
    /**
     * Corresponds to {@link TransferItem#getItemId()}.
     */
    public final long id;

    /**
     * The position of bytes to start from when sending the data.
     * <p>
     * If skip fails, do not attempt to sending this item.
     */
    public final long position;

    TransferRequest(long id, long position)
    {
        this.id = id;
        this.position = position;
    }
}
