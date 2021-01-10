package org.monora.uprotocol.core.transfer;

/**
 * Holds the details for a transfer item.
 */
public interface Transfer
{
    /**
     * The path in which this item should be stored.
     *
     * @return The relative path.
     * @see #setTransferDirectory(String)
     */
    String getTransferDirectory();

    /**
     * The unique of the item.
     *
     * @return The unique id.
     * @see #setTransferId(long)
     * @see #getTransferGroupId()
     */
    long getTransferId();

    /**
     * The last time this item was interacted with.
     *
     * @return The time in UNIX epoch format.
     * @see #setTransferItemLastChangeTime(long)
     */
    long getTransferLastChangeTime();

    /**
     * The MIME-type of the item.
     *
     * @return The MIME-type.
     * @see #setTransferMimeType(String)
     */
    String getTransferMimeType();

    /**
     * The name of this item.
     *
     * @return The item name.
     * @see #setTransferItemName(String)
     */
    String getTransferName();

    /**
     * The length (size) of the item in bytes.
     *
     * @return The size in bytes.
     */
    long getTransferSize();

    /**
     * The type of this item showing whether it is an incoming or outgoing transfer.
     *
     * @return The item type.
     * @see #setTransferType(Transfer.Type)
     */
    Transfer.Type getTransferType();

    /**
     * The group id of the item that specifies the group it belongs to.
     *
     * @return The group id.
     * @see #setTransferGroupId(long)
     * @see #getTransferId()
     */
    long getTransferGroupId();

    /**
     * Sets the item id that refers to the item individually.
     *
     * @param id Of the item.
     * @see #getTransferId()
     * @see #setTransferGroupId(long)
     */
    void setTransferId(long id);

    /**
     * Sets the group id that this item belongs to.
     *
     * @param groupId Of the group that the item belongs to.
     * @see #getTransferGroupId()
     * @see #setTransferId(long)
     */
    void setTransferGroupId(long groupId);

    /**
     * Sets the name of the item.
     *
     * @param name Of the item.
     * @see #getTransferName()
     */
    void setTransferItemName(String name);

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
     * @see #getTransferMimeType()
     */
    void setTransferMimeType(String mimeType);

    /**
     * The total length (size) of the item.
     *
     * @param size Of the item in bytes.
     * @see #getTransferSize()
     */
    void setTransferItemSize(long size);

    /**
     * Sets the last time this item was interacted with
     *
     * @param lastChangeTime In UNIX epoch format.
     * @see #getTransferLastChangeTime()
     */
    void setTransferItemLastChangeTime(long lastChangeTime);

    /**
     * Sets the type of the item specifying whether it is an incoming or outgoing item.
     *
     * @param type Of the item.
     * @see #getTransferType()
     */
    void setTransferType(Transfer.Type type);

    public enum Type
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
