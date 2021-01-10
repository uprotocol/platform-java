package org.monora.uprotocol.variant;

import org.monora.uprotocol.core.transfer.Transfer;

public class DefaultTransferItem implements Transfer
{
    public long id;

    public long groupId;

    public String name;

    public String directory;

    public String mimeType;

    public long size = 0;

    public long lastChangeTime;

    public Type type = Type.INCOMING;

    public DefaultTransferItem(long groupId, long id, String name, String mimeType, long size, String directory,
                               Transfer.Type type)
    {
        setTransferGroupId(groupId);
        setTransferId(id);
        setTransferItemName(name);
        setTransferMimeType(mimeType);
        setTransferItemSize(size);
        setTransferDirectory(directory);
        setTransferType(type);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof Transfer) {
            Transfer other = (Transfer) obj;
            return getTransferType() != null && getTransferType().equals(other.getTransferType())
                    && getTransferGroupId() == other.getTransferGroupId()
                    && getTransferId() == other.getTransferId();
        }

        return super.equals(obj);
    }

    @Override
    public String getTransferDirectory()
    {
        return directory;
    }

    @Override
    public long getTransferId()
    {
        return id;
    }

    @Override
    public long getTransferLastChangeTime()
    {
        return lastChangeTime;
    }

    @Override
    public String getTransferMimeType()
    {
        return mimeType;
    }

    @Override
    public String getTransferName()
    {
        return name;
    }

    @Override
    public long getTransferSize()
    {
        return size;
    }

    @Override
    public Type getTransferType()
    {
        return type;
    }

    @Override
    public long getTransferGroupId()
    {
        return groupId;
    }

    @Override
    public void setTransferId(long id)
    {
        this.id = id;
    }

    @Override
    public void setTransferGroupId(long groupId)
    {
        this.groupId = groupId;
    }

    @Override
    public void setTransferItemName(String name)
    {
        this.name = name;
    }

    @Override
    public void setTransferDirectory(String directory)
    {
        this.directory = directory;
    }

    @Override
    public void setTransferMimeType(String mimeType)
    {
        this.mimeType = mimeType;
    }

    @Override
    public void setTransferItemSize(long size)
    {
        this.size = size;
    }

    @Override
    public void setTransferItemLastChangeTime(long lastChangeTime)
    {
        this.lastChangeTime = lastChangeTime;
    }

    @Override
    public void setTransferType(Type type)
    {
        this.type = type;
    }
}
