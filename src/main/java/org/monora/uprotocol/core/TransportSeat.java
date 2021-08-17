package org.monora.uprotocol.core;

import org.jetbrains.annotations.NotNull;
import org.monora.uprotocol.core.persistence.OnPrepareListener;
import org.monora.uprotocol.core.persistence.PersistenceException;
import org.monora.uprotocol.core.persistence.PersistenceProvider;
import org.monora.uprotocol.core.protocol.Client;
import org.monora.uprotocol.core.protocol.ClientAddress;
import org.monora.uprotocol.core.protocol.ClipboardType;
import org.monora.uprotocol.core.protocol.Direction;
import org.monora.uprotocol.core.protocol.communication.ProtocolException;
import org.monora.uprotocol.core.transfer.TransferItem;
import org.monora.uprotocol.core.transfer.TransferOperation;
import org.monora.uprotocol.core.transfer.Transfers;

import java.util.List;

/**
 * A transport seat instance handles the requests coming from the uprotocol clients.
 * <p>
 * {@link TransportSession} takes an instance of this class during initialization, and redirect the requests to it, so
 * that you can handle the permissions, guarding, etc.
 */
public interface TransportSeat
{
    /**
     * The remote is ready to transfer files.
     * <p>
     * The file transfer should be made on the same thread since the bridge belongs to {@link TransportSession}.
     * <p>
     * Invoke {@link Transfers#receive(CommunicationBridge, TransferOperation, long)} for
     * {@link Direction#Incoming} or {@link Transfers#send(CommunicationBridge, TransferOperation, long)}
     * for {@link Direction#Outgoing} types.
     *
     * @param bridge    The bridge that speaks on behalf of you when making requests. A connection wrapper.
     * @param client    That is making the request.
     * @param groupId   {@link TransferItem#getItemGroupId()}.
     * @param direction Of the transfer.
     * @throws PersistenceException If some data is missing for this transfer (i.e., the remote doesn't have some
     *                              permissions enabled in the database).
     * @throws ProtocolException    If the remote doesn't have satisfactory permissions or sent invalid values.
     */
    void beginFileTransfer(@NotNull CommunicationBridge bridge, @NotNull Client client, long groupId,
                           @NotNull Direction direction)
            throws PersistenceException, ProtocolException;

    /**
     * The remote wants us to notice it.
     * <p>
     * If the user is about to pick a client, this should be the one that is picked.
     * <p>
     * This invocation should not block for too long as the result will be returned to the remote.
     *
     * @param client        That wants to be noticed.
     * @param clientAddress Where that client resides.
     * @param direction     Of the remote that you should fulfill.
     * @return True if the request will be fulfilled, or false if it is an unexpected request or the direction
     * doesn't match to what you expect.
     * @see CommunicationBridge#requestAcquaintance(Direction)
     */
    boolean handleAcquaintanceRequest(@NotNull Client client, @NotNull ClientAddress clientAddress,
                                      @NotNull Direction direction);

    /**
     * Handle the text transfer request.
     *
     * @param client  That sent the request.
     * @param content To handle.
     * @param type    Of the content.
     * @return True if the handling of the request was successful.
     * @see CommunicationBridge#requestClipboard(String, ClipboardType)
     */
    boolean handleClipboardRequest(@NotNull Client client, @NotNull String content, @NotNull ClipboardType type);

    /**
     * Handle the file transfer request.
     * <p>
     * Error-checking should be done prior to inflating the JSON data and any error should be thrown.
     * <p>
     * Unless something fatal, do not throw {@link RuntimeException}.
     * <p>
     * Anything after the checks should be done on a separate thread.
     *
     * @param client    That is making the file transfer request.
     * @param hasPin    Whether the remote client had a valid PIN when it made this request.
     * @param groupId   The unique transfer id to mention a group of items.
     * @param jsonArray The transfer item data.
     * @throws PersistenceException If anything related to handling of the persistent data goes wrong.
     * @throws ProtocolException    If something related to permissions or similar goes wrong.
     */
    void handleFileTransferRequest(@NotNull Client client, boolean hasPin, long groupId, @NotNull String jsonArray)
            throws PersistenceException, ProtocolException;

    /**
     * The remote has rejected the file transfer request we made with
     * {@link CommunicationBridge#requestFileTransfer(long, List, OnPrepareListener)}.
     * <p>
     * You should return the result as soon as possible, so that it can be delivered to the remote.
     * <p>
     * This will be an asynchronous operation, in which case you should not expect an immediate reply. The remote
     * may choose send a reply at an arbitrary time.
     *
     * @param client  That is informing us and should own the transfer.
     * @param groupId That points to the transfer request.
     * @return True if the transfer existed and belonged to the remote and marked as rejected (or removed).
     * @see CommunicationBridge#requestNotifyTransferRejection(long)
     */
    boolean handleFileTransferRejection(@NotNull Client client, long groupId);

    /**
     * Check whether there is an ongoing transfer for the given parameters.
     *
     * @param groupId   The transfer id as in {@link TransferItem#getItemGroupId()}
     * @param clientUid The {@link Client#getClientUid()} if this needs to concern only the given client, or null you
     *                  need check all transfer processes.
     * @param direction To limit the direction of the transfer as in {@link TransferItem#getItemDirection()}.
     * @return True if there is an ongoing transfer for the given parameters.
     */
    boolean hasOngoingTransferFor(long groupId, @NotNull String clientUid, @NotNull Direction direction);

    /**
     * Check whether there is an indexing process for the given transfer id.
     * <p>
     * The 'id' is the {@link TransferItem#getItemGroupId()} field.
     *
     * @param groupId The transfer id as in {@link TransferItem#getItemGroupId()}
     * @return True if there is an ongoing transfer indexing for the given id.
     */
    boolean hasOngoingIndexingFor(long groupId);

    /**
     * A known client could not connect due to a mismatch in credentials.
     * <p>
     * This is a suspicious event that should be handled in cooperation with user.
     * <p>
     * This request will occur only once unless {@link PersistenceProvider#saveRequestForInvalidationOfCredentials(String)}
     * doesn't save the request and {@link PersistenceProvider#hasRequestForInvalidationOfCredentials(String)} returns
     * {@code true}.
     *
     * @param client That has accessed the server.
     * @see PersistenceProvider#saveRequestForInvalidationOfCredentials(String)
     */
    void notifyClientCredentialsChanged(@NotNull Client client);
}
