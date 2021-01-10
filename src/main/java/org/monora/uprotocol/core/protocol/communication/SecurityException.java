package org.monora.uprotocol.core.protocol.communication;

import org.monora.uprotocol.core.protocol.Client;

/**
 * Thrown when an SSL-related error occurs.
 * <p>
 * The cause reflects what exactly went wrong.
 */
public class SecurityException extends CommunicationException
{
    public SecurityException(Client client, Throwable cause)
    {
        super(client, cause);

        if (cause == null)
            throw new NullPointerException("The cause cannot be null.");
    }
}
