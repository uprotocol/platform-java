package org.monora.uprotocol.core.transfer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.monora.uprotocol.core.persistence.PersistenceProvider;
import org.monora.uprotocol.core.protocol.Direction;

/**
 * Holds the details for a transfer item.
 */
public interface TransferItem
{
    /**
     * The type of this item showing whether it is an incoming or outgoing transfer.
     *
     * @return The item type.
     * @see #setItemDirection(Direction)
     */
    @NotNull Direction getItemDirection();

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
     * Sets the direction of the item specifying whether it is an incoming or outgoing item.
     *
     * @param direction Of the item.
     * @see #getItemDirection()
     */
    void setItemDirection(@NotNull Direction direction);

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
     * The persistent state of an item.
     */
    enum State
    {
        /**
         * The item is in pending state. It can have a temporary location.
         * <p>
         * In the case of incoming files, if you force set this state, keep the temporary file location as we can later
         * restart this item, resuming from where it was left.
         * <p>
         * This is the only state that will feed the {@link PersistenceProvider#getFirstReceivableItem(long)}
         * invocations.
         */
        Pending(Constants.PENDING),

        /**
         * The item is invalidated temporarily. The reason for that may be an unexpected connection drop not recovered.
         * <p>
         * The user can reset this state to {@link #Pending}.
         */
        InvalidatedTemporarily(Constants.INVALIDATED_TEMPORARILY),

        /**
         * The item is invalidated indefinitely because its length has changed or the sender file no longer exits.
         * <p>
         * The user should <b>NOT</b> be able to remove this flag, e.g., setting a valid state such as {@link #Pending}.
         */
        Invalidated(Constants.INVALIDATED),

        /**
         * The transaction for the item has finished.
         * <p>
         * The user should <b>NOT</b> able to remove this flag.
         */
        Done(Constants.DONE);

        State(String constantString)
        {
            if (!this.name().equals(constantString)) {
                throw new IllegalArgumentException(this.name() + " enum doesn't match the constant value "
                        + constantString);
            }
        }

        /**
         * Convenience class that keeps different state types as compile-time values.
         */
        public static class Constants
        {
            public static final String
                    PENDING = "Pending",
                    INVALIDATED_TEMPORARILY = "InvalidatedTemporarily",
                    INVALIDATED = "Invalidated",
                    DONE = "Done";
        }
    }
}
