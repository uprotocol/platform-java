package org.monora.uprotocol.core.protocol;

import org.jetbrains.annotations.NotNull;
import org.monora.uprotocol.core.spec.v1.Config;

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
    @NotNull InetAddress getClientAddress();

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
    @NotNull String getClientAddressOwnerUid();

    /**
     * The port that the uprotocol is running on, usually corresponds to {@link Config#PORT_UPROTOCOL}.
     *
     * @return The port.
     * @see #setClientAddressPort(int)
     */
    int getClientAddressPort();

    /**
     * Change the address of this instance.
     *
     * @param inetAddress The internet address.
     * @see #getClientAddress()
     */
    void setClientAddress(@NotNull InetAddress inetAddress);

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
    void setClientAddressOwnerUid(@NotNull String clientUid);

    /**
     * Change the port of this instance.
     *
     * @param port The port.
     * @see #getClientAddressPort()
     */
    void setClientAddressPort(int port);
}
