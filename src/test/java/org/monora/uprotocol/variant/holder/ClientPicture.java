package org.monora.uprotocol.variant.holder;

public class ClientPicture
{
    public final String clientUid;

    public final byte[] data;

    public ClientPicture(String clientUid, byte[] data)
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
