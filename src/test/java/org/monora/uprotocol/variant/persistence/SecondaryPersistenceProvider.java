package org.monora.uprotocol.variant.persistence;

import org.monora.uprotocol.core.network.Device;
import org.monora.uprotocol.variant.DefaultDevice;

public class SecondaryPersistenceProvider extends BasePersistenceProvider
{
    @Override
    public String getDeviceUid()
    {
        return "secondary-device";
    }

    @Override
    public Device getDevice()
    {
        return new DefaultDevice(getDeviceUid(), "Sec", 100, 100, "Xyz", "Tuv");
    }
}
