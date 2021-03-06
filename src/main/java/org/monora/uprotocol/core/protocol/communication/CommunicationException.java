package org.monora.uprotocol.core.protocol.communication;

import org.jetbrains.annotations.NotNull;
import org.monora.uprotocol.core.protocol.Client;

/**
 * This is a high-level error that is thrown when communication with a remote client fails in some way.
 * <p>
 * The main difference from {@link ProtocolException} is this exception also shows with which client it occurred.
 */
public class CommunicationException extends ProtocolException
{
    public final @NotNull Client client;

    public CommunicationException(@NotNull Client client)
    {
        super();
        this.client = client;
    }

    public CommunicationException(@NotNull Client client, @NotNull Throwable cause)
    {
        super(cause);
        this.client = client;
    }
}
