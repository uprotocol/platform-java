package org.monora.uprotocol.core.protocol;

import org.monora.coolsocket.core.session.ActiveConnection;

import java.io.IOException;
import java.net.InetAddress;

/**
 * This factory class is responsible for opening connections in order to bypass the NAT/firewall. What this does is
 * communicate with the appropriate system resources before opening a connection.
 */
public interface ConnectionProvider
{
    /**
     * Open a CoolSocket connection serving a uprotocol service.
     * <p>
     * {@link org.monora.uprotocol.core.CommunicationBridge#openConnection(InetAddress)} can complete the connection
     * actual connection phase.
     *
     * @param address To open a connection with.
     * @return The class representing the connection.
     * @throws IOException If an IO related error occurs.
     */
    ActiveConnection openConnection(InetAddress address) throws IOException;
}
