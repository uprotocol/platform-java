package org.monora.uprotocol.core.network;

import org.monora.uprotocol.core.protocol.ClientType;

import java.security.cert.X509Certificate;

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

    public X509Certificate certificate;

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

    /**
     * Load the details for this instance from another instance.
     *
     * @param device To load from.
     */
    public void from(Device device)
    {
        from(device.username, device.brand, device.model, device.clientType, device.versionName, device.versionCode,
                device.protocolVersion, device.protocolVersionMin, device.isTrusted, device.isBlocked, device.certificate);
    }

    protected void from(String username, String brand, String model, ClientType clientType, String versionName,
                        int versionCode, int protocolVersion, int protocolVersionMin, boolean isTrusted,
                        boolean isBlocked, X509Certificate certificate)
    {
        this.username = username;
        this.brand = brand;
        this.model = model;
        this.clientType = clientType;
        this.versionName = versionName;
        this.versionCode = versionCode;
        this.protocolVersion = protocolVersion;
        this.protocolVersionMin = protocolVersionMin;
        this.isTrusted = isTrusted;
        this.isBlocked = isBlocked;
        this.certificate = certificate;
    }
}
