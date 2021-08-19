package org.monora.uprotocol.core.protocol.communication;

import org.jetbrains.annotations.NotNull;
import org.monora.uprotocol.core.protocol.Client;

/**
 * Thrown when a remote requests an unsupported operation.
 * <p>
 * This only occurs with remote requests
 */
public class RequestUnsupportedException extends UnsupportedException
{
    /**
     * The remote wanted.
     */
    public final @NotNull String request;

    /**
     * Creates a new instance.
     *
     * @param client  Who requested or couldn't perform the operation.
     * @param request The remote wanted.
     */
    public RequestUnsupportedException(@NotNull Client client, @NotNull String request)
    {
        super(client);
        this.request = request;
    }
}
