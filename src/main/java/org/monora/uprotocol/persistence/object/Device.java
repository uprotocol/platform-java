package org.monora.uprotocol.persistence.object;

public interface Device
{
    /**
     * The device manufacturer/brand extra value.
     */
    int EXTRA_DEVICE_BRAND = 0x1;

    /**
     * The device model/model code extra value.
     */
    int EXTRA_DEVICE_MODEL = 0x2;

    /**
     * Check whether this devices is marked as blocked.
     *
     * @return true when the device is blocked.
     */
    boolean isBlocked();

    /**
     * Check whether this devices is marked as trusted.
     *
     * @return true when the device is trusted.
     */
    boolean isTrusted();

    /**
     * This will return the unique ID for this device.
     *
     * @return the unique ID for the device.
     */
    String getUid();

    /**
     * Get an extra integer value that is not crucial to the protocol's workflow.
     *
     * @param key that represents the extra value.
     * @return the extra integer value.
     */
    int getIntExtra(int key);

    /**
     * Get an extra long value that is not crucial to the protocol's workflow.
     *
     * @param key that represents the extra value.
     * @return the extra long value.
     */
    int getLongExtra(int key);

    /**
     * Get an extra string value that is not crucial to the protocol's workflow.
     *
     * @param key that represents the extra value.
     * @return the extra string value.
     */
    String getStringExtra(int key);

    /**
     * This will return the nickname for this device usually set by the user.
     *
     * @return the device name.
     */
    String getNickname();

    /**
     * The string representation of the client OS (e.g., Android).
     *
     * @return the string representation of the client OS.
     */
    String getClientOs();

    /**
     * The version code of the client that this device uses.
     *
     * @return the version code.
     */
    int getClientVersion();

    /**
     * The version name of th client that this devices uses.
     *
     * @return the version name.
     */
    int getClientVersionName();

    /**
     * The latest protocol version that this device supports. This shouldn't be relied on as the device may get an
     * update after a disconnection.
     *
     * @return the latest protocol version that the device reported us as supported.
     */
    int getProtocolVersion();

    /**
     * This returns the key that is used to represents the secure exchange key.
     *
     * @return the key previously exchanged with this device in some way (e.g., manual-entry, QR code).
     */
    int getSecureKey();

    /**
     * Load this device's info from a {@link DeviceInfo} which should be flexible enough to provide this to its
     * descendants.
     *
     * @param deviceInfo where the information will be gathered.
     */
    void loadFrom(DeviceInfo deviceInfo);

    /**
     * Return the data for this data as a {@link DeviceInfo} instance.
     *
     * @return the device info instance.
     */
    DeviceInfo toDeviceInfo();
}
