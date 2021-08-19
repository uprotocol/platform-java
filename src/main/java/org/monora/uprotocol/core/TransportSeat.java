package org.monora.uprotocol.core;

import org.jetbrains.annotations.NotNull;
import org.monora.uprotocol.core.persistence.OnPrepareListener;
import org.monora.uprotocol.core.persistence.PersistenceException;
import org.monora.uprotocol.core.persistence.PersistenceProvider;
import org.monora.uprotocol.core.protocol.*;
import org.monora.uprotocol.core.protocol.communication.ProtocolException;
import org.monora.uprotocol.core.spec.v1.Config;
import org.monora.uprotocol.core.transfer.TransferItem;
import org.monora.uprotocol.core.transfer.TransferOperation;
import org.monora.uprotocol.core.transfer.Transfers;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

/**
 * Handles the requests coming from the uprotocol clients.
 * <p>
 * {@link TransportSession} takes an instance of this class during initialization, and redirects the requests to it, so
 * that you can handle the permissions, guarding, etc.
 * <p>
 * It is also used on certain requests made to a remote client.
 */
public interface TransportSeat
{
    /**
     * Start a file transfer operation. It may be you or the remote requesting this.
     * <p>
     * This is invoked after the checks, meaning you should not check for the validity of the transfer.
     * <p>
     * If a runtime failure occurs, you should just throw before starting the transfer. Also, additional errors should
     * be derivatives of {@link ProtocolException}.
     * <p>
     * The bridge ownership is transferred and any operation should be carried out on a separate thread, releasing
     * the caller thread.
     * <p>
     * Invoke {@link Transfers#receive(CommunicationBridge, TransferOperation, long)} for
     * {@link Direction#Incoming} or {@link Transfers#send(CommunicationBridge, TransferOperation, long)}
     * for {@link Direction#Outgoing} types.
     *
     * @param bridge    The bridge that speaks on behalf of you when making requests.
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
     * The remote wants you to notice it, and if you are about to pick a client, it should be the one you might want to
     * pick.
     * <p>
     * If the request is successful, you should use the bridge to make the request that you were going to do after
     * picking a client. If it is unsuccessful (e.g., the remote is standing in the same direction as you are), you
     * should send 'false' using {@link CommunicationBridge#send(boolean)}.
     * <p>
     * On a successful request, you can perform any request that you can normally do after connecting using
     * {@link CommunicationBridge#connect(ConnectionFactory, PersistenceProvider, InetAddress)}.
     *
     * @param bridge        The bridge that speaks on behalf of you when making requests.
     * @param client        That wants to be noticed.
     * @param clientAddress Where that client resides.
     * @param direction     Of the remote.
     * @throws IOException       If an IO error occurs.
     * @throws ProtocolException If something related to permissions or similar goes wrong.
     * @see CommunicationBridge#requestAcquaintance(TransportSeat, Direction)
     */
    void handleAcquaintanceRequest(@NotNull CommunicationBridge bridge, @NotNull Client client,
                                   @NotNull ClientAddress clientAddress, @NotNull Direction direction)
            throws IOException, ProtocolException;

    /**
     * Handle the clipboard request.
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
     * You may block the caller thread for a certain amount of time, which is {@link Config#TIMEOUT_SOCKET_DEFAULT} by
     * default.
     * <p>
     * Unless something fatal, do not throw {@link RuntimeException}.
     * <p>
     * The remote will consider the request successful unless an appropriate {@link ProtocolException} is thrown.
     * <p>
     * This will start the transfer process if it returns true, in which case, the
     * {@link TransportSeat#beginFileTransfer(CommunicationBridge, Client, long, Direction)} method will be invoked.
     * <p>
     * Returning false will mean you have prompted the user and he or she will decide whether the transfer is going
     * to be accepted or rejected.
     * <p>
     * The JSON array can be consumed using {@link Transfers#toTransferItemList(String)}.
     *
     * @param client    That is making the file transfer request.
     * @param hasPin    Whether the remote client had a valid PIN when it made this request.
     * @param groupId   The unique transfer id to mention a group of items.
     * @param jsonArray The transfer item data.
     * @return True if the transfer is accepted and is about to start, or false if the user prompt is needed, and it
     * will be started or rejected after it.
     * @throws PersistenceException If anything related to handling of the persistent data goes wrong.
     * @throws ProtocolException    If something related to permissions or similar goes wrong.
     */
    boolean handleFileTransferRequest(@NotNull Client client, boolean hasPin, long groupId, @NotNull String jsonArray)
            throws PersistenceException, ProtocolException;

    /**
     * The remote has rejected the file transfer request we made with
     * {@link CommunicationBridge#requestFileTransfer(long, List, OnPrepareListener)}.
     * <p>
     * You should return the result as soon as possible, so that it can be delivered to the remote.
     * <p>
     * This will be an asynchronous operation, in which case you should not expect an immediate reply. The remote
     * may choose to send a reply at an arbitrary time.
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
