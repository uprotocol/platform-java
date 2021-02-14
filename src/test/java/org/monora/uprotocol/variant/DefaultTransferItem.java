package org.monora.uprotocol.variant;

import org.monora.uprotocol.core.transfer.TransferItem;

public class DefaultTransferItem implements TransferItem
{
    private long id;

    private long groupId;

    private String name;

    private String directory;

    private String mimeType;

    private long size;

    private long lastChangeTime;

    private Type type;

    public DefaultTransferItem(long groupId, long id, String name, String mimeType, long size, String directory,
                               TransferItem.Type type)
    {
        this.groupId = groupId;
        this.id = id;
        this.name = name;
        this.mimeType = mimeType;
        this.size = size;
        this.directory = directory;
        this.type = type;
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
    public void setItemGroupId(long groupId)
    {
        this.groupId = groupId;
    }

    @Override
    public void setItemName(String name)
    {
        this.name = name;
    }

    @Override
    public void setItemDirectory(String directory)
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
