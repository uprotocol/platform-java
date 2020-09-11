package org.monora.uprotocol.core.network;

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
     * The OS that the client is running on.
     */
    public String clientOs;

    /**
     * The brand/manufacturer of the device.
     */
    public String brand;

    /**
     * The model name of the device.
     */
    public String model;

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
     * This represents whether this devices is blocked on this client. When devices are blocked they
     * cannot access unless the user for this client unblocks it.
     *
     * @see #isTrusted
     */
    public boolean isBlocked;

    /**
     * Determine whether this device instance belongs to this client (us).
     */
    public boolean isLocal;
}
