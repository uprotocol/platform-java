package org.monora.uprotocol.variant.holder;

public class Avatar
{
    public final String deviceUid;

    public final byte[] data;

    public Avatar(String deviceUid, byte[] data)
    {
        this.deviceUid = deviceUid;
        this.data = data;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof Avatar) {
            return deviceUid.equals(((Avatar) obj).deviceUid);
        }
        return super.equals(obj);
    }
}
