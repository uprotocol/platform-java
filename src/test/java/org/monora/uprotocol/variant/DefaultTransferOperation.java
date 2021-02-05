package org.monora.uprotocol.variant;

import org.monora.uprotocol.core.io.StreamDescriptor;
import org.monora.uprotocol.core.transfer.TransferItem;
import org.monora.uprotocol.core.transfer.TransferOperation;

public class DefaultTransferOperation implements TransferOperation
{
    private TransferItem transferItem;

    private long bytesOngoing;

    private long bytesTotal;

    private int count;

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
    public TransferItem getOngoing()
    {
        return transferItem;
    }

    @Override
    public void installReceivedContent(StreamDescriptor descriptor)
    {

    }

    @Override
    public void onCancelOperation()
    {

    }

    @Override
    public void onUnhandledException(Exception e)
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
    public void setOngoing(TransferItem transferItem)
    {
        this.transferItem = transferItem;
    }
}
