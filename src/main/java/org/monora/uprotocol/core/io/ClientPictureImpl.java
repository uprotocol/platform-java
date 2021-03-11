package org.monora.uprotocol.core.io;

import org.jetbrains.annotations.NotNull;

class ClientPictureImpl implements ClientPicture
{
    private final @NotNull String clientUid;

    private final byte @NotNull [] data;

    private final int checksum;

    public ClientPictureImpl(@NotNull String clientUid, byte @NotNull [] data, int checksum)
    {
        this.clientUid = clientUid;
        this.data = data;
        this.checksum = checksum;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof ClientPicture) {
            return getClientUid().equals(((ClientPicture) obj).getClientUid());
        }
        return super.equals(obj);
    }

    @Override
    public @NotNull String getClientUid()
    {
        return clientUid;
    }

    @Override
    public byte @NotNull [] getPictureData()
    {
        return data;
    }

    @Override
    public int getPictureChecksum()
    {
        return checksum;
    }

    @Override
    public boolean hasPicture()
    {
        return data.length > 0;
    }
}
