package org.monora.uprotocol.core.transfer;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.monora.coolsocket.core.session.ActiveConnection;
import org.monora.coolsocket.core.session.CancelledException;
import org.monora.uprotocol.core.CommunicationBridge;
import org.monora.uprotocol.core.Responses;
import org.monora.uprotocol.core.TransportSeat;
import org.monora.uprotocol.core.io.StreamDescriptor;
import org.monora.uprotocol.core.persistence.OnPrepareListener;
import org.monora.uprotocol.core.persistence.PersistenceException;
import org.monora.uprotocol.core.persistence.PersistenceProvider;
import org.monora.uprotocol.core.protocol.Client;
import org.monora.uprotocol.core.protocol.communication.ContentException;
import org.monora.uprotocol.core.protocol.communication.ProtocolException;
import org.monora.uprotocol.core.spec.v1.Keyword;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles the communication part of a file transfer operation.
 */
public class Transfers
{
    /**
     * This is used after reading the JSON data for the operation safely using one of the
     * {@link CommunicationBridge#receiveChecked} methods.
     *
     * @param jsonObject To read the requested item from.
     * @return The requested item holder.
     * @throws JSONException If something goes wrong when inflating the JSON data.
     */
    public static @NotNull TransferRequest getTransferRequest(@NotNull JSONObject jsonObject) throws JSONException
    {
        return new TransferRequest(jsonObject.getLong(Keyword.TRANSFER_ID),
                jsonObject.getLong(Keyword.TRANSFER_CURRENT_POSITION));
    }

    /**
     * Request item from the remote.
     *
     * @param bridge          The bridge that speaks on behalf of you when making requests. A connection wrapper.
     * @param itemId          Corresponds to {@link TransferItem#getItemId()}.
     * @param currentPosition To skip if this item has recovered from an error, meaning it already has some bytes transferred
     *                        on your side.
     * @return True if the remote approved of the request.
     * @throws IOException       If an IO error occurs.
     * @throws JSONException     If something goes wrong when creating JSON object.
     * @throws ProtocolException When there is a communication error due to misconfiguration.
     */
    public static boolean requestItem(@NotNull CommunicationBridge bridge, long itemId, long currentPosition)
            throws IOException, JSONException, ProtocolException
    {
        bridge.send(true, new JSONObject()
                .put(Keyword.TRANSFER_ID, itemId)
                .put(Keyword.TRANSFER_CURRENT_POSITION, currentPosition));
        return bridge.receiveResult();
    }

    /**
     * Handle the receive process. You can invoke this method in the {@link TransportSeat#beginFileTransfer} method
     * when the type is {@link TransferItem.Type#Incoming}.
     * <p>
     * This can also be invoked when using {@link CommunicationBridge#requestFileTransferStart}.
     *
     * @param bridge    The bridge that speaks on behalf of you when making requests. A connection wrapper.
     * @param operation The operation object that handles the GUI side of things.
     * @param groupId   As in {@link TransferItem#getItemGroupId()}.
     * @see TransportSeat#beginFileTransfer
     * @see CommunicationBridge#requestFileTransferStart
     */
    public static void receive(@NotNull CommunicationBridge bridge, @NotNull TransferOperation operation, long groupId)
    {
        PersistenceProvider persistenceProvider = bridge.getPersistenceProvider();
        ActiveConnection activeConnection = bridge.getActiveConnection();
        Client client = bridge.getRemoteClient();
        TransferItem item;

        try {
            while ((item = persistenceProvider.getFirstReceivableItem(groupId)) != null) {
                operation.setOngoing(item);
                operation.publishProgress();

                // On the receiver side, we do not recover from permission or file system errors. This is why the
                // following file operation is not inside a try-catch block. Those types of errors are not recoverable
                // and there is no point in keeping on going.
                StreamDescriptor descriptor = persistenceProvider.getDescriptorFor(item);

                // Set the bytes as the size of the file that may have previously been exchanged partially.
                // This will be '0' for the newly started operations.
                operation.setBytesOngoing(descriptor.length(), descriptor.length());

                try (OutputStream outputStream = persistenceProvider.openOutputStream(descriptor)) {
                    // This if-block will throw an error if the result is false.
                    if (Transfers.requestItem(bridge, item.getItemId(), descriptor.length())) {
                        int len;
                        ActiveConnection.Description description = bridge.getActiveConnection().readBegin();
                        WritableByteChannel writableByteChannel = Channels.newChannel(outputStream);

                        while (description.hasAvailable() && (len = activeConnection.read(description)) != -1) {
                            operation.publishProgress();
                            operation.setBytesOngoing(operation.getBytesOngoing() + len, len);
                            writableByteChannel.write(description.byteBuffer);
                        }

                        outputStream.flush();
                        persistenceProvider.setState(client.getClientUid(), item, TransferItem.State.Done, null);
                        operation.setBytesTotal(operation.getBytesTotal() + operation.getBytesOngoing());
                        operation.setCount(operation.getCount() + 1);
                        operation.installReceivedContent(descriptor);
                        operation.clearBytesOngoing();
                    }
                } catch (CancelledException e) {
                    // The task is cancelled. We reset the state of this item to 'pending'.
                    persistenceProvider.setState(client.getClientUid(), item, TransferItem.State.Pending, e);
                    throw e;
                } catch (FileNotFoundException e) {
                    throw e;
                } catch (ContentException e) {
                    switch (e.error) {
                        case NotFound:
                            persistenceProvider.setState(client.getClientUid(), item, TransferItem.State.Invalidated, e);
                            break;
                        case AlreadyExists:
                        case NotAccessible:
                        default:
                            persistenceProvider.setState(client.getClientUid(), item,
                                    TransferItem.State.InvalidatedTemporarily, e);
                    }
                } catch (Exception e) {
                    persistenceProvider.setState(client.getClientUid(), item,
                            TransferItem.State.InvalidatedTemporarily, e);
                    throw e;
                } finally {
                    persistenceProvider.persist(client.getClientUid(), item);
                    operation.clearOngoing();
                }
            }

            bridge.send(false);

            if (operation.getCount() > 0) {
                operation.finishOperation();
            }
        } catch (CancelledException e) {
            operation.onCancelOperation();
        } catch (Exception e) {
            try {
                bridge.send(e);
            } catch (Exception e1) {
                operation.onUnhandledException(e);
            }
        }
    }

