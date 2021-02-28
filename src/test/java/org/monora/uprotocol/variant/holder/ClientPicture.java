package org.monora.uprotocol.variant.holder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClientPicture
{
    public final @NotNull String clientUid;

    public final byte @NotNull [] data;

    public ClientPicture(@NotNull String clientUid, byte @NotNull [] data)
    {
        this.clientUid = clientUid;
        this.data = data;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof ClientPicture) {
            return clientUid.equals(((ClientPicture) obj).clientUid);
        }
        return super.equals(obj);
    }
}
