package org.monora.uprotocol.core.protocol.communication;

import org.jetbrains.annotations.NotNull;
import org.monora.uprotocol.core.protocol.Client;

/**
 * Thrown when an SSL-related error occurs.
 */
public class SecurityException extends CommunicationException
{
    public SecurityException(@NotNull Client client, @NotNull Throwable cause)
    {
        super(client, cause);
    }
}
