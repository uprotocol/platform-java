package org.monora.uprotocol.core.network;

import org.monora.uprotocol.core.protocol.ClientType;

/**
 * A device is a representation of a client using the latest information it has provided in a previous communication.
 */
public abstract class Device
{
    /**
     * Represents the unique identifier for the device.
     */
    public String uid;

    /**
     * The user-preferred username representing the device.
     */
    public String username;

    /**
     * The version name for the client running on the device.
     */
    public String versionName;

    /**
     * The brand/manufacturer of the device.
     */
    public String brand;

    /**
     * The model name of the device.
     */
    public String model;

    /**
     * The client type.
     */
    public ClientType clientType;

    /**
     * The key that we will send to the remote when we are the one who is initiating the communication.
     *
     * @see #receiverKey
     */
    public int senderKey;

    /**
     * The key that the remote will send us when it is the one initiating the communication.
     *
     * @see #senderKey
     */
    public int receiverKey;

    /**
     * The version code for the client running on the device.
     */
    public int versionCode;

    /**
     * The latest uprotocol version supported by the client.
     */
    public int protocolVersion;

    /**
     * Minimum supported uprotocol version for this client.
     */
    public int protocolVersionMin;

    /**
     * The last timestamp that the device was used.
     */
    public long lastUsageTime;

    /**
     * This represents whether this device is trusted by this client. Trusted devices has more access
     * rights than those who don't.
     *
     * @see #isBlocked
     */
    public boolean isTrusted;

    /**
     * This represents whether this device is blocked on this client. When devices are blocked they
     * cannot access unless the user for this client unblocks it.
     *
     * @see #isTrusted
     */
    public boolean isBlocked;

    /**
     * Determine whether this device instance belongs to this client (us).
     */
    public boolean isLocal;

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof Device) {
            return uid != null && uid.equals(((Device) obj).uid);
        }
        return super.equals(obj);
    }

    public void from(Device device)
    {
        from(device.username, device.senderKey, device.receiverKey, device.brand, device.model, device.clientType,
                device.versionName, device.versionCode, device.protocolVersion, device.protocolVersionMin);
    }

    protected void from(String username, int senderKey, int receiverKey, String brand, String model,
                        ClientType clientType, String versionName, int versionCode, int protocolVersion,
                        int protocolVersionMin)
    {
        this.username = username;
        this.senderKey = senderKey;
        this.receiverKey = receiverKey;
        this.brand = brand;
        this.model = model;
        this.clientType = clientType;
        this.versionName = versionName;
        this.versionCode = versionCode;
        this.protocolVersion = protocolVersion;
        this.protocolVersionMin = protocolVersionMin;
    }
}
