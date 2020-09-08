package org.monora.uprotocol.core.network;

import java.net.InetAddress;

/**
 * This class is a way to consume objects that are not actually an {@link InetAddress} but have internal object that
 * can be transformed into one.
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
    public String deviceId;

    /**
     * The last time {@link System#currentTimeMillis()} that a connection was started with this address.
     */
    public long lastCheckedDate;
}
