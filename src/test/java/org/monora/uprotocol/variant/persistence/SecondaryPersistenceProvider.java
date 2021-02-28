package org.monora.uprotocol.variant.persistence;

import org.jetbrains.annotations.NotNull;
import org.monora.uprotocol.core.protocol.Client;
import org.monora.uprotocol.variant.DefaultClient;

public class SecondaryPersistenceProvider extends BasePersistenceProvider
{
    @Override
    public @NotNull Client getClient()
    {
        return new DefaultClient(getClientUid(), getClientNickname(), "Xyz", "Tuv", getCertificate());
    }

    @Override
    public @NotNull String getClientNickname()
    {
        return "Sec";
    }

    @Override
    public @NotNull String getClientUid()
    {
        return "secondary-client";
    }
}
