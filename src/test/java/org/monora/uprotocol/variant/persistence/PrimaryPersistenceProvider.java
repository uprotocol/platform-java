package org.monora.uprotocol.variant.persistence;

import org.monora.uprotocol.core.protocol.Client;
import org.monora.uprotocol.variant.DefaultClient;

public class PrimaryPersistenceProvider extends BasePersistenceProvider
{
    @Override
    public String getClientUid()
    {
        return "primary-client";
    }

    @Override
    public Client getClient()
    {
        return new DefaultClient(getClientUid(), "Primo", "Abc", "Def", getCertificate());
    }
}
