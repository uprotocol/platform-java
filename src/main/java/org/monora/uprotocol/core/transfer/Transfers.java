package org.monora.uprotocol.core.transfer;

import org.json.JSONException;
import org.json.JSONObject;
import org.monora.uprotocol.core.CommunicationBridge;
import org.monora.uprotocol.core.protocol.communication.ProtocolException;
import org.monora.uprotocol.core.spec.v1.Keyword;

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
    public static ItemPointer getItemRequest(JSONObject jsonObject) throws JSONException
    {
        return new ItemPointer(jsonObject.getLong(Keyword.TRANSFER_ID),
                jsonObject.getLong(Keyword.TRANSFER_CURRENT_POSITION));
    }

    /**
     * Request item from the remote.
     *
     * @param bridge          The bridge that speaks on behalf of you when making requests. A connection wrapper.
     * @param itemId          Corresponds to {@link Transfer#getTransferId()}.
     * @param currentPosition To skip if this item has recovered from an error, meaning it already has some bytes transferred
     *                        on your side.
     * @return True if the remote approved of the request.
     * @throws IOException       If an IO error occurs.
     * @throws JSONException     If something goes wrong when creating JSON object.
     * @throws ProtocolException When there is a communication error due to misconfiguration.
     */
    public static boolean requestItem(CommunicationBridge bridge, long itemId, long currentPosition) throws IOException,
            JSONException, ProtocolException
    {
        bridge.sendSecure(true, new JSONObject()
                .put(Keyword.TRANSFER_ID, itemId)
                .put(Keyword.TRANSFER_CURRENT_POSITION, currentPosition));
        return bridge.receiveResult();
    }
}
