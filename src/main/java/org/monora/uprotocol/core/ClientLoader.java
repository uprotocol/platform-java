package org.monora.uprotocol.core;

import net.iharder.Base64;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.monora.uprotocol.core.persistence.PersistenceProvider;
import org.monora.uprotocol.core.protocol.Client;
import org.monora.uprotocol.core.protocol.ClientType;
import org.monora.uprotocol.core.protocol.Clients;
import org.monora.uprotocol.core.protocol.ConnectionFactory;
import org.monora.uprotocol.core.protocol.communication.ProtocolException;
import org.monora.uprotocol.core.protocol.communication.client.BlockedRemoteClientException;
import org.monora.uprotocol.core.spec.v1.Keyword;

import java.io.IOException;
import java.net.InetAddress;
import java.security.cert.CertificateException;

import static org.monora.uprotocol.core.spec.v1.Config.LENGTH_CLIENT_USERNAME;

/**
 * Handles loading details of remote.
 */
public class ClientLoader
{
    /**
     * Load the client details from the JSON as a client to the server.
     *
     * @param persistenceProvider That stores persistent data.
     * @param object              To load the details from.
     * @param clientUid           For which this method invocation is being made.
     * @param unblock             True if this should unblock the remote if blocked.
     * @return The client produced from the JSON object and persistence database.
     * @throws JSONException                If something goes wrong when inflating the JSON data.
     * @throws BlockedRemoteClientException If the remote is blocked on the side and 'unblock' parameter is false.
     */
    public static @NotNull Client loadAsClient(@NotNull PersistenceProvider persistenceProvider,
                                               @NotNull JSONObject object, @NotNull String clientUid,
                                               boolean unblock)
            throws JSONException, BlockedRemoteClientException
    {
        Client client = loadFrom(persistenceProvider, object, clientUid, false, true, unblock);

        if (client.isClientBlocked()) {
            throw new BlockedRemoteClientException(client);
        }

        return client;
    }

    /**
     * Load the client details from the JSON as the server.
     *
     * @param persistenceProvider That stores persistent data.
     * @param object              To load the details from.
     * @param clientUid           For which this method invocation is being made.
     * @param hasPin              Whether the request has a valid PIN. When it does, the remote client will be unblocked
     *                            if blocked.
     * @return The client data produced from the JSON object and persistence database.
     * @throws JSONException                If something goes wrong when inflating the JSON data.
     * @throws BlockedRemoteClientException If remote is blocked and has no valid PIN. The underlying data is loaded
     *                                      after this is thrown.
     */
    public static @NotNull Client loadAsServer(@NotNull PersistenceProvider persistenceProvider,
                                               @NotNull JSONObject object, @NotNull String clientUid, boolean hasPin)
            throws JSONException, BlockedRemoteClientException
    {
        Client client = loadFrom(persistenceProvider, object, clientUid, hasPin, false, false);

        if (client.isClientBlocked()) {
            throw new BlockedRemoteClientException(client);
        }

        return client;
    }

    private static @NotNull Client loadFrom(@NotNull PersistenceProvider persistenceProvider,
                                            @NotNull JSONObject response, @NotNull String clientUid, boolean hasPin,
                                            boolean asClient, boolean unblockAsClient) throws JSONException
    {
        final String nickname = Clients.cleanNickname(response.getString(Keyword.CLIENT_NICKNAME));
        final String manufacturer = response.getString(Keyword.CLIENT_MANUFACTURER);
        final String product = response.getString(Keyword.CLIENT_PRODUCT);
        final ClientType clientType = ClientType.from(response.getString(Keyword.CLIENT_TYPE));
        final String versionName = response.getString(Keyword.CLIENT_VERSION_NAME);
        final int versionCode = response.getInt(Keyword.CLIENT_VERSION_CODE);
        final int protocolVersion = response.getInt(Keyword.CLIENT_PROTOCOL_VERSION);
        final int protocolVersionMin = response.getInt(Keyword.CLIENT_PROTOCOL_VERSION_MIN);
        final long revisionOfPicture = response.getLong(Keyword.CLIENT_REVISION_PICTURE);
        final long lastUsageTime = System.currentTimeMillis();
        final boolean local = persistenceProvider.getClientUid().equals(clientUid);

        Client client = persistenceProvider.getClientFor(clientUid);

        final boolean updating = client != null;
        final boolean needsPictureRevision = client == null || client.getClientRevisionOfPicture() != revisionOfPicture;

        if (client == null) {
            client = persistenceProvider.createClientFor(clientUid, nickname, manufacturer, product, clientType,
                    versionName, versionCode, protocolVersion, protocolVersionMin, revisionOfPicture);
        } else {
            Clients.fill(client, clientUid, client.getClientCertificate(), nickname, manufacturer, product, clientType,
                    versionName, versionCode, protocolVersion, protocolVersionMin, revisionOfPicture,
                    client.isClientTrusted(), client.isClientBlocked());
        }

        try {
            if (asClient) {
                if (unblockAsClient) {
                    client.setClientBlocked(false);
                }
            } else if (hasPin) {
                client.setClientBlocked(false);
                client.setClientTrusted(true);
            }
        } finally {
            client.setClientLastUsageTime(lastUsageTime);
            client.setClientLocal(local);
            persistenceProvider.persist(client, updating);

            if (needsPictureRevision) {
                String data = response.optString(Keyword.CLIENT_PICTURE);
                try {
                    persistenceProvider.persistClientPicture(client, data != null ? Base64.decode(data) : null);
                } catch (IOException ignored) {
                }
            }
        }

        return client;
    }

    /**
     * Load a client's details by connection to its internet address.
     *
     * @param connectionFactory   That will set up the connection.
     * @param persistenceProvider That stores the persistent data.
     * @param inetAddress         Where the remote client resides on the network.
     * @return The remote client.
     * @throws IOException          If an IO related error occurs.
     * @throws ProtocolException    If a protocol related error occurs.
     * @throws CertificateException If the existing certificates fail to allow an encrypted communication.
     */
    public static @NotNull Client load(@NotNull ConnectionFactory connectionFactory,
                                       @NotNull PersistenceProvider persistenceProvider,
                                       @NotNull InetAddress inetAddress)
            throws IOException, ProtocolException, CertificateException
    {
        CommunicationBridge.Builder builder = new CommunicationBridge.Builder(connectionFactory, persistenceProvider,
                inetAddress);
        builder.setClearBlockedStatus(false);

        try (CommunicationBridge bridge = builder.connect()) {
            bridge.send(false);
            return bridge.getRemoteClient();
        }
    }
}
