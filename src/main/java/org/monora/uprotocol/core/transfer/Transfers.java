package org.monora.uprotocol.core.transfer;

import org.json.JSONException;
import org.json.JSONObject;
import org.monora.uprotocol.core.CommunicationBridge;
import org.monora.uprotocol.core.protocol.communication.CommunicationException;
import org.monora.uprotocol.core.spec.alpha.Keyword;

import java.io.IOException;

public class Transfers
{
    public RequestedItem getRequestedItem(JSONObject jsonObject) throws JSONException
    {
        return new RequestedItem(jsonObject.getLong(Keyword.TRANSFER_ITEM_ID),
                jsonObject.getLong(Keyword.TRANSFER_SKIPPED_BYTES));
    }

    public static boolean requestItem(CommunicationBridge bridge, long itemId, long skipBytes) throws IOException,
            JSONException, CommunicationException
    {
        bridge.sendSecure(true, new JSONObject()
                .put(Keyword.TRANSFER_ITEM_ID, itemId)
                .put(Keyword.TRANSFER_SKIPPED_BYTES, skipBytes));
        return bridge.receiveResult();
    }
}
