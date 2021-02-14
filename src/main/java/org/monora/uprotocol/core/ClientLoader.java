package org.monora.uprotocol.core;

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
import java.util.Base64;

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
     * @return The client produced from the JSON object and persistence database.
     * @throws JSONException If something goes wrong when inflating the JSON data.
     */
    public static Client loadAsClient(PersistenceProvider persistenceProvider, JSONObject object, String clientUid)
            throws JSONException
    {
        return loadFrom(persistenceProvider, object, clientUid, true, false);
    }

    /**
     * Load the client details from the JSON as the server.
     *
     * @param persistenceProvider That stores persistent data.
     * @param object              To load the details from.
     * @param clientUid           For which this method invocation is being made.
     * @param hasPin              Whether the request has a valid PIN. When it does, the remote client will be unblocked
     *                            if blocked.
     * @return The client produced from the JSON object and persistence database.
     * @throws JSONException                If something goes wrong when inflating the JSON data.
     * @throws BlockedRemoteClientException If remote is blocked and has no valid PIN. The underlying data is loaded
     *                                      after this is thrown.
     */
    public static Client loadAsServer(PersistenceProvider persistenceProvider, JSONObject object, String clientUid,
                                      boolean hasPin) throws JSONException, BlockedRemoteClientException
    {
        Client client = loadFrom(persistenceProvider, object, clientUid, false, hasPin);

        if (client.isClientBlocked())
            throw new BlockedRemoteClientException(client);

        return client;
    }

    private static Client loadFrom(PersistenceProvider persistenceProvider, JSONObject response, String clientUid,
                                   boolean asClient, boolean hasPin) throws JSONException
    {
        String nickname = response.getString(Keyword.CLIENT_NICKNAME);
        String manufacturer = response.getString(Keyword.CLIENT_MANUFACTURER);
        String product = response.getString(Keyword.CLIENT_PRODUCT);
        ClientType clientType = ClientType.from(response.getString(Keyword.CLIENT_TYPE));
        String versionName = response.getString(Keyword.CLIENT_VERSION_NAME);
        int versionCode = response.getInt(Keyword.CLIENT_VERSION_CODE);
        int protocolVersion = response.getInt(Keyword.CLIENT_PROTOCOL_VERSION);
        int protocolVersionMin = response.getInt(Keyword.CLIENT_PROTOCOL_VERSION_MIN);
        long lastUsageTime = System.currentTimeMillis();
        boolean local = persistenceProvider.getClientUid().equals(clientUid);

        if (nickname.length() > LENGTH_CLIENT_USERNAME)
            nickname = nickname.substring(0, LENGTH_CLIENT_USERNAME);

        byte[] clientPicture;
        try {
            clientPicture = Base64.getDecoder().decode(response.getString(Keyword.CLIENT_PICTURE));
        } catch (Exception ignored) {
            clientPicture = new byte[0];
        }

        Client client = persistenceProvider.getClientFor(clientUid);
        if (client == null) {
            client = persistenceProvider.createClientFor(clientUid, nickname, manufacturer, product, clientType,
                    versionName, versionCode, protocolVersion, protocolVersionMin);
        } else {
            Clients.fill(client, clientUid, client.getClientCertificate(), nickname, manufacturer, product, clientType,
                    versionName, versionCode, protocolVersion, protocolVersionMin, client.isClientTrusted(),
                    client.isClientBlocked());
        }

        if (asClient) {
            client.setClientBlocked(false);
        } else if (hasPin) {
            client.setClientBlocked(false);
            client.setClientTrusted(true);
        }

        client.setClientLastUsageTime(lastUsageTime);
        client.setClientLocal(local);
        persistenceProvider.save(client);
        persistenceProvider.saveClientPicture(client.getClientUid(), clientPicture);

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
    public static Client load(ConnectionFactory connectionFactory, PersistenceProvider persistenceProvider,
                              InetAddress inetAddress) throws IOException, ProtocolException, CertificateException
    {
        try (CommunicationBridge bridge = CommunicationBridge.connect(connectionFactory, persistenceProvider,
                inetAddress, null, 0)) {
            bridge.send(false);
            return bridge.getRemoteClient();
        }
    }
}
