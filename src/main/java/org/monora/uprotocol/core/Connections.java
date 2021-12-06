package org.monora.uprotocol.core;

import org.monora.uprotocol.core.protocol.communication.client.DifferentRemoteClientException;

import java.io.IOException;

/**
 * This class contains the utility methods related to connections.
 */
public class Connections
{
    /**
     * Checks whether the given integer is an internet port.
     *
     * @param port To check.
     * @return True if the given integer is a possible internet port.
     */
    public static boolean isPort(int port)
    {
        return port >= 0 && port <= 0xFFFF;
    }

    /**
     * Check whether an error that occurred when connecting to a remote was related to the connection but not
     * to the remote itself.
     * <p>
     * This means, if you have more than one route to the same remote, as long as this method returns true, you can
     * try different ones.
     *
     * @param exception To assess whether or not this exception is related to the protocol.
     * @return True if this is not a protocol related error and other route can be tried.
     */
    public static boolean shouldTryAnotherConnection(Exception exception)
    {
        return exception instanceof IOException || exception instanceof DifferentRemoteClientException;
    }
}