    /**
     * Handle the sending process. You can invoke this method via {@link TransportSeat#beginFileTransfer} method when
     * the type is {@link TransferItem.Type#Outgoing}.
     * <p>
     * This can also be invoked when using {@link CommunicationBridge#requestFileTransferStart}.
     *
     * @param bridge    The bridge that speaks on behalf of you when making requests. A connection wrapper.
     * @param operation The operation object that handles the GUI side of things.
     * @param groupId   As in {@link TransferItem#getItemGroupId()}.
     * @see TransportSeat#beginFileTransfer
     * @see CommunicationBridge#requestFileTransferStart
     */
    public static void send(@NotNull CommunicationBridge bridge, @NotNull TransferOperation operation, long groupId)
    {
        PersistenceProvider persistenceProvider = bridge.getPersistenceProvider();
        ActiveConnection activeConnection = bridge.getActiveConnection();
        Client client = bridge.getRemoteClient();
        TransferItem item;

        try {
            while (activeConnection.getSocket().isConnected()) {
                JSONObject request = bridge.receiveChecked();

                if (!Responses.getResult(request))
                    break;

                operation.publishProgress();

                try {
                    final TransferRequest transferRequest = Transfers.getTransferRequest(request);
                    item = persistenceProvider.loadTransferItem(client.getClientUid(), groupId, transferRequest.id,
                            TransferItem.Type.Outgoing);

                    operation.setOngoing(item);
                    operation.setBytesOngoing(transferRequest.position, transferRequest.position);

                    try {
                        StreamDescriptor descriptor = persistenceProvider.getDescriptorFor(item);
                        if (descriptor.length() != item.getItemSize())
                            throw new FileNotFoundException("File size has changed. It is probably a different file.");

                        try (InputStream inputStream = persistenceProvider.openInputStream(descriptor)) {
                            if (transferRequest.position > 0
                                    && inputStream.skip(transferRequest.position) != transferRequest.position)
                                throw new IOException("Failed to skip " + transferRequest.position + " bytes");

                            bridge.send(true);

                            // TODO: 7/20/21 This doesn't seem to update anything. Check if it does!
                            persistenceProvider.persist(client.getClientUid(), item);

                            ActiveConnection.Description description = activeConnection.writeBegin(0,
                                    item.getItemSize() - transferRequest.position);
                            byte[] bytes = new byte[8096];
                            int len;

                            // For avoiding Android MediaStore bug where the reported size is different than actual
                            // data size.
                            boolean exceedingClose = false;
                            long available;

                            while ((len = inputStream.read(bytes)) != -1) {
                                operation.publishProgress();

                                if (len > 0) {
                                    available = description.available();
                                    if (len > available) {
                                        len = (int) available;
                                        exceedingClose = true;
                                    }

                                    operation.setBytesOngoing(operation.getBytesOngoing() + len, len);
                                    activeConnection.write(description, bytes, 0, len);

                                    if (exceedingClose) {
                                        break;
                                    }
                                }
                            }

                            activeConnection.writeEnd(description);

                            operation.setBytesTotal(operation.getBytesTotal() + operation.getBytesOngoing());
                            operation.setCount(operation.getCount() + 1);
                            operation.clearBytesOngoing();
                            persistenceProvider.setState(client.getClientUid(), item, TransferItem.State.Done, null);
                        }
                    } catch (CancelledException e) {
                        persistenceProvider.setState(client.getClientUid(), item, TransferItem.State.Pending, e);
                        throw e;
                    } catch (FileNotFoundException e) {
                        persistenceProvider.setState(client.getClientUid(), item, TransferItem.State.Invalidated, e);
                        throw e;
                    } catch (Exception e) {
                        persistenceProvider.setState(client.getClientUid(), item,
                                TransferItem.State.InvalidatedTemporarily, e);
                        throw e;
                    } finally {
                        persistenceProvider.persist(client.getClientUid(), item);
                        operation.clearOngoing();
                    }
                } catch (CancelledException e) {
                    throw e;
                } catch (@NotNull FileNotFoundException | PersistenceException e) {
                    bridge.send(Keyword.ERROR_NOT_FOUND);
                } catch (IOException e) {
                    bridge.send(Keyword.ERROR_NOT_ACCESSIBLE);
                } catch (Exception e) {
                    bridge.send(Keyword.ERROR_UNKNOWN);
                }
            }
        } catch (CancelledException e) {
            operation.onCancelOperation();
        } catch (Exception e) {
            operation.onUnhandledException(e);
        }
    }

