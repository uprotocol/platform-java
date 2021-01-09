package org.monora.uprotocol.core;

import org.json.JSONObject;
import org.monora.coolsocket.core.response.SizeOverflowException;
import org.monora.coolsocket.core.session.ActiveConnection;
import org.monora.coolsocket.core.session.CancelledException;
import org.monora.uprotocol.core.network.Client;
import org.monora.uprotocol.core.network.ClientAddress;
import org.monora.uprotocol.core.network.TransferItem;
import org.monora.uprotocol.core.persistence.PersistenceException;
import org.monora.uprotocol.core.persistence.PersistenceProvider;
import org.monora.uprotocol.core.persistence.StreamDescriptor;
import org.monora.uprotocol.core.protocol.communication.ContentException;
import org.monora.uprotocol.core.protocol.communication.ProtocolException;
import org.monora.uprotocol.core.spec.v1.Keyword;
import org.monora.uprotocol.core.transfer.ItemPointer;
import org.monora.uprotocol.core.transfer.Transfers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
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
     *
     * @param bridge     The bridge that speaks on behalf of you when making requests. A connection wrapper.
     * @param client     That is making the request.
     * @param transferId {@link TransferItem#transferId}
     * @param type       Of the transfer.
     * @throws PersistenceException If some of the data is missing for this transfer (i.e., the remote doesn't have
     *                              some permissions enabled in the database).
     * @throws ProtocolException    If the remote doesn't have satisfactory permissions or sent invalid values.
     */
    void beginFileTransfer(CommunicationBridge bridge, Client client, long transferId, TransferItem.Type type)
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
    boolean handleAcquaintanceRequest(Client client, ClientAddress clientAddress);

    /**
     * Handle the file transfer request.
     * <p>
     * Error-checking should be done prior to inflating the JSON data and any error should be thrown.
     * <p>
     * Unless something fatal, do not throw {@link RuntimeException}.
     * <p>
     * Anything after the checks should be done on a separate thread.
     *
     * @param client     That is making the file transfer request.
     * @param hasPin     Whether the remote client had a valid PIN when it made this request.
     * @param transferId The unique transfer id to mention a group of items.
     * @param jsonArray  The transfer item data.
     * @throws PersistenceException If anything related to handling of the persistent data goes wrong.
     * @throws ProtocolException    If something related to permissions or similar goes wrong.
     */
    void handleFileTransferRequest(Client client, boolean hasPin, long transferId, String jsonArray)
            throws PersistenceException, ProtocolException;

    /**
     * The remote has returned the answer to the file transfer request we made with
     * {@link CommunicationBridge#requestFileTransfer(long, List)}.
     * <p>
     * This may or not be called depending on the uprotocol client. You should not wait for this.
     *
     * @param client     That is informing us. You may need to check if it owns the transfer request.
     * @param transferId That points to the transfer request.
     * @param isAccepted True if the remote has accepted the request.
     */
    void handleFileTransferState(Client client, long transferId, boolean isAccepted);

    /**
     * Handle the text transfer request.
     *
     * @param client That sent the request.
     * @param text   That has been received.
     */
    void handleTextTransfer(Client client, String text);

    /**
     * Check whether there is an ongoing transfer for the given parameters.
     *
     * @param transferId The transfer id as in {@link TransferItem#transferId}
     * @param clientUid  The {@link Client#getClientUid()} if this needs to concern only the given client, or null you
     *                   need check all transfer processes.
     * @param type       To limit the type of the transfer as in {@link TransferItem#type}.
     * @return True if there is an ongoing transfer for the given parameters.
     */
    boolean hasOngoingTransferFor(long transferId, String clientUid, TransferItem.Type type);

    /**
     * Check whether there is an indexing process for the given transfer id.
     * <p>
     * The 'id' is the {@link org.monora.uprotocol.core.network.TransferItem#transferId} field.
     *
     * @param transferId The transfer id as in {@link TransferItem#transferId}
     * @return True if there is an ongoing transfer indexing for the given id.
     */
    boolean hasOngoingIndexingFor(long transferId);

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
    void notifyClientCredentialsChanged(Client client);

    /**
     * Handle the receive process. You can invoke this method in the {@link #beginFileTransfer} method when the type is
     * {@link org.monora.uprotocol.core.network.TransferItem.Type#INCOMING}.
     * <p>
     * You should not override this default method unless that is really what you need.
     *
     * @param bridge     The bridge that speaks on behalf of you when making requests. A connection wrapper.
     * @param transferId As in {@link TransferItem#transferId}.
     */
    default void receiveFiles(CommunicationBridge bridge, long transferId)
    {
        PersistenceProvider persistenceProvider = bridge.getPersistenceProvider();
        ActiveConnection activeConnection = bridge.getActiveConnection();
        Client client = bridge.getRemoteClient();
        TransferItem item;

        // TODO: 11/6/20 This should belong to the task manager making the ETA calculation.
        TransferItem lastItem;
        long currentBytes = 0;
        long completedBytes = 0;
        long completedCount = 0;

        try {
            while ((item = persistenceProvider.getFirstReceivableItem(transferId)) != null) {
                // TODO: 11/6/20 Update the state here

                // On the receiver side, we do not recover from permission or file system errors. This is why the
                // following file operation is not inside a try-catch block. Those types of errors are not recoverable
                // and there is no point in keeping on going.
                StreamDescriptor descriptor = persistenceProvider.getDescriptorFor(item);
                int itemState = PersistenceProvider.STATE_INVALIDATED_TEMPORARILY;
                currentBytes = descriptor.length();

                try (OutputStream outputStream = persistenceProvider.openOutputStream(descriptor)) {
                    // This if-block will throw an error if the result is false.
                    if (Transfers.requestItem(bridge, item.id, currentBytes)) {
                        int len;
                        ActiveConnection.Description description = bridge.getActiveConnection().readBegin();
                        WritableByteChannel writableByteChannel = Channels.newChannel(outputStream);

                        while (description.hasAvailable() && (len = activeConnection.read(description)) != -1) {
                            // TODO: 11/6/20 Update the item state here.

                            currentBytes += len;
                            writableByteChannel.write(description.byteBuffer);
                        }

                        outputStream.flush();
                        persistenceProvider.setState(client.getClientUid(), item, PersistenceProvider.STATE_DONE,
                                null);
                        completedBytes += currentBytes;
                        completedCount++;
                        lastItem = item;

                        // TODO: 11/6/20 Save the file here
                    }
                } catch (CancelledException e) {
                    // The task is cancelled. We reset the state of this item to 'pending'.
                    persistenceProvider.setState(client.getClientUid(), item, PersistenceProvider.STATE_PENDING, e);
                    throw e;
                } catch (FileNotFoundException e) {
                    throw e;
                } catch (ContentException e) {
                    switch (e.error) {
                        case NotFound:
                            persistenceProvider.setState(client.getClientUid(), item,
                                    PersistenceProvider.STATE_INVALIDATED_STICKY, e);
                            break;
                        case AlreadyExists:
                        case NotAccessible:
                        default:
                            persistenceProvider.setState(client.getClientUid(), item,
                                    PersistenceProvider.STATE_INVALIDATED_TEMPORARILY, e);
                    }
                } catch (Exception e) {
                    persistenceProvider.setState(client.getClientUid(), item,
                            PersistenceProvider.STATE_INVALIDATED_TEMPORARILY, e);
                    throw e;
                } finally {
                    persistenceProvider.save(client.getClientUid(), item);
                    item = null;
                }
            }

            CommunicationBridge.sendResult(activeConnection, false);

            if (completedCount > 0) {
                // TODO: 11/6/20 Notify the total number of files received.
            }
        } catch (CancelledException e) {

        } catch (Exception e) {
            try {
                bridge.sendError(e);
            } catch (Exception e1) {
                // TODO: 11/6/20 Notify errors
            }
        }
    }

    /**
     * Handle the sending process. You can invoke this method in the {@link #beginFileTransfer} method when the type is
     * {@link org.monora.uprotocol.core.network.TransferItem.Type#OUTGOING}.
     * <p>
     * You should not override this default method unless that is what you need.
     *
     * @param bridge     The bridge that speaks on behalf of you when making requests. A connection wrapper.
     * @param transferId As in {@link TransferItem#transferId}.
     */
    default void sendFiles(CommunicationBridge bridge, long transferId)
    {
        PersistenceProvider persistenceProvider = bridge.getPersistenceProvider();
        ActiveConnection activeConnection = bridge.getActiveConnection();
        Client client = bridge.getRemoteClient();
        TransferItem item;

        // TODO: 11/6/20 These variables belong to the ETA calculator.
        long currentBytes = 0;
        long completedBytes = 0;
        int completedCount = 0;

        try {
            while (activeConnection.getSocket().isConnected()) {
                // TODO: 11/6/20 Publish status here.
                JSONObject request = bridge.receiveSecure();

                if (!CommunicationBridge.resultOf(request))
                    break;

                try {
                    final ItemPointer itemPointer = Transfers.getItemRequest(request);
                    item = persistenceProvider.loadTransferItem(client.getClientUid(), transferId, itemPointer.itemId,
                            TransferItem.Type.OUTGOING);
                    currentBytes = itemPointer.position;

                    try {
                        StreamDescriptor descriptor = persistenceProvider.getDescriptorFor(item);
                        if (descriptor.length() != item.size)
                            // FIXME: 11/6/20 Is it a good idea to throw an unrelated error? Probably not.
                            throw new FileNotFoundException("File size has changed. It is probably a different file.");

                        try (InputStream inputStream = persistenceProvider.openInputStream(descriptor)) {
                            if (inputStream == null)
                                throw new FileNotFoundException("The input stream failed to open.");

                            if (currentBytes > 0 && inputStream.skip(currentBytes) != currentBytes)
                                throw new IOException("Failed to skip " + currentBytes + " bytes");

                            bridge.sendResult(true);

                            persistenceProvider.setState(client.getClientUid(), item,
                                    PersistenceProvider.STATE_IN_PROGRESS, null);
                            persistenceProvider.save(client.getClientUid(), item);

                            ActiveConnection.Description description = activeConnection.writeBegin(0,
                                    item.size - currentBytes);
                            byte[] bytes = new byte[8096];
                            int readLength;

                            try {
                                while ((readLength = inputStream.read(bytes)) != -1) {
                                    // TODO: 11/6/20 Publish item status here.

                                    if (readLength > 0) {
                                        currentBytes += readLength;
                                        activeConnection.write(description, bytes, 0, readLength);
                                    }
                                }

                                activeConnection.writeEnd(description);
                            } catch (SizeOverflowException ignored) {
                            }

                            completedBytes += currentBytes;
                            completedCount++;
                            persistenceProvider.setState(client.getClientUid(), item, PersistenceProvider.STATE_DONE,
                                    null);
                        }
                    } catch (CancelledException e) {
                        persistenceProvider.setState(client.getClientUid(), item, PersistenceProvider.STATE_PENDING, e);
                        throw e;
                    } catch (FileNotFoundException e) {
                        persistenceProvider.setState(client.getClientUid(), item,
                                PersistenceProvider.STATE_INVALIDATED_STICKY, e);
                        throw e;
                    } catch (Exception e) {
                        persistenceProvider.setState(client.getClientUid(), item,
                                PersistenceProvider.STATE_INVALIDATED_TEMPORARILY, e);
                        throw e;
                    } finally {
                        persistenceProvider.save(client.getClientUid(), item);
                        item = null;
                    }
                } catch (CancelledException e) {
                    throw e;
                } catch (FileNotFoundException | PersistenceException e) {
                    bridge.sendError(Keyword.ERROR_NOT_FOUND);
                } catch (IOException e) {
                    bridge.sendError(Keyword.ERROR_NOT_ACCESSIBLE);
                } catch (Exception e) {
                    bridge.sendError(Keyword.ERROR_UNKNOWN);
                }
            }
        } catch (CancelledException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
            // TODO: 11/6/20 Handle the error
        }
    }
}
