package org.monora.uprotocol.core.protocol;

import org.monora.uprotocol.core.persistence.PersistenceException;
import org.monora.uprotocol.core.persistence.PersistenceProvider;

import java.security.cert.X509Certificate;

/**
 * This class helps move and copy an {@link Client} object.
 */
public class Clients
{
    /**
     * Copy one {@link Client} to another.
     *
     * @param from The instance to copy from.
     * @param to   The instance to copy to.
     */
    public static void copy(Client from, Client to)
    {
        fill(to, from.getClientUid(), from.getClientCertificate(), from.getClientNickname(), from.getClientManufacturer(),
                from.getClientProduct(), from.getClientType(), from.getClientVersionName(), from.getClientVersionCode(),
                from.getClientProtocolVersion(), from.getClientProtocolVersionMin(), from.isClientTrusted(),
                from.isClientBlocked());
    }

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
    public static void fill(Client client, String uid, X509Certificate certificate, String nickname, String manufacturer,
                            String product, ClientType clientType, String versionName, int versionCode,
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

    /**
     * Find and return a known client using its unique identifier or fail if the client is unknown.
     *
     * @param persistenceProvider On which this will search.
     * @param uid                 Of the client.
     * @return The known client instance.
     * @throws PersistenceException If the client isn't known yet.
     */
    public static Client getClientOrFail(PersistenceProvider persistenceProvider, String uid)
            throws PersistenceException
    {
        Client client = persistenceProvider.getClientFor(uid);

        if (client == null) {
            throw new PersistenceException("There is no client for the requested unique identifier");
        }

        return client;
    }
}
