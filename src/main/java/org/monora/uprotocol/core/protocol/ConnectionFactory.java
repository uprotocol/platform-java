package org.monora.uprotocol.core.protocol;

import org.jetbrains.annotations.NotNull;
import org.monora.coolsocket.core.session.ActiveConnection;
import org.monora.uprotocol.core.CommunicationBridge;
import org.monora.uprotocol.core.TransportSession;
import org.monora.uprotocol.core.spec.v1.Config;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;

/**
 * This factory class is responsible for opening connections and bypassing the NAT/firewall. It communicates with
 * the appropriate system resources before opening a connection.
 */
public interface ConnectionFactory
{
    /**
     * Open a CoolSocket connection serving a uprotocol service.
     * <p>
     * {@link CommunicationBridge#openConnection} can handle the actual connection phase.
     *
     * @param address To open a connection with.
     * @return The class representing the connection.
     * @throws IOException If an IO related error occurs.
     */
    @NotNull ActiveConnection openConnection(@NotNull InetSocketAddress address) throws IOException;

    /**
     * Invoked before establishing a secure connection with a remote client.
     * <p>
     * The cipher suites enabled by default can be found in the cipher list parameter.
     * <p>
     * At least one cipher suite should be enabled.
     *
     * @param supportedCipherSuites  You can enable.
     * @param enabledCipherSuiteList To which you can add more cipher suites which will be enabled during the SSL
     */
    void enableCipherSuites(String @NotNull [] supportedCipherSuites,
                            @NotNull List<@NotNull String> enabledCipherSuiteList);

    /**
     * The uprotocol server port that this client is/will be running on.
     * <p>
     * This port will be used by {@link TransportSession} as the uprotocol service/server port. It will also be sent
     * to remote clients when this client connects to them, and they will use it to connect back to this client when
     * there is no network discovery service is available.
     * <p>
     * The port is {@link Config#PORT_UPROTOCOL} by default and can be changed by overriding this default method.
     *
     * @return The port that this client's uprotocol server is running on.
     */
    default int getServicePort()
    {
        return Config.PORT_UPROTOCOL;
    }
}
