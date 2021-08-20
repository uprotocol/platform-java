package org.monora.uprotocol.core.protocol.communication;

import org.json.JSONObject;
import org.monora.uprotocol.core.CommunicationBridge;
import org.monora.uprotocol.core.protocol.Direction;

/**
 * Returned as the result of an {@link CommunicationBridge#requestGuidance(Direction)}
 */
public class GuidanceResult
{
    /**
     * Denotes whether the request was successful.
     * <p>
     * If true,
     */
    public final boolean result;

    /**
     * The data that was sent by the remote along with the {@link #result}.
     */
    public final JSONObject response;

    /**
     * @param result   That denotes whether another request will precede the request.
     * @param response That will contain the request if {@link #result} is 'true'.
     */
    public GuidanceResult(boolean result, JSONObject response)
    {
        this.result = result;
        this.response = response;
    }
}
