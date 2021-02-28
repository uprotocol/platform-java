package org.monora.uprotocol.variant;

import org.jetbrains.annotations.NotNull;
import org.monora.coolsocket.core.session.ActiveConnection;
import org.monora.uprotocol.core.CommunicationBridge;
import org.monora.uprotocol.core.protocol.ConnectionFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

public class DefaultConnectionFactory implements ConnectionFactory
{
    @Override
    public @NotNull ActiveConnection openConnection(@NotNull InetAddress address) throws IOException
    {
        return CommunicationBridge.openConnection(address);
    }

    @Override
    public void enableCipherSuites(String @NotNull [] supportedCipherSuites,
                                   @NotNull List<@NotNull String> enabledCipherSuiteList)
    {
        enabledCipherSuiteList.add("TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384");
        enabledCipherSuiteList.add("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256");
    }
}
