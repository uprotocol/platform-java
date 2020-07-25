package org.monora.uprotocol.persistence.object;

import java.util.HashMap;
import java.util.Map;

public class DeviceInfo
{
    /**
     * Represents the unique identifier for the device.
     */
    public String uid;

    /**
     * User-defined nickname used to represent the device.
     */
    public String nickname;

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
     * The key used to perform secure connections.
     */
    public int secureKey;

    /**
     * The version code for the client running on the device.
     */
    public int versionCode;

    /**
     * The latest uprotocol version supported by the client.
     */
    public int protocolVersion;

    /**
     * The last timestamp that the device was used.
     */
    public long lastUsageTime;

    /**
     * The extras provided by you
     */
    protected Map<String, Object> extras = new HashMap<>();

    /**
     * Retrieve the previously put extra value using its key.
     *
     * @param key for the previously put value.
     * @return the value for the key or null if there is no matching value with the given key.
     */
    public Object getExtra(String key)
    {
        return extras.get(key);
    }
}
