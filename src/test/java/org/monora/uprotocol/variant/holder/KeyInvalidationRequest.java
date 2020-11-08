package org.monora.uprotocol.variant.holder;

public class KeyInvalidationRequest
{
    public final String deviceId;

    public final int receiverKey;

    public final int senderKey;

    public KeyInvalidationRequest(String deviceId, int receiverKey, int senderKey)
    {
        this.deviceId = deviceId;
        this.receiverKey = receiverKey;
        this.senderKey = senderKey;
    }
}