    /**
     * Transform a given {@link TransferItem} list into its {@link JSONArray} equivalent.
     * <p>
     * The resulting {@link JSONArray} can be fed to
     * {@link CommunicationBridge#requestFileTransfer(long, List, OnPrepareListener)} to start a file transfer operation.
     * <p>
     * You can have the same JSON data back using {@link #toTransferItemList(String)}.
     *
     * @param transferItemList To convert.
     * @return The JSON equivalent of the same list.
     */
    public static @NotNull JSONArray toJson(@NotNull List<@NotNull TransferItem> transferItemList)
    {
        JSONArray jsonArray = new JSONArray();

        for (TransferItem transferItem : transferItemList) {
            JSONObject json = new JSONObject()
                    .put(Keyword.TRANSFER_ID, transferItem.getItemId())
                    .put(Keyword.INDEX_FILE_NAME, transferItem.getItemName())
                    .put(Keyword.INDEX_FILE_SIZE, transferItem.getItemSize())
                    .put(Keyword.INDEX_FILE_MIME, transferItem.getItemMimeType());

            if (transferItem.getItemDirectory() != null) {
                json.put(Keyword.INDEX_DIRECTORY, transferItem.getItemDirectory());
            }

            jsonArray.put(json);
        }

        return jsonArray;
    }

    /**
     * Inflate the given JSON data that was received from the remote and make it consumable as a collection.
     *
     * @param jsonArray That is going to be inflated.
     * @return The list of items inflated from the JSON data.
     * @throws JSONException If the JSON data is corrupted or has missing/mismatch values.
     * @see #toJson(List)
     */
    public static @NotNull List<@NotNull MetaTransferItem> toTransferItemList(@NotNull String jsonArray)
            throws JSONException
    {
        JSONArray json = new JSONArray(jsonArray);
        List<MetaTransferItem> list = new ArrayList<>(0);

        if (json.length() > 0) {
            for (int i = 0; i < json.length(); i++) {
                JSONObject jsonObject = json.getJSONObject(i);
                String directory = jsonObject.has(Keyword.INDEX_DIRECTORY)
                        ? jsonObject.getString(Keyword.INDEX_DIRECTORY) : null;
                MetaTransferItem item = new MetaTransferItem(jsonObject.getLong(Keyword.TRANSFER_ID),
                        jsonObject.getString(Keyword.INDEX_FILE_NAME), jsonObject.getLong(Keyword.INDEX_FILE_SIZE),
                        jsonObject.getString(Keyword.INDEX_FILE_MIME), directory);

                list.add(item);
            }
        }

        return list;
    }
}
