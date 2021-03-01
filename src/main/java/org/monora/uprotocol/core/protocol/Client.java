package org.monora.uprotocol.core.protocol;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.cert.X509Certificate;

/**
 * This interface helps generate client specific information.
 *
 * @see Clients
 */
public interface Client
{
    /**
     * This certificate helps secure the communication between two or more devices.
     *
     * @return The client's X.509 certificate used for authentication and encryption.
     * @see #setClientCertificate(X509Certificate)
     */
    @Nullable X509Certificate getClientCertificate();

    /**
     * The last time contacted with this client.
     *
     * @return The time in UNIX epoch format.
     * @see #setClientLastUsageTime(long)
     */
    long getClientLastUsageTime();

    /**
     * The manufacturer of the client specifies the specific environment.
     *
     * @return The manufacturer of the client.
     * @see #setClientManufacturer(String)
     * @see #getClientProduct()
     */
    @NotNull String getClientManufacturer();

    /**
     * The preferred nickname for the client.
     *
     * @return The client's nickname of choice.
     * @see #setClientNickname(String)
     */
    @NotNull String getClientNickname();

    /**
     * The product is an extra information under {@link #getClientManufacturer()}.
     *
     * @return The product name of the client (sub-brand).
     * @see #setClientProduct(String)
     * @see #getClientManufacturer()
     */
    @NotNull String getClientProduct();

    /**
     * The protocol version the specific version that the client was designed to work with.
     *
     * @return The target protocol (uprotocol specification) version.
     * @see #setClientProtocolVersion(int)
     * @see #getClientProtocolVersionMin()
     */
    int getClientProtocolVersion();

    /**
     * The minimum supported protocol version shows the oldest generation of protocol that the client can work with.
     *
     * @return The minimum supported protocol by the client.
     * @see #setClientProtocolVersionMin(int)
     * @see #getClientProtocolVersion()
     */
    int getClientProtocolVersionMin();

    /**
     * This tells on which type of device that the client runs on.
     *
     * @return The type of device that the client runs on.
     * @see #setClientType(ClientType)
     */
    @NotNull ClientType getClientType();

    /**
     * Represents the unique identifier for the client.
     *
     * @return The unique identifier of the client.
     * @see #setClientUid(String)
     */
    @NotNull String getClientUid();

    /**
     * The client specific version code.
     * <p>
     * This shouldn't tell anything about the protocol itself.
     *
     * @return The client's version code.
     * @see #setClientVersionCode(int)
     * @see #getClientProtocolVersion()
     */
    int getClientVersionCode();

    /**
     * The client specific version name.
     * <p>
     * This shouldn't tell anything about the protocol itself.
     *
     * @return The client's version name.
     * @see #setClientVersionName(String)
     * @see #getClientProtocolVersion()
     */
    @NotNull String getClientVersionName();

    /**
     * Whether or not the (remote) client is blocked on this client.
     *
     * @return True if the client is blocked or false if not.
     * @see #setClientBlocked(boolean)
     */
    boolean isClientBlocked();

    /**
     * Whether or not this client instance points to the client itself.
     *
     * @return True if this is a loopback client.
     * @see #setClientLocal(boolean)
     */
    boolean isClientLocal();

    /**
     * Whether or not the (remote) client is trusted on this client.
     * <p>
     * Trusted devices has more access rights than those who don't.
     *
     * @return True if this client is trusted.
     * @see #setClientTrusted(boolean)
     */
    boolean isClientTrusted();

    /**
     * Set whether or not this client is blocked.
     *
     * @param blocked True if blocked or false if otherwise.
     * @see #isClientBlocked()
     */
    void setClientBlocked(boolean blocked);

    /**
     * Set the client trust certificate.
     *
     * @param certificate The certificate.
     * @see #getClientCertificate()
     */
    void setClientCertificate(@Nullable X509Certificate certificate);

    /**
     * Set the last usage time of this client.
     *
     * @param lastUsageTime The last usage time in UNIX epoch time format.
     * @see #getClientLastUsageTime()
     */
    void setClientLastUsageTime(long lastUsageTime);

    /**
     * Set whether or not this client is a loopback instance.
     *
     * @param local True if this is a loopback client representing itself or false if otherwise.
     * @see #isClientLocal()
     */
    void setClientLocal(boolean local);

    /**
     * Set the client's manufacturer.
     *
     * @param manufacturer The manufacturer.
     * @see #getClientManufacturer()
     * @see #setClientProduct(String)
     */
    void setClientManufacturer(@NotNull String manufacturer);

    /**
     * Set the nickname of this client.
     *
     * @param nickname To be set.
     * @see #getClientNickname()
     */
    void setClientNickname(@NotNull String nickname);

    /**
     * Set the product name of this client (sub-brand)
     *
     * @param product The name.
     * @see #getClientProduct()
     * @see #setClientManufacturer(String)
     */
    void setClientProduct(@NotNull String product);

    /**
     * Set the target protocol version of the client.
     *
     * @param protocolVersion That the client is targeting.
     * @see #getClientProtocolVersion()
     * @see #setClientProtocolVersionMin(int)
     */
    void setClientProtocolVersion(int protocolVersion);

    /**
     * Set the minimum protocol version supported by the client.
     *
     * @param protocolVersionMin That the client supports.
     * @see #getClientProtocolVersionMin()
     * @see #setClientProtocolVersion(int)
     */
    void setClientProtocolVersionMin(int protocolVersionMin);

    /**
     * Set whether or not this client is trusted.
     *
     * @param trusted True if trusted or false if otherwise.
     * @see #isClientTrusted()
     */
    void setClientTrusted(boolean trusted);

    /**
     * Set the type of the client (the type of device that the client runs on).
     *
     * @param type That the client runs on.
     * @see #getClientType()
     */
    void setClientType(@NotNull ClientType type);

    /**
     * Set the client's unique identifier.
     *
     * @param uid To be set.
     * @see #getClientUid()
     */
    void setClientUid(@NotNull String uid);

    /**
     * Set the client's version code.
     *
     * @param versionCode To be set.
     * @see #getClientVersionCode()
     * @see #setClientVersionName(String)
     */
    void setClientVersionCode(int versionCode);

    /**
     * Set the client's version name.
     *
     * @param versionName To be set.
     * @see #getClientVersionName()
     * @see #setClientVersionCode(int)
     */
    void setClientVersionName(@NotNull String versionName);
}
