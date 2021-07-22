package org.monora.uprotocol.core.transfer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @see Transfers#toTransferItemList(String)
 */
public class MetaTransferItem
{
    /**
     * @see TransferItem#getItemId()
     */
    public long id;

    /**
     * @see TransferItem#getItemName()
     */
    public @NotNull String name;

    /**
     * @see TransferItem#getItemSize()
     */
    public long size;

    /**
     * @see TransferItem#getItemMimeType()
     */
    public @NotNull String mimeType;

    /**
     * @see TransferItem#getItemDirectory()
     */
    public @Nullable String directory;

    public MetaTransferItem(long id, @NotNull String name, long size, @NotNull String mimeType,
                            @Nullable String directory)
    {
        this.id = id;
        this.name = name;
        this.size = size;
        this.mimeType = mimeType;
        this.directory = directory;
    }
}
