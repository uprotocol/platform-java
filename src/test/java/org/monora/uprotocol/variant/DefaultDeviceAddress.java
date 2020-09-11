package org.monora.uprotocol.variant;

import org.monora.uprotocol.core.network.DeviceAddress;

import java.net.InetAddress;

public class DefaultDeviceAddress extends DeviceAddress
{
    public DefaultDeviceAddress(InetAddress inetAddress)
    {
        this.inetAddress = inetAddress;
    }

    public DefaultDeviceAddress(InetAddress inetAddress, String deviceUid, long lastUsageTime)
    {
        this(inetAddress);
        this.deviceUid = deviceUid;
        this.lastUsageTime = lastUsageTime;
    }
}
