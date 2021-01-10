package org.monora.uprotocol.core.network;

import java.net.InetAddress;

/**
 * This class ties an internet address with a client.
 *
 * @see InetAddress
 * @see Client
 */
public interface ClientAddress
{
    /**
     * The internet address that points to the client.
     *
     * @return The internet address.
     * @see #setClientAddress(InetAddress)
     */
    InetAddress getClientAddress();

    /**
     * The last usage time of this address.
     *
     * @return The time in UNIX epoch format.
     * @see #getClientAddressLastUsageTime()
     */
    long getClientAddressLastUsageTime();

    /**
     * The uid of the {@link Client} that owns this address.
     *
     * @return The client uid.
     * @see #setClientAddressOwnerUid(String)
     */
    String getClientAddressOwnerUid();

    /**
     * Change the address of this instance.
     *
     * @param inetAddress The internet address.
     * @see #getClientAddress()
     */
    void setClientAddress(InetAddress inetAddress);

    /**
     * Change the time that this address was used.
     *
     * @param lastUsageTime The time in UNIX epoch format.
     * @see #getClientAddressLastUsageTime()
     */
    void setClientAddressLastUsageTime(long lastUsageTime);

    /**
     * Change the client uid owning this address.
     *
     * @param clientUid The uid of the client.
     * @see #getClientAddressLastUsageTime()
     * @see System#currentTimeMillis()
     */
    void setClientAddressOwnerUid(String clientUid);
}
