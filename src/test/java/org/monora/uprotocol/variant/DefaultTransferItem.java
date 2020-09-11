package org.monora.uprotocol.variant;

import org.monora.uprotocol.core.network.TransferItem;

public class DefaultTransferItem extends TransferItem
{
    public DefaultTransferItem(long transferId, long id, String name, String mimeType, long size, String file,
                               String directory, TransferItem.Type type)
    {
        this.transferId = transferId;
        this.id = id;
        this.name = name;
        this.mimeType = mimeType;
        this.size = size;
        this.file = file;
        this.directory = directory;
        this.type = type;
    }
}
