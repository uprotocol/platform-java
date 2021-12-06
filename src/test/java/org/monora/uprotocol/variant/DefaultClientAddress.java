package org.monora.uprotocol.variant;

import org.jetbrains.annotations.NotNull;
import org.monora.uprotocol.core.protocol.ClientAddress;

import java.net.InetAddress;

public class DefaultClientAddress implements ClientAddress
{
    private @NotNull InetAddress inetAddress;

    private int port;

    private @NotNull String clientUid;

    private long lastUsageTime;

    public DefaultClientAddress(@NotNull InetAddress inetAddress, int port, @NotNull String clientUid,
                                long lastUsageTime)
    {
        this.inetAddress = inetAddress;
        this.port = port;
        this.clientUid = clientUid;
        this.lastUsageTime = lastUsageTime;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof ClientAddress) {
            return inetAddress.equals(((ClientAddress) obj).getClientAddress());
        }
        return super.equals(obj);
    }

    @Override
    public @NotNull InetAddress getClientAddress()
    {
        return inetAddress;
    }

    @Override
    public long getClientAddressLastUsageTime()
    {
        return lastUsageTime;
    }

    @Override
    public @NotNull String getClientAddressOwnerUid()
    {
        return clientUid;
    }

    @Override
    public int getClientAddressPort() {
        return port;
    }

    @Override
    public void setClientAddress(@NotNull InetAddress inetAddress)
    {
        this.inetAddress = inetAddress;
    }

    @Override
    public void setClientAddressLastUsageTime(long lastUsageTime)
    {
        this.lastUsageTime = lastUsageTime;
    }

    @Override
    public void setClientAddressOwnerUid(@NotNull String clientUid)
    {
        this.clientUid = clientUid;
    }

    @Override
    public void setClientAddressPort(int port) {
        this.port = port;
    }
}
