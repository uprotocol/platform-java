package org.monora.uprotocol.core;

import org.monora.uprotocol.core.network.Device;

public interface TransportSeat
{
    void handleBlockedDevice(Device.DeviceInfo deviceInfo);
}
