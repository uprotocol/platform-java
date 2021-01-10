package org.monora.uprotocol.variant;

import org.monora.uprotocol.core.protocol.Client;
import org.monora.uprotocol.core.protocol.Clients;
import org.monora.uprotocol.core.protocol.ClientType;

import java.security.cert.X509Certificate;

import static org.monora.uprotocol.core.spec.v1.Config.VERSION_UPROTOCOL;
import static org.monora.uprotocol.core.spec.v1.Config.VERSION_UPROTOCOL_MIN;

public class DefaultClient implements Client
{
    private X509Certificate certificate;

    private String manufacturer;

    private String nickname;

    private String product;

    private ClientType type;

    private String uid;

    private String versionName;

    private int protocolVersion;

    private int protocolVersionMin;

    private int versionCode;

    private long lastUsageTime;

    private boolean blocked;

    private boolean local;

    private boolean trusted;

    public DefaultClient()
    {
    }

    public DefaultClient(String uid)
    {
        this();
        this.uid = uid;
    }

    public DefaultClient(String uid, X509Certificate certificate, String nickname, String manufacturer, String product,
                         ClientType type, String versionName, int versionCode, int protocolVersion,
                         int protocolVersionMin)
    {
        this();
        Clients.fill(this, uid, certificate, nickname, manufacturer, product, type, versionName, versionCode,
                protocolVersion, protocolVersionMin, false, false);
    }

    public DefaultClient(String uid, String nickname, String manufacturer, String product, X509Certificate certificate)
    {
        this(uid, certificate, nickname, manufacturer, product, ClientType.Desktop, "1.0", 1,
                VERSION_UPROTOCOL, VERSION_UPROTOCOL_MIN);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof Client) {
            return uid != null && uid.equals(((Client) obj).getClientUid());
        }
        return super.equals(obj);
    }

    @Override
    public X509Certificate getClientCertificate()
    {
        return certificate;
    }

    @Override
    public long getClientLastUsageTime()
    {
        return lastUsageTime;
    }

    @Override
    public String getClientManufacturer()
    {
        return manufacturer;
    }

    @Override
    public String getClientNickname()
    {
        return nickname;
    }

    @Override
    public String getClientProduct()
    {
        return product;
    }

    @Override
    public int getClientProtocolVersion()
    {
        return protocolVersion;
    }

    @Override
    public int getClientProtocolVersionMin()
    {
        return protocolVersionMin;
    }

    @Override
    public ClientType getClientType()
    {
        return type;
    }

    @Override
    public String getClientUid()
    {
        return uid;
    }

    @Override
    public int getClientVersionCode()
    {
        return versionCode;
    }

    @Override
    public String getClientVersionName()
    {
        return versionName;
    }

    @Override
    public boolean isClientBlocked()
    {
        return blocked;
    }

    @Override
    public boolean isClientLocal()
    {
        return local;
    }

    @Override
    public boolean isClientTrusted()
    {
        return trusted;
    }

    @Override
    public void setClientBlocked(boolean blocked)
    {
        this.blocked = blocked;
    }

    @Override
    public void setClientCertificate(X509Certificate certificate)
    {
        this.certificate = certificate;
    }

    @Override
    public void setClientLastUsageTime(long lastUsageTime)
    {
        this.lastUsageTime = lastUsageTime;
    }

    @Override
    public void setClientLocal(boolean local)
    {
        this.local = local;
    }

    @Override
    public void setClientManufacturer(String manufacturer)
    {
        this.manufacturer = manufacturer;
    }

    @Override
    public void setClientNickname(String nickname)
    {
        this.nickname = nickname;
    }

    @Override
    public void setClientProduct(String product)
    {
        this.product = product;
    }

    @Override
    public void setClientProtocolVersion(int protocolVersion)
    {
        this.protocolVersion = protocolVersion;
    }

    @Override
    public void setClientProtocolVersionMin(int protocolVersionMin)
    {
        this.protocolVersionMin = protocolVersionMin;
    }

    @Override
    public void setClientTrusted(boolean trusted)
    {
        this.trusted = trusted;
    }

    @Override
    public void setClientType(ClientType type)
    {
        this.type = type;
    }

    @Override
    public void setClientUid(String uid)
    {
        this.uid = uid;
    }

    @Override
    public void setClientVersionCode(int versionCode)
    {
        this.versionCode = versionCode;
    }

    @Override
    public void setClientVersionName(String versionName)
    {
        this.versionName = versionName;
    }
}
