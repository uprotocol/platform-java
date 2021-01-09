package org.monora.uprotocol.core.protocol;

import org.monora.coolsocket.core.session.ActiveConnection;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

/**
 * This factory class is responsible for opening connections and bypassing the NAT/firewall. What this does is
 * communicate with the appropriate system resources before opening a connection.
 */
public interface ConnectionFactory
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

    /**
     * Invoked before establishing a secure connection with a remote client.
     * <p>
     * The cipher suites enabled by default can be found in the cipher list parameter.
     * <p>
     * At least one cipher suite should be enabled.
     *
     * @param supportedCipherSuites  You can enable.
     * @param enabledCipherSuiteList To which you can add more cipher suites which will be enabled during the SSL
     *                               communication.
     */
    void enableCipherSuites(String[] supportedCipherSuites, List<String> enabledCipherSuiteList);
}
