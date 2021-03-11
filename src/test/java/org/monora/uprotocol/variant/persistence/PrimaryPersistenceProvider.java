package org.monora.uprotocol.variant.persistence;

import org.jetbrains.annotations.NotNull;
import org.monora.uprotocol.core.protocol.Client;
import org.monora.uprotocol.variant.DefaultClient;

public class PrimaryPersistenceProvider extends BasePersistenceProvider
{
    private static final byte @NotNull [] FAKE_PICTURE_BYTES = "Primary's Fake Picture Bytes".getBytes();

    public @NotNull Client getClient()
    {
        return new DefaultClient(getClientUid(), getClientNickname(), "Abc", "Def",
                getCertificate(), FAKE_PICTURE_BYTES);
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
