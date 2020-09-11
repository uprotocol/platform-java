package org.monora.uprotocol.core.transfer;

public class RequestedItem
{
    public final long itemId;

    public final long currentBytes;

    RequestedItem(long itemId, long currentBytes)
    {
        this.itemId = itemId;
        this.currentBytes = currentBytes;
    }
}
