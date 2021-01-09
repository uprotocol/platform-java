package org.monora.uprotocol.variant;

import org.monora.uprotocol.core.network.ClientAddress;

import java.net.InetAddress;

public class DefaultClientAddress extends ClientAddress
{
    public DefaultClientAddress(InetAddress inetAddress)
    {
        this.inetAddress = inetAddress;
    }

    public DefaultClientAddress(InetAddress inetAddress, String clientUid, long lastUsageTime)
    {
        this(inetAddress);
        this.clientUid = clientUid;
        this.lastUsageTime = lastUsageTime;
    }
}
