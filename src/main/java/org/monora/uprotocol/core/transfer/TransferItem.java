package org.monora.uprotocol.core.transfer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.monora.uprotocol.core.spec.v1.Keyword;

/**
 * Holds the details for a transfer item.
 */
public interface TransferItem
{
    /**
     * The path in which this item should be stored.
     *
     * @return The relative path.
     * @see #setItemDirectory(String)
     */
    @Nullable String getItemDirectory();

    /**
     * The group id of the item that specifies the group it belongs to.
     *
     * @return The group id.
     * @see #setItemGroupId(long)
     * @see #getItemId()
     */
    long getItemGroupId();

    /**
     * The unique of the item.
     *
     * @return The unique id.
     * @see #setItemId(long)
     * @see #getItemGroupId()
     */
    long getItemId();

    /**
     * The last time this item was interacted with.
     *
     * @return The time in UNIX epoch format.
     * @see #setItemLastChangeTime(long)
     */
    long getItemLastChangeTime();

    /**
     * The MIME-type of the item.
     *
     * @return The MIME-type.
     * @see #setItemMimeType(String)
     */
    @NotNull String getItemMimeType();

    /**
     * The name of this item.
     *
     * @return The item name.
     * @see #setItemName(String)
     */
    @NotNull String getItemName();

    /**
     * The length (size) of the item in bytes.
     *
     * @return The size in bytes.
     */
    long getItemSize();

    /**
     * The type of this item showing whether it is an incoming or outgoing transfer.
     *
     * @return The item type.
     * @see #setItemType(Type)
     */
    @NotNull Type getItemType();

    /**
     * Sets the relative path that this item should be in.
     *
     * @param directory Or the relative path that the item should be in.
     */
    void setItemDirectory(@Nullable String directory);

    /**
     * Sets the group id that this item belongs to.
     *
     * @param groupId Of the group that the item belongs to.
     * @see #getItemGroupId()
     * @see #setItemId(long)
     */
    void setItemGroupId(long groupId);

    /**
     * Sets the item id that refers to the item individually.
     *
     * @param id Of the item.
     * @see #getItemId()
     * @see #setItemGroupId(long)
     */
    void setItemId(long id);

    /**
     * Sets the name of the item.
     *
     * @param name Of the item.
     * @see #getItemName()
     */
    void setItemName(@NotNull String name);

    /**
     * Sets the last time this item was interacted with
     *
     * @param lastChangeTime In UNIX epoch format.
     * @see #getItemLastChangeTime()
     */
    void setItemLastChangeTime(long lastChangeTime);


    /**
     * Sets the MIME-type of the item.
     *
     * @param mimeType Of the item.
     * @see #getItemMimeType()
     */
    void setItemMimeType(@NotNull String mimeType);

    /**
     * The total length (size) of the item.
     *
     * @param size Of the item in bytes.
     * @see #getItemSize()
     */
    void setItemSize(long size);

    /**
     * Sets the type of the item specifying whether it is an incoming or outgoing item.
     *
     * @param type Of the item.
     * @see #getItemType()
     */
    void setItemType(@NotNull Type type);

    /**
     * The enum for the type of the transfer item.
     */
    enum Type
    {
        /**
         * The item is incoming
         */
        Incoming(Keyword.TRANSFER_TYPE_INCOMING),

        /**
         * The item is outgoing.
         */
        Outgoing(Keyword.TRANSFER_TYPE_OUTGOING);

        /**
         * The value that the protocol specifies which is different from the platform-based enum value.
         */
        public final String protocolValue;

        Type(String protocolValue)
        {
            this.protocolValue = protocolValue;
        }

        public static @NotNull Type from(String value)
        {
            for (Type type : values())
                if (type.protocolValue.equals(value))
                    return type;

            throw new IllegalArgumentException("Unknown type: " + value);
        }
    }
}
