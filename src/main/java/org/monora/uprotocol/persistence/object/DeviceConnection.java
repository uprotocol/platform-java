package org.monora.uprotocol.persistence.object;

import java.net.InetAddress;

public interface DeviceConnection
{
    /**
     * This will return the unique identifier for the device which uses this network interface.
     *
     * @return the unique device identifier.
     */
    String getDeviceUid();

    /**
     * The adapter name that represents the network adapter that owns the IP address range.
     *
     * @return the adapter name.
     */
    String getAdapterName();

    /**
     * Get the IP address that is either IPv6 or IPv4.
     *
     * @return the IP address long integer format.
     */
    long getIpAddress();

    /**
     * Set the adapter name that represents the network adapter that owns the IP address range.
     *
     * @param adapterName similar to "wlan0" and "eth0" to better address this network interface.
     */
    void setAdapterName(String adapterName);

    /**
     * Set the device uid for this connection.
     *
     * @param uid of the device that will own this connection.
     */
    void setDeviceUid(String uid);

    /**
     * Set the last time that this connection is used.
     *
     * @param timemillis Unix time.
     */
    void setLastUsageTime(long timemillis);

    /**
     * Encapsulate the internal IP address in {@link InetAddress}.
     *
     * @return the encapsulated instance of the internal IP address.
     */
    InetAddress toInetAddress();
}
