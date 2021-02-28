package org.monora.uprotocol.variant.holder;

import org.jetbrains.annotations.NotNull;
import org.monora.uprotocol.core.io.StreamDescriptor;
import org.monora.uprotocol.core.transfer.TransferItem;

import java.io.ByteArrayOutputStream;

/**
 * This class will match with its other instances regardless of the direction of the{@link TransferItem} it holds, that
 * is, {@link TransferItem.Type#Incoming} will <b>NOT</b> a difference.
 */
public class MemoryStreamDescriptor implements StreamDescriptor
{
    public final @NotNull ByteArrayOutputStream data;

    public final @NotNull TransferItem transferItem;

    MemoryStreamDescriptor(@NotNull TransferItem transferItem)
    {
        this.transferItem = transferItem;
        this.data = new ByteArrayOutputStream((int) transferItem.getItemSize());
    }

    public static @NotNull MemoryStreamDescriptor newInstance(@NotNull TransferItem transferItem)
    {
        if (transferItem.getItemSize() < 0 || transferItem.getItemSize() >= Short.MAX_VALUE)
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
