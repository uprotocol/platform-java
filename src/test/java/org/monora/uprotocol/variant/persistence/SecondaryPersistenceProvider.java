package org.monora.uprotocol.variant.persistence;

import org.monora.uprotocol.core.network.Client;
import org.monora.uprotocol.variant.DefaultClient;

public class SecondaryPersistenceProvider extends BasePersistenceProvider
{
    @Override
    public String getClientUid()
    {
        return "secondary-client";
    }

    @Override
    public Client getClient()
    {
        return new DefaultClient(getClientUid(), "Sec", "Xyz", "Tuv", getCertificate());
    }
}
