package org.monora.uprotocol;

import org.json.JSONObject;
import org.monora.uprotocol.persistence.object.Device;
import org.monora.uprotocol.persistence.object.DeviceInfo;

public interface DeviceLoader<T extends Device>
{
    /**
     * This is a factory class for producing {@link Device}. The {@link Device} produced should
     * benefit from the given {@link DeviceInfo} object as it will include the known fields like
     * {{@link DeviceInfo#nickname}} and other fields defined in it.
     *
     * @param object     to read the data from.
     * @param deviceInfo that will include the known fields.
     * @return the resulting {@link Device}.
     */
    T loadDeviceFrom(JSONObject object, DeviceInfo deviceInfo);
}
