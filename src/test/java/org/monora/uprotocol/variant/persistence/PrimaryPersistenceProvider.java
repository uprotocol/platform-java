package org.monora.uprotocol.variant.persistence;

import org.monora.uprotocol.core.network.Device;
import org.monora.uprotocol.core.protocol.ClientType;
import org.monora.uprotocol.variant.DefaultDevice;

public class PrimaryPersistenceProvider extends BasePersistenceProvider
{
    @Override
    public String getDeviceUid()
    {
        return "primary-device";
    }

    @Override
    public Device getDevice()
    {
        return new DefaultDevice(getDeviceUid(), "Primo", 1, 1, "Abc", "Def",
                getCertificate());
    }
}
