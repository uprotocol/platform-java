package org.monora.uprotocol.variant;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.monora.uprotocol.core.content.Direction;
import org.monora.uprotocol.core.transfer.TransferItem;

public class DefaultTransferItem implements TransferItem
{
    private long id;

    private long groupId;

    private @NotNull String name;

    private @Nullable String directory;

    private @NotNull String mimeType;

    private long size;

    private long lastChangeTime;

    private @NotNull Direction direction;

    public DefaultTransferItem(long groupId, long id, @NotNull String name, @NotNull String mimeType, long size,
                               @Nullable String directory, @NotNull Direction direction)
    {
        this.groupId = groupId;
        this.id = id;
        this.name = name;
        this.mimeType = mimeType;
        this.size = size;
        this.directory = directory;
        this.direction = direction;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof TransferItem) {
            TransferItem other = (TransferItem) obj;
            return getItemDirection().equals(other.getItemDirection()) && getItemGroupId() == other.getItemGroupId()
                    && getItemId() == other.getItemId();
        }

        return super.equals(obj);
    }

    @Override
    public @NotNull Direction getItemDirection()
    {
        return direction;
    }

    @Override
    public @Nullable String getItemDirectory()
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
    public @NotNull String getItemMimeType()
    {
        return mimeType;
    }

    @Override
    public @NotNull String getItemName()
    {
        return name;
    }

    @Override
    public long getItemSize()
    {
        return size;
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
    public void setItemName(@NotNull String name)
    {
        this.name = name;
    }

    @Override
    public void setItemDirection(@NotNull Direction direction)
    {
        this.direction = direction;
    }

    @Override
    public void setItemDirectory(@Nullable String directory)
    {
        this.directory = directory;
    }

    @Override
    public void setItemMimeType(@NotNull String mimeType)
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
}
