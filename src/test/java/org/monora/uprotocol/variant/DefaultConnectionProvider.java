package org.monora.uprotocol.variant;

import org.monora.coolsocket.core.session.ActiveConnection;
import org.monora.uprotocol.core.CommunicationBridge;
import org.monora.uprotocol.core.protocol.ConnectionProvider;

import java.io.IOException;
import java.net.InetAddress;

public class DefaultConnectionProvider implements ConnectionProvider
{
    @Override
    public ActiveConnection openConnection(InetAddress address) throws IOException
    {
        return CommunicationBridge.openConnection(address);
    }
}
