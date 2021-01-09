package org.monora.uprotocol.core.protocol.communication;

import org.monora.uprotocol.core.network.Client;

/**
 * This is a high-level error that is thrown when communication with a remote client fails in some way.
 * <p>
 * The main difference from {@link ProtocolException} is this exception also shows with which client it occurred.
 */
public class CommunicationException extends ProtocolException
{
    public final Client client;

    public CommunicationException(Client client)
    {
        super();
        this.client = client;
    }

    public CommunicationException(Client client, Throwable cause)
    {
        super(cause);
        this.client = client;
    }
}
