package org.monora.uprotocol.core.transfer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.monora.uprotocol.core.CommunicationBridge;
import org.monora.uprotocol.core.io.StreamDescriptor;
import org.monora.uprotocol.core.persistence.PersistenceException;
import org.monora.uprotocol.core.persistence.PersistenceProvider;
import org.monora.uprotocol.core.protocol.Direction;

import java.io.IOException;

/**
 * Handles the user side of things of a transfer operation, i.e. updating the GUI with the info it holds like the
 * progress.
 * <p>
 * The data it handles should wait until the {@link #publishProgress()} method is invoked so that the worker thread
 * doesn't get too many interruptions, which can affect the health of the process.
 */
public interface TransferOperation
{
    /**
     * Clear the bytes counter of the ongoing content {@link #getOngoing()}.
     */
    void clearBytesOngoing();

    /**
     * Clear the ongoing content from {@link #getOngoing()}.
     */
    void clearOngoing();

    /**
     * Finish this operation with at least one successful delivery.
     * <p>
     * Publish notifications in this method.
     */
    void finishOperation();

    /**
     * The bytes exchanged for {@link #getOngoing()}. This is not included in {@link #getBytesTotal()} until the content
     * exchange is completed successfully.
     *
     * @return The bytes exchanged for a content that is still ongoing.
     * @see #setBytesOngoing(long, long)
     * @see #getBytesTotal()
     */
    long getBytesOngoing();

    /**
     * The total number of bytes exchanged with this operation.
     * <p>
     * This does not include the bytes for the content in exchange {@link #getOngoing()}. For that, use
     * {@link #getBytesOngoing()}.
     *
     * @return The total number of bytes exchanged for this operation.
     * @see #setBytesTotal(long)
     * @see #getBytesOngoing()
     */
    long getBytesTotal();

    /**
     * The total number of content that has been exchanged during this operation.
     *
     * @return The total number of successful delivery of content.
     */
    int getCount();

    /**
     * This will return the descriptor that points to the file that is received or sent.
     * <p>
     * For instance, this can be a file descriptor or a network stream of which only the name, size and location are
     * known.
     *
     * @param transferItem For which the descriptor will be generated.
     * @return The generated descriptor.
     * @throws IOException When this fails to create a descriptor for this transfer item.
     * @see PersistenceProvider#openInputStream(StreamDescriptor)
     * @see PersistenceProvider#openOutputStream(StreamDescriptor)
     */
    @NotNull StreamDescriptor getDescriptorFor(@NotNull TransferItem transferItem) throws IOException;

    /**
     * This will return the first valid item that this side can receive.
     *
     * @return The transfer receivable item or null if there are none.
     */
    @Nullable TransferItem getFirstReceivableItem();

    /**
     * The content that is in an ongoing exchange process.
     *
     * @return The content.
     * @see #setOngoing(TransferItem)
     */
    @Nullable TransferItem getOngoing();

    /**
     * Load transfer item for the given parameters.
     *
     * @param id        Points to {@link TransferItem#getItemId()}.
     * @param direction Specifying whether this is an incoming or outgoing operation.
     * @return The transfer item that points to the given parameters or null if there is no match.
     * @throws PersistenceException When the given parameters don't point to a valid item.
     */
    @NotNull TransferItem loadTransferItem(long id, @NotNull Direction direction) throws PersistenceException;

    /**
     * Change the state of the given item.
     * <p>
     * Note: this should set the state but should not update it since saving it is spared for
     * {@link PersistenceProvider#persist(String, TransferItem)} unless the state is held on a different location.
     *
     * @param item  Of which the given state will be applied.
     * @param state The level of invalidation.
     * @param e     The nullable additional exception cause this state.
     * @see TransferItem.State
     */
    void setState(@NotNull TransferItem item, @NotNull TransferItem.State state, @Nullable Exception e);

    /**
     * Install a received file to its final location.
     *
     * @param descriptor That belongs to the content and specifies its location.
     */
    void installReceivedContent(@NotNull StreamDescriptor descriptor);

    /**
     * Invoked when an operation is cancelled by the user.
     */
    void onCancelOperation();

    /**
     * Invoked when {@link CommunicationBridge} fails to handle an exception.
     *
     * @param e The unhandled exception.
     */
    void onUnhandledException(@NotNull Exception e);

    /**
     * Update and save the existing data.
     */
    void publishProgress();

    /**
     * Sets the bytes belonging to the transfer that is still ongoing. After it completes, the bytes will be
     * added to the total bytes by invoking {@link #setBytesTotal(long)}.
     *
     * @param bytes         The total bytes that should be the new value of {@link #getBytesOngoing()}.
     * @param bytesIncrease The increase in bytes that has been added to the {@code byte} param.
     * @see #getBytesOngoing()
     * @see #setBytesTotal(long)
     */
    void setBytesOngoing(long bytes, long bytesIncrease);

    /**
     * Sets the total bytes that has been exchanged with this operation.
     * <p>
     * The value provided should be the new value of {@link #getBytesTotal()}.
     * <p>
     * This is invoked once a content completes successfully.
     *
     * @param bytes The total bytes that should be the new value of {@link #getBytesTotal()}.
     * @see #getBytesTotal()
     * @see #setBytesOngoing(long, long)
     */
    void setBytesTotal(long bytes);

    /**
     * Set the total number of content that has been exchanged and will be the next value of {@link #getCount()}.
     *
     * @param count The total number.
     * @see #getCount()
     */
    void setCount(int count);

    /**
     * Sets the content that is in exchange and is the new value of {@link #getOngoing()}.
     *
     * @param transferItem That is ongoing. This is a non-null value.
     * @see #getOngoing()
     */
    void setOngoing(@NotNull TransferItem transferItem);
}
