package org.monora.uprotocol.variant.persistence;

import org.jetbrains.annotations.NotNull;
import org.monora.uprotocol.core.protocol.Client;
import org.monora.uprotocol.variant.DefaultClient;

public class SecondaryPersistenceProvider extends BasePersistenceProvider
{
    private static final byte @NotNull [] FAKE_PICTURE_BYTES = "Secondary's Fake Picture Bytes".getBytes();

    @Override
    byte @NotNull [] fakePictureBytes()
    {
        return FAKE_PICTURE_BYTES;
    }

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
