package org.monora.uprotocol.core.transfer;

/**
 * Holds the details for a transfer item.
 */
public interface TransferItem
{
    /**
     * The path in which this item should be stored.
     *
     * @return The relative path.
     * @see #setTransferDirectory(String)
     */
    String getItemDirectory();

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
    String getItemMimeType();

    /**
     * The name of this item.
     *
     * @return The item name.
     * @see #setItemName(String)
     */
    String getItemName();

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
     * @see #setItemType(TransferItem.Type)
     */
    TransferItem.Type getItemType();

    /**
     * The group id of the item that specifies the group it belongs to.
     *
     * @return The group id.
     * @see #setTransferGroupId(long)
     * @see #getItemId()
     */
    long getItemGroupId();

    /**
     * Sets the item id that refers to the item individually.
     *
     * @param id Of the item.
     * @see #getItemId()
     * @see #setTransferGroupId(long)
     */
    void setItemId(long id);

    /**
     * Sets the group id that this item belongs to.
     *
     * @param groupId Of the group that the item belongs to.
     * @see #getItemGroupId()
     * @see #setItemId(long)
     */
    void setTransferGroupId(long groupId);

    /**
     * Sets the name of the item.
     *
     * @param name Of the item.
     * @see #getItemName()
     */
    void setItemName(String name);

    /**
     * Sets the relative path that this item should be in.
     *
     * @param directory Or the relative path that the item should be in.
     */
    void setTransferDirectory(String directory);

    /**
     * Sets the MIME-type of the item.
     *
     * @param mimeType Of the item.
     * @see #getItemMimeType()
     */
    void setItemMimeType(String mimeType);

    /**
     * The total length (size) of the item.
     *
     * @param size Of the item in bytes.
     * @see #getItemSize()
     */
    void setItemSize(long size);

    /**
     * Sets the last time this item was interacted with
     *
     * @param lastChangeTime In UNIX epoch format.
     * @see #getItemLastChangeTime()
     */
    void setItemLastChangeTime(long lastChangeTime);

    /**
     * Sets the type of the item specifying whether it is an incoming or outgoing item.
     *
     * @param type Of the item.
     * @see #getItemType()
     */
    void setItemType(TransferItem.Type type);

    /**
     * The enum for the type of the transfer item.
     */
    enum Type
    {
        /**
         * The item is incoming
         */
        INCOMING,

        /**
         * The item is outgoing.
         */
        OUTGOING
    }
}
