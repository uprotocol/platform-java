package org.monora.uprotocol.variant.persistence;

import org.monora.uprotocol.core.protocol.Client;
import org.monora.uprotocol.variant.DefaultClient;

public class PrimaryPersistenceProvider extends BasePersistenceProvider
{
    public Client getClient()
    {
        return new DefaultClient(getClientUid(), getClientNickname(), "Abc", "Def", getCertificate());
    }

    @Override
    public String getClientNickname()
    {
        return "Primo";
    }

    @Override
    public String getClientUid()
    {
        return "primary-client";
    }
}
