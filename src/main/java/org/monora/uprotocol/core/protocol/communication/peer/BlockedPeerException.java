package org.monora.uprotocol.core.protocol.communication.peer;

import org.monora.uprotocol.core.network.Client;
import org.monora.uprotocol.core.protocol.communication.CommunicationException;

/**
 * This error concerns the peer and is thrown when it reaches this client while it is blocked.
 * <p>
 * If this clients connects to a blocked peer, the blocked status will be removed.
 */
public class BlockedPeerException extends CommunicationException
{
    public BlockedPeerException(Client client)
    {
        super(client);
    }
}
