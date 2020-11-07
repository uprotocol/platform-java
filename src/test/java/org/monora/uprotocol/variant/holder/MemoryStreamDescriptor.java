package org.monora.uprotocol.variant.holder;

import org.monora.uprotocol.core.network.TransferItem;
import org.monora.uprotocol.core.persistence.StreamDescriptor;

import java.io.ByteArrayOutputStream;

/**
 * This class will match with its other instances regardless of the direction of the{@link TransferItem} it holds, that
 * is, {@link org.monora.uprotocol.core.network.TransferItem.Type#INCOMING} will <b>NOT</b> a difference.
 */
public class MemoryStreamDescriptor implements StreamDescriptor
{
    public final ByteArrayOutputStream data;

    public final TransferItem transferItem;

    MemoryStreamDescriptor(TransferItem transferItem)
    {
        this.transferItem = transferItem;
        this.data = new ByteArrayOutputStream((int) transferItem.size);
    }

    public static MemoryStreamDescriptor newInstance(TransferItem transferItem)
    {
        if (transferItem.size < 0 || transferItem.size >= Short.MAX_VALUE)
            throw new ArrayIndexOutOfBoundsException("Transfer item size cannot be larger than " + Short.MAX_VALUE
                    + " or smaller than 0");
        return new MemoryStreamDescriptor(transferItem);
    }

    @Override
    public long length()
    {
        return data.size();
    }
}
