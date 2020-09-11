package org.monora.uprotocol.core.transfer;

import org.json.JSONException;
import org.json.JSONObject;
import org.monora.uprotocol.core.CommunicationBridge;
import org.monora.uprotocol.core.network.TransferItem;
import org.monora.uprotocol.core.protocol.communication.CommunicationException;
import org.monora.uprotocol.core.spec.alpha.Keyword;

import java.io.IOException;

/**
 * Handles the communication part of a file transfer operation.
 */
public class Transfers
{
    /**
     * This is used after reading the JSON data for the operation safely using one of the
     * {@link CommunicationBridge#receiveSecure} methods.
     *
     * @param jsonObject To read the requested item from.
     * @return The requested item holder.
     * @throws JSONException If something goes wrong when inflating the JSON data.
     */
    public RequestedItem getRequestedItem(JSONObject jsonObject) throws JSONException
    {
        return new RequestedItem(jsonObject.getLong(Keyword.TRANSFER_ITEM_ID),
                jsonObject.getLong(Keyword.TRANSFER_SKIPPED_BYTES));
    }

    /**
     * Request item from the remote.
     *
     * @param bridge    The bridge that speaks on behalf of you when making requests. A connection wrapper.
     * @param itemId    Corresponds to {@link TransferItem#id}.
     * @param skipBytes To skip if this item has recovered and error, meaning it already has some bytes transferred
     *                  on your side.
     * @return True if the remote approved of the request.
     * @throws IOException            If an IO error occurs.
     * @throws JSONException          If something goes wrong when creating JSON object.
     * @throws CommunicationException When there is a communication error due to misconfiguration.
     */
    public static boolean requestItem(CommunicationBridge bridge, long itemId, long skipBytes) throws IOException,
            JSONException, CommunicationException
    {
        bridge.sendSecure(true, new JSONObject()
                .put(Keyword.TRANSFER_ITEM_ID, itemId)
                .put(Keyword.TRANSFER_SKIPPED_BYTES, skipBytes));
        return bridge.receiveResult();
    }
}
