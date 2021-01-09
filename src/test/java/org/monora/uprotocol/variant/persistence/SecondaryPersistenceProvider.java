package org.monora.uprotocol.variant.persistence;

import org.monora.uprotocol.core.network.Client;
import org.monora.uprotocol.variant.DefaultClient;

public class SecondaryPersistenceProvider extends BasePersistenceProvider
{
    @Override
    public String getDeviceUid()
    {
        return "secondary-device";
    }

    @Override
    public Client getDevice()
    {
        return new DefaultClient(getDeviceUid(), "Sec", "Xyz", "Tuv", getCertificate());
    }
}
