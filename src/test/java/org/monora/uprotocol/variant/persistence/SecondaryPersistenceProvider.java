package org.monora.uprotocol.variant.persistence;

import org.jetbrains.annotations.NotNull;
import org.monora.uprotocol.core.protocol.Client;
import org.monora.uprotocol.variant.DefaultClient;

public class SecondaryPersistenceProvider extends BasePersistenceProvider
{
    private static final byte @NotNull [] FAKE_PICTURE_BYTES = "Secondary's Fake Picture Bytes".getBytes();

    private static final long revisionOfPicture = 20;

    @Override
    public @NotNull Client getClient()
    {
        Client client = new DefaultClient(getClientUid(), getClientNickname(), "Xyz", "Tuv",
                getCertificate(), revisionOfPicture);
        persistClientPicture(client, FAKE_PICTURE_BYTES);
        return client;
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
