package org.monora.uprotocol.variant;

import org.monora.uprotocol.core.network.Device;
import org.monora.uprotocol.core.protocol.ClientType;

import java.security.cert.X509Certificate;

import static org.monora.uprotocol.core.spec.alpha.Config.VERSION_UPROTOCOL;
import static org.monora.uprotocol.core.spec.alpha.Config.VERSION_UPROTOCOL_MIN;

public class DefaultDevice extends Device
{
    public DefaultDevice()
    {
    }

    public DefaultDevice(String uid)
    {
        this();
        this.uid = uid;
    }

    public DefaultDevice(String uid, String username, String brand, String model, ClientType type, String versionName,
                         int versionCode, int protocolVersion, int protocolVersionMin, X509Certificate certificate)
    {
        this(uid);
        from(username, brand, model, type, versionName, versionCode, protocolVersion,
                protocolVersionMin, false, false, certificate);
    }

    public DefaultDevice(String uid, String username, String brand, String model, X509Certificate certificate)
    {
        this(uid, username, brand, model, ClientType.Desktop, "1.0", 1,
                VERSION_UPROTOCOL, VERSION_UPROTOCOL_MIN, certificate);
    }
}
