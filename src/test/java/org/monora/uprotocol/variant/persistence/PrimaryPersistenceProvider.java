package org.monora.uprotocol.variant.persistence;

import org.jetbrains.annotations.NotNull;
import org.monora.uprotocol.core.protocol.Client;
import org.monora.uprotocol.variant.DefaultClient;

public class PrimaryPersistenceProvider extends BasePersistenceProvider
{
    public @NotNull Client getClient()
    {
        return new DefaultClient(getClientUid(), getClientNickname(), "Abc", "Def", getCertificate());
    }

    @Override
    public @NotNull String getClientNickname()
    {
        return "Primo";
    }

    @Override
    public @NotNull String getClientUid()
    {
        return "primary-client";
    }
}
