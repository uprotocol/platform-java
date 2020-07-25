package org.monora.uprotocol.persistence;

import org.json.JSONObject;
import org.monora.uprotocol.persistence.object.Device;
import org.monora.uprotocol.persistence.object.DeviceConnection;
import org.monora.uprotocol.persistence.object.DeviceInfo;

import java.io.IOException;

/**
 * This class is an abstraction mechanism from a default persistence provider that you might already be using. For
 * instance, you can implement this to provide the data this library needs when you are using Room Persistence Library.
 */
public interface PersistenceProvider
{
    /**
     * Check if this device is known to the persistence provider. The device should at the very least have the uid
     * in order for this to function.
     *
     * @param device to check.
     * @return true if the device is known.
     */
    boolean exists(Device device);

    /**
     * Check if this connection is known to the persistence provider. The connection should at the very least have the
     * ip address in order for this to function.
     *
     * @param connection to check.
     * @return true if the device is known.
     */
    boolean exists(DeviceConnection connection);

    /**
     * Sync the given device to its latest revision in the database.
     *
     * @param device to sync.
     */
    void sync(Device device) throws NotFoundException;

    /**
     * Sync the given connection to its latest revision in the database.
     *
     * @param connection to sync.
     */
    void sync(DeviceConnection connection) throws NotFoundException;

    /**
     * Save this revision of the given device.
     *
     * @param device to be saved.
     */
    void process(Device device);

    /**
     * Save this revision of the given connection.
     *
     * @param deviceConnection to be saved.
     */
    void process(DeviceConnection deviceConnection);

    void remove();

    /**
     * Find the previously saved {@link Device} from the database.
     *
     * @param uid the serial/uuid for the device.
     * @return the instance representing the device or null when there is no match.
     */
    Device findDevice(String uid);

    /**
     * Save the profile avatar coming from the device to a persistent storage to show it to the user later.
     *
     * @param device the device which owns the avatar.
     * @param bitmap the data that contains the avatar for the given device.
     * @throws IOException when an IO error occurs.
     */
    void saveDeviceAvatar(Device device, byte[] bitmap) throws IOException;

    /**
     * This will return the factory class for {@link Device}, {@link DeviceConnection}, and other similar classes.
     *
     * @return the factory class.
     */
    ObjectFactory getObjectFactory();

    DeviceInfo loadDeviceInfoFrom(JSONObject object);
}
