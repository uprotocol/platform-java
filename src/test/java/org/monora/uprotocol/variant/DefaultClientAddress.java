package org.monora.uprotocol.variant;

import org.monora.uprotocol.core.network.ClientAddress;

import java.net.InetAddress;

public class DefaultClientAddress implements ClientAddress
{
    private InetAddress inetAddress;

    private String clientUid;

    private long lastUsageTime;

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

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof ClientAddress) {
            return inetAddress != null && inetAddress.equals(((ClientAddress) obj).getClientAddress());
        }
        return super.equals(obj);
    }

    @Override
    public InetAddress getClientAddress()
    {
        return inetAddress;
    }

    @Override
    public long getClientAddressLastUsageTime()
    {
        return lastUsageTime;
    }

    @Override
    public String getClientAddressOwnerUid()
    {
        return clientUid;
    }

    @Override
    public void setClientAddress(InetAddress inetAddress)
    {
        this.inetAddress = inetAddress;
    }

    @Override
    public void setClientAddressLastUsageTime(long lastUsageTime)
    {
        this.lastUsageTime = lastUsageTime;
    }

    @Override
    public void setClientAddressOwnerUid(String clientUid)
    {
        this.clientUid = clientUid;
    }
}
