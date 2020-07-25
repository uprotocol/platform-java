package org.monora.uprotocol.persistence;

import org.monora.uprotocol.persistence.object.Device;
import org.monora.uprotocol.persistence.object.DeviceConnection;

public interface ObjectFactory
{
    /**
     * Request a new {@link DeviceConnection} instance with no values set.
     *
     * @return the generated object.
     */
    DeviceConnection createDeviceConnection();

    /**
     * Request a new {@link DeviceConnection} instance with IP address set.
     *
     * @param ipAddress the IP address to represent.
     * @return the generated object.
     */
    DeviceConnection createDeviceConnection(String ipAddress);

    /**
     * Request a new {@link Device} instance with no values set.
     *
     * @return the generated object.
     */
    Device createDevice();

    /**
     * Request a new {@link Device} instance with unique identifier set.
     *
     * @param uid that represents the device.
     * @return the generated object.
     */
    Device createDevice(String uid);
}
