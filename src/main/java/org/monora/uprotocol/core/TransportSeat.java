package org.monora.uprotocol.core;

import org.json.JSONArray;
import org.monora.uprotocol.core.network.Device;
import org.monora.uprotocol.core.network.DeviceAddress;
import org.monora.uprotocol.core.network.TransferItem;
import org.monora.uprotocol.core.persistence.PersistenceException;
import org.monora.uprotocol.core.protocol.communication.CommunicationException;

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
     * @param device     That is making the request.
     * @param transferId {@link TransferItem#transferId}
     * @param type       Of the transfer.
     */
    void beginFileTransfer(CommunicationBridge bridge, Device device, long transferId, TransferItem.Type type)
            throws PersistenceException, CommunicationException;

    /**
     * The remote wants us to notice it.
     * <p>
     * If the user is about to pick a device, this should be the one that is picked.
     *
     * @param device        That wants to be noticed.
     * @param deviceAddress Where that device is located.
     */
    void handleAcquaintanceRequest(Device device, DeviceAddress deviceAddress);

    /**
     * Handle the file transfer request.
     * <p>
     * Error-checking should be done prior to inflating the JSON data and any error should be thrown.
     * <p>
     * Unless something fatal, do not throw {@link RuntimeException}.
     * <p>
     * Anything after the checks should be done on a separate thread.
     *
     * @param device     That is making the file transfer request.
     * @param hasPin     Whether this device had a valid PIN when it made this request.
     * @param transferId The unique transfer id to mention a group of items.
     * @param jsonArray  The transfer item data.
     * @throws PersistenceException   If anything related to handling of the persistent data goes wrong.
     * @throws CommunicationException If something related to permissions or similar goes wrong.
     */
    void handleFileTransfer(Device device, boolean hasPin, long transferId, String jsonArray)
            throws PersistenceException, CommunicationException;

    /**
     * The remote has returned the answer to the file transfer request we made with
     * {@link CommunicationBridge#requestFileTransfer(long, JSONArray)}.
     * <p>
     * This may or not be called depending on the uprotocol client. You should not wait for this.
     *
     * @param device     That is informing us. You may need to check if it owns the transfer request.
     * @param transferId That points to the transfer request.
     * @param isAccepted True if the remote has accepted the request.
     */
    void handleFileTransferState(Device device, long transferId, boolean isAccepted);

    /**
     * Handle the text transfer request.
     *
     * @param device That sent the request.
     * @param text   That has been received.
     */
    void handleTextTransfer(Device device, String text);

    /**
     * Check whether there is an ongoing transfer for the given parameters.
     *
     * @param transferId The transfer id as in {@link TransferItem#transferId}
     * @param deviceUid  The {@link Device#uid} if this needs to concern only the given device, or null
     *                   you need check any transfer process for any device.
     * @param type       To limit the type of the transfer as in {@link TransferItem#type}.
     * @return True if there is an ongoing transfer for the given parameters.
     */
    boolean hasTransferFor(long transferId, String deviceUid, TransferItem.Type type);

    /**
     * Check whether there is an indexing process for the given transfer id.
     * <p>
     * The 'id' is the {@link org.monora.uprotocol.core.network.TransferItem#transferId} field.
     *
     * @param transferId The transfer id as in {@link TransferItem#transferId}
     * @return True if there is an ongoing transfer indexing for the given id.
     */
    boolean hasTransferIndexingFor(long transferId);

    /**
     * A device that was previously known accessed the server with a different key.
     * <p>
     * This is a suspicious event that should be handled in cooperation with user.
     * <p>
     * The next requests from this client will be blocked until this is resolved. You will need to unblock the device
     * {@link Device#isBlocked} if user approves of the new key.
     *
     * @param device      That has accessed the server.
     * @param receiverKey That the device sent but doesn't match.
     * @param senderKey   The new sender key that we want to use if user accepts the new receiver key from the remote.
     */
    void notifyDeviceKeyChanged(Device device, int receiverKey, int senderKey);
}
