package org.monora.uprotocol.variant.persistence;

import org.monora.uprotocol.core.protocol.Client;
import org.monora.uprotocol.variant.DefaultClient;

public class SecondaryPersistenceProvider extends BasePersistenceProvider
{
    @Override
    public Client getClient()
    {
        return new DefaultClient(getClientUid(), getClientNickname(), "Xyz", "Tuv", getCertificate());
    }

    @Override
    public String getClientNickname()
    {
        return "Sec";
    }

    @Override
    public String getClientUid()
    {
        return "secondary-client";
    }
}
