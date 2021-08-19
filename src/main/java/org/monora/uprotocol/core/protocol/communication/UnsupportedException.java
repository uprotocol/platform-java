package org.monora.uprotocol.core.protocol.communication;

import org.jetbrains.annotations.NotNull;
import org.monora.uprotocol.core.protocol.Client;

/**
 * Thrown when an unsupported operation is requested.
 */
public class UnsupportedException extends CommunicationException
{
    /**
     * Creates a new instance.
     *
     * @param client Who requested or couldn't perform the operation.
     */
    public UnsupportedException(@NotNull Client client)
    {
        super(client);
    }
}
