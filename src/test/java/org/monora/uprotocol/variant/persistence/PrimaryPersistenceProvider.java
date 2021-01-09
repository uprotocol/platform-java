package org.monora.uprotocol.variant.persistence;

import org.monora.uprotocol.core.network.Client;
import org.monora.uprotocol.variant.DefaultClient;

public class PrimaryPersistenceProvider extends BasePersistenceProvider
{
    @Override
    public String getDeviceUid()
    {
        return "primary-device";
    }

    @Override
    public Client getDevice()
    {
        return new DefaultClient(getDeviceUid(), "Primo", "Abc", "Def", getCertificate());
    }
}
