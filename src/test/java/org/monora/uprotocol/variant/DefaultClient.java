package org.monora.uprotocol.variant;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.monora.uprotocol.core.protocol.Client;
import org.monora.uprotocol.core.protocol.ClientType;

import java.security.cert.X509Certificate;
import java.util.Arrays;

import static org.monora.uprotocol.core.spec.v1.Config.VERSION_UPROTOCOL;
import static org.monora.uprotocol.core.spec.v1.Config.VERSION_UPROTOCOL_MIN;

public class DefaultClient implements Client
{
    private @Nullable X509Certificate certificate;

    private @NotNull String manufacturer;

    private @NotNull String nickname;

    private @NotNull String product;

    private @NotNull ClientType type;

    private @NotNull String uid;

    private @NotNull String versionName;

    private int protocolVersion;

    private int protocolVersionMin;

    private int versionCode;

    private long lastUsageTime;

    private boolean blocked;

    private boolean local;

    private boolean trusted;

    public byte[] pictureData = null;

    public int pictureChecksum = 0;

    public DefaultClient(@NotNull String uid, @NotNull String nickname, @NotNull String manufacturer,
                         @NotNull String product, @NotNull ClientType type, @NotNull String versionName,
                         int versionCode, int protocolVersion, int protocolVersionMin)
    {
        this.uid = uid;
        this.nickname = nickname;
        this.manufacturer = manufacturer;
        this.product = product;
        this.type = type;
        this.versionName = versionName;
        this.versionCode = versionCode;
        this.protocolVersion = protocolVersion;
        this.protocolVersionMin = protocolVersionMin;
    }

    public DefaultClient(@NotNull String uid, @NotNull String nickname, @NotNull String manufacturer,
                         @NotNull String product, @Nullable X509Certificate certificate, byte[] pictureData)
    {
        this(uid, nickname, manufacturer, product, ClientType.Desktop, "1.0", 1,
                VERSION_UPROTOCOL, VERSION_UPROTOCOL_MIN);
        this.certificate = certificate;
        this.pictureData = pictureData;
        this.pictureChecksum = Arrays.hashCode(pictureData);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof Client) {
            return uid.equals(((Client) obj).getClientUid());
        }
        return super.equals(obj);
    }

    @Override
    public @Nullable X509Certificate getClientCertificate()
    {
        return certificate;
    }

    @Override
    public long getClientLastUsageTime()
    {
        return lastUsageTime;
    }

    @Override
    public @NotNull String getClientManufacturer()
    {
        return manufacturer;
    }

    @Override
    public @NotNull String getClientNickname()
    {
        return nickname;
    }

    @Override
    public byte @NotNull [] getClientPictureData()
    {
        return pictureData;
    }

    @Override
    public int getClientPictureChecksum()
    {
        return pictureChecksum;
    }

    @Override
    public @NotNull String getClientProduct()
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
    public @NotNull ClientType getClientType()
    {
        return type;
    }

    @Override
    public @NotNull String getClientUid()
    {
        return uid;
    }

    @Override
    public int getClientVersionCode()
    {
        return versionCode;
    }

    @Override
    public @NotNull String getClientVersionName()
    {
        return versionName;
    }

    @Override
    public boolean hasPicture()
    {
        return pictureData.length > 0;
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
    public void setClientCertificate(@Nullable X509Certificate certificate)
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
    public void setClientManufacturer(@NotNull String manufacturer)
    {
        this.manufacturer = manufacturer;
    }

    @Override
    public void setClientNickname(@NotNull String nickname)
    {
        this.nickname = nickname;
    }

    @Override
    public void setClientProduct(@NotNull String product)
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
    public void setClientType(@NotNull ClientType type)
    {
        this.type = type;
    }

    @Override
    public void setClientUid(@NotNull String uid)
    {
        this.uid = uid;
    }

    @Override
    public void setClientVersionCode(int versionCode)
    {
        this.versionCode = versionCode;
    }

    @Override
    public void setClientVersionName(@NotNull String versionName)
    {
        this.versionName = versionName;
    }
}
