package org.monora.uprotocol.variant.holder;

import org.monora.uprotocol.core.transfer.Transfer;
import org.monora.uprotocol.core.io.StreamDescriptor;

import java.io.ByteArrayOutputStream;

/**
 * This class will match with its other instances regardless of the direction of the{@link Transfer} it holds, that
 * is, {@link Transfer.Type#INCOMING} will <b>NOT</b> a difference.
 */
public class MemoryStreamDescriptor implements StreamDescriptor
{
    public final ByteArrayOutputStream data;

    public final Transfer transfer;

    MemoryStreamDescriptor(Transfer transfer)
    {
        this.transfer = transfer;
        this.data = new ByteArrayOutputStream((int) transfer.getTransferSize());
    }

    public static MemoryStreamDescriptor newInstance(Transfer transfer)
    {
        if (transfer.getTransferSize() < 0 || transfer.getTransferSize() >= Short.MAX_VALUE)
            throw new ArrayIndexOutOfBoundsException("Transfer item size cannot be larger than " + Short.MAX_VALUE
                    + " or smaller than 0");
        return new MemoryStreamDescriptor(transfer);
    }

    @Override
    public long length()
    {
        return data.size();
    }
}
