package org.monora.uprotocol.core.protocol.communication;

import org.monora.uprotocol.core.network.Device;

/**
 * Thrown when an error related to SSL occurs.
 * <p>
 * The cause should reflect what exactly went wrong.
 */
public class SecureClientCommunicationException extends ClientCommunicationException
{
    public SecureClientCommunicationException(Device device, Throwable cause)
    {
        super(device, cause);

        if (cause == null)
            throw new NullPointerException("The cause cannot be null.");
    }
}
