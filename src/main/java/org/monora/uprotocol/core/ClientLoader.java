package org.monora.uprotocol.core;

import org.json.JSONException;
import org.json.JSONObject;
import org.monora.uprotocol.core.persistence.PersistenceException;
import org.monora.uprotocol.core.persistence.PersistenceProvider;
import org.monora.uprotocol.core.protocol.Client;
import org.monora.uprotocol.core.protocol.ClientAddress;
import org.monora.uprotocol.core.protocol.ClientType;
import org.monora.uprotocol.core.protocol.ConnectionFactory;
import org.monora.uprotocol.core.protocol.communication.ProtocolException;
import org.monora.uprotocol.core.protocol.communication.client.BlockedRemoteClientException;
import org.monora.uprotocol.core.spec.v1.Keyword;

import java.io.IOException;
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
     * @param client              To load into.
     * @throws JSONException If something goes wrong when inflating the JSON data.
     */
    public static void loadAsClient(PersistenceProvider persistenceProvider, JSONObject object, Client client)
            throws JSONException
    {
        client.setClientBlocked(false);
        loadFrom(persistenceProvider, object, client);
    }

    /**
     * Load the client details from the JSON as the server.
     *
     * @param persistenceProvider That stores persistent data.
     * @param object              To load the details from.
     * @param client              To load into.
     * @param hasPin              Whether the request has a valid PIN. When it does, the remote client will be unblocked
     *                            if blocked.
     * @throws JSONException                If something goes wrong when inflating the JSON data.
     * @throws BlockedRemoteClientException If remote is blocked and has no valid PIN. The underlying data is loaded
     *                                      after this is thrown.
     */
    public static void loadAsServer(PersistenceProvider persistenceProvider, JSONObject object, Client client,
                                    boolean hasPin) throws JSONException, BlockedRemoteClientException
    {
        try {
            try {
                persistenceProvider.sync(client);
            } catch (PersistenceException ignored) {
            }

            if (hasPin) {
                client.setClientBlocked(false);
                client.setClientTrusted(true);
            } else if (client.isClientBlocked())
                throw new BlockedRemoteClientException(client);
        } finally {
            loadFrom(persistenceProvider, object, client);
        }
    }

    private static void loadFrom(PersistenceProvider persistenceProvider, JSONObject response, Client client)
            throws JSONException
    {
        client.setClientLocal(persistenceProvider.getClientUid().equals(client.getClientUid()));
        client.setClientManufacturer(response.getString(Keyword.CLIENT_MANUFACTURER));
        client.setClientProduct(response.getString(Keyword.CLIENT_PRODUCT));
        client.setClientNickname(response.getString(Keyword.CLIENT_NICKNAME));
        client.setClientType(ClientType.from(response.getString(Keyword.CLIENT_TYPE)));
        client.setClientLastUsageTime(System.currentTimeMillis());
        client.setClientVersionCode(response.getInt(Keyword.CLIENT_VERSION_CODE));
        client.setClientVersionName(response.getString(Keyword.CLIENT_VERSION_NAME));
        client.setClientProtocolVersion(response.getInt(Keyword.CLIENT_PROTOCOL_VERSION));
        client.setClientProtocolVersionMin(response.getInt(Keyword.CLIENT_PROTOCOL_VERSION_MIN));

        if (client.getClientNickname().length() > LENGTH_CLIENT_USERNAME)
            client.setClientNickname(client.getClientNickname().substring(0, LENGTH_CLIENT_USERNAME));

        persistenceProvider.save(client);

        byte[] clientPicture;
        try {
            clientPicture = Base64.getDecoder().decode(response.getString(Keyword.CLIENT_PICTURE));
        } catch (Exception ignored) {
            clientPicture = new byte[0];
        }

        persistenceProvider.saveClientPicture(client.getClientUid(), clientPicture);
    }

    /**
     * Load a client's details by connection to its internet address.
     *
     * @param connectionFactory   That will set up the connection.
     * @param persistenceProvider That stores the persistent data.
     * @param clientAddress       Where the remote client resides on the network.
     * @return The remote client.
     * @throws IOException          If an IO related error occurs.
     * @throws ProtocolException    If a protocol related error occurs.
     * @throws CertificateException If the existing certificates fail to allow an ecrypted communication.
     */
    public static Client load(ConnectionFactory connectionFactory, PersistenceProvider persistenceProvider,
                              ClientAddress clientAddress) throws IOException, ProtocolException, CertificateException
    {
        try (CommunicationBridge bridge = CommunicationBridge.connect(connectionFactory, persistenceProvider,
                clientAddress, null, 0)) {
            bridge.send(false);
            return bridge.getRemoteClient();
        }
    }
}
