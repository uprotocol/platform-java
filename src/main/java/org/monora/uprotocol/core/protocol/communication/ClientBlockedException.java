package org.monora.uprotocol.core.protocol.communication;

import org.monora.uprotocol.core.network.Device;

/**
 * Thrown when the remote is blocked with {@link Device#isBlocked} and wants to connect.
 * <p>
 * If the remote is blocked and this client wants to connect to it, the block is removed.
 */
public class ClientBlockedException extends ClientAuthorizationException
{
    public ClientBlockedException(Device device)
    {
        super(device);
    }
}
