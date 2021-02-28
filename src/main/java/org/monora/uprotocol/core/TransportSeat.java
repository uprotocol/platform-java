package org.monora.uprotocol.core;

import org.jetbrains.annotations.NotNull;
import org.monora.uprotocol.core.persistence.PersistenceException;
import org.monora.uprotocol.core.persistence.PersistenceProvider;
import org.monora.uprotocol.core.protocol.Client;
import org.monora.uprotocol.core.protocol.ClientAddress;
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
     * {@link TransferItem.Type#Incoming} or {@link Transfers#send(CommunicationBridge, TransferOperation, long)}
     * for {@link TransferItem.Type#Outgoing} types.
     *
     * @param bridge  The bridge that speaks on behalf of you when making requests. A connection wrapper.
     * @param client  That is making the request.
     * @param groupId {@link TransferItem#getItemGroupId()}.
     * @param type    Of the transfer.
     * @throws PersistenceException If some of the data is missing for this transfer (i.e., the remote doesn't have
     *                              some permissions enabled in the database).
     * @throws ProtocolException    If the remote doesn't have satisfactory permissions or sent invalid values.
     */
    void beginFileTransfer(@NotNull CommunicationBridge bridge, @NotNull Client client, long groupId,
                           @NotNull TransferItem.Type type)
            throws PersistenceException, ProtocolException;

    /**
     * The remote wants us to notice it.
     * <p>
     * If the user is about to pick a client, this should be the one that is picked.
     *
     * @param client        That wants to be noticed.
     * @param clientAddress Where that client resides.
     * @return True if the request will be fulfilled.
     * @see CommunicationBridge#requestAcquaintance()
     */
    boolean handleAcquaintanceRequest(@NotNull Client client, @NotNull ClientAddress clientAddress);

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
     * The remote has returned the answer to the file transfer request we made with
     * {@link CommunicationBridge#requestFileTransfer(long, List)}.
     * <p>
     * This may or not be called depending on the uprotocol client. You should not wait for this.
     *
     * @param client     That is informing us. You may need to check if it owns the transfer request.
     * @param groupId    That points to the transfer request.
     * @param isAccepted True if the remote has accepted the request.
     */
    void handleFileTransferState(@NotNull Client client, long groupId, boolean isAccepted);

    /**
     * Handle the text transfer request.
     *
     * @param client That sent the request.
     * @param text   That has been received.
     */
    void handleTextTransfer(@NotNull Client client, @NotNull String text);

    /**
     * Check whether there is an ongoing transfer for the given parameters.
     *
     * @param groupId   The transfer id as in {@link TransferItem#getItemGroupId()}
     * @param clientUid The {@link Client#getClientUid()} if this needs to concern only the given client, or null you
     *                  need check all transfer processes.
     * @param type      To limit the type of the transfer as in {@link TransferItem#getItemType()}.
     * @return True if there is an ongoing transfer for the given parameters.
     */
    boolean hasOngoingTransferFor(long groupId, @NotNull String clientUid, @NotNull TransferItem.Type type);

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
