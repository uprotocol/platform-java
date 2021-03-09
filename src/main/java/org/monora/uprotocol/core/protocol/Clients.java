package org.monora.uprotocol.core.protocol;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.monora.uprotocol.core.persistence.PersistenceException;
import org.monora.uprotocol.core.persistence.PersistenceProvider;

import java.security.cert.X509Certificate;

/**
 * This class helps move and copy an {@link Client} object.
 */
public class Clients
{
    /**
     * Fill in the details of a {@link Client} instance.
     *
     * @param client             To fill in.
     * @param uid                The client uid.
     * @param certificate        The client certificate.
     * @param nickname           The client nickname.
     * @param manufacturer       The client manufacturer.
     * @param product            The client product.
     * @param clientType         The client type.
     * @param versionName        The client version name.
     * @param versionCode        The client version code.
     * @param protocolVersion    The client protocol version.
     * @param protocolVersionMin The minimum protocol version by the client.
     * @param isTrusted          Whether the client is trusted or not.
     * @param isBlocked          Whether the client is blocked or not.
     */
    public static void fill(@NotNull Client client, @NotNull String uid, @Nullable X509Certificate certificate,
                            @NotNull String nickname, @NotNull String manufacturer, @NotNull String product,
                            @NotNull ClientType clientType, @NotNull String versionName, int versionCode,
                            int protocolVersion, int protocolVersionMin, boolean isTrusted, boolean isBlocked)
    {
        client.setClientUid(uid);
        client.setClientCertificate(certificate);
        client.setClientNickname(nickname);
        client.setClientManufacturer(manufacturer);
        client.setClientProduct(product);
        client.setClientType(clientType);
        client.setClientVersionName(versionName);
        client.setClientVersionCode(versionCode);
        client.setClientProtocolVersion(protocolVersion);
        client.setClientProtocolVersionMin(protocolVersionMin);
        client.setClientTrusted(isTrusted);
        client.setClientBlocked(isBlocked);
    }
}
