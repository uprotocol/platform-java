package org.monora.uprotocol.variant;

import org.monora.uprotocol.core.transfer.TransferItem;

public class DefaultTransferItemItem implements TransferItem
{
    public long id;

    public long groupId;

    public String name;

    public String directory;

    public String mimeType;

    public long size = 0;

    public long lastChangeTime;

    public Type type = Type.INCOMING;

    public DefaultTransferItemItem(long groupId, long id, String name, String mimeType, long size, String directory,
                                   TransferItem.Type type)
    {
        setTransferGroupId(groupId);
        setItemId(id);
        setItemName(name);
        setItemMimeType(mimeType);
        setItemSize(size);
        setTransferDirectory(directory);
        setItemType(type);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof TransferItem) {
            TransferItem other = (TransferItem) obj;
            return getItemType() != null && getItemType().equals(other.getItemType())
                    && getItemGroupId() == other.getItemGroupId() && getItemId() == other.getItemId();
        }

        return super.equals(obj);
    }

    @Override
    public String getItemDirectory()
    {
        return directory;
    }

    @Override
    public long getItemId()
    {
        return id;
    }

    @Override
    public long getItemLastChangeTime()
    {
        return lastChangeTime;
    }

    @Override
    public String getItemMimeType()
    {
        return mimeType;
    }

    @Override
    public String getItemName()
    {
        return name;
    }

    @Override
    public long getItemSize()
    {
        return size;
    }

    @Override
    public Type getItemType()
    {
        return type;
    }

    @Override
    public long getItemGroupId()
    {
        return groupId;
    }

    @Override
    public void setItemId(long id)
    {
        this.id = id;
    }

    @Override
    public void setTransferGroupId(long groupId)
    {
        this.groupId = groupId;
    }

    @Override
    public void setItemName(String name)
    {
        this.name = name;
    }

    @Override
    public void setTransferDirectory(String directory)
    {
        this.directory = directory;
    }

    @Override
    public void setItemMimeType(String mimeType)
    {
        this.mimeType = mimeType;
    }

    @Override
    public void setItemSize(long size)
    {
        this.size = size;
    }

    @Override
    public void setItemLastChangeTime(long lastChangeTime)
    {
        this.lastChangeTime = lastChangeTime;
    }

    @Override
    public void setItemType(Type type)
    {
        this.type = type;
    }
}
