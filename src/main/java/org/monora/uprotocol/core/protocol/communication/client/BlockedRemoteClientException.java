package org.monora.uprotocol.core.protocol.communication.client;

import org.monora.uprotocol.core.protocol.Client;
import org.monora.uprotocol.core.protocol.communication.CommunicationException;

/**
 * This error concerns a remote client and is thrown when it reaches this client while it is blocked.
 * <p>
 * If this client connects to a blocked remote client, the blocked status will be removed.
 */
public class BlockedRemoteClientException extends CommunicationException
{
    public BlockedRemoteClientException(Client client)
    {
        super(client);
    }
}
