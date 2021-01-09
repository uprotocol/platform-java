package org.monora.uprotocol.core.network;

import java.net.InetAddress;

/**
 * This class ties an internet address with a client.
 *
 * @see InetAddress
 * @see Client
 */
public abstract class ClientAddress
{
    /**
     * The internet address that this instance is targeting.
     */
    public InetAddress inetAddress;

    /**
     * The {@link Client#uid} that specifies who owns this address.
     */
    public String clientUid;

    /**
     * The last time that a communication was started with this address.
     *
     * @see System#currentTimeMillis()
     */
    public long lastUsageTime;

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof ClientAddress) {
            return inetAddress != null && inetAddress.equals(((ClientAddress) obj).inetAddress);
        }
        return super.equals(obj);
    }
}
