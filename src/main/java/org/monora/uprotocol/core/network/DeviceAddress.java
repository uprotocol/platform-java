package org.monora.uprotocol.core.network;

import java.net.InetAddress;

/**
 * This class ties an internet address with a device.
 *
 * @see InetAddress
 * @see Device
 */
public abstract class DeviceAddress
{
    /**
     * The internet address that this instance is targeting.
     */
    public InetAddress inetAddress;

    /**
     * The {@link Device#uid} that specifies who owns this address.
     */
    public String deviceUid;

    /**
     * The last time that a communication was started with this address.
     *
     * @see System#currentTimeMillis()
     */
    public long lastUsageTime;

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof DeviceAddress) {
            return inetAddress != null && inetAddress.equals(((DeviceAddress) obj).inetAddress);
        }
        return super.equals(obj);
    }
}
