package org.monora.uprotocol.core;

import org.json.JSONException;
import org.json.JSONObject;
import org.monora.uprotocol.core.network.Client;
import org.monora.uprotocol.core.network.ClientAddress;
import org.monora.uprotocol.core.persistence.PersistenceException;
import org.monora.uprotocol.core.persistence.PersistenceProvider;
import org.monora.uprotocol.core.protocol.ClientType;
import org.monora.uprotocol.core.protocol.ConnectionFactory;
import org.monora.uprotocol.core.protocol.communication.client.BlockedRemoteClientException;
import org.monora.uprotocol.core.spec.v1.Keyword;

import java.net.InetAddress;
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
        client.isBlocked = false;
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
     * @throws BlockedRemoteClientException If remote is blocked and has no valid PIN.
     */
    public static void loadAsServer(PersistenceProvider persistenceProvider, JSONObject object, Client client,
                                    boolean hasPin) throws JSONException, BlockedRemoteClientException
    {
        client.uid = object.getString(Keyword.CLIENT_UID);
        if (hasPin)
            client.isTrusted = true;

        try {
            try {
                persistenceProvider.sync(client);
            } catch (PersistenceException ignored) {
            }

            if (hasPin) {
                client.isBlocked = false;
            } else if (client.isBlocked)
                throw new BlockedRemoteClientException(client);
        } finally {
            loadFrom(persistenceProvider, object, client);
        }
    }

    private static void loadFrom(PersistenceProvider persistenceProvider, JSONObject object, Client client)
            throws JSONException
    {
        client.isLocal = persistenceProvider.getClientUid().equals(client.uid);
        client.brand = object.getString(Keyword.CLIENT_MANUFACTURER);
        client.model = object.getString(Keyword.CLIENT_PRODUCT);
        client.username = object.getString(Keyword.CLIENT_USERNAME);
        client.clientType = object.getEnum(ClientType.class, Keyword.CLIENT_TYPE);
        client.lastUsageTime = System.currentTimeMillis();
        client.versionCode = object.getInt(Keyword.CLIENT_VERSION_CODE);
        client.versionName = object.getString(Keyword.CLIENT_VERSION_NAME);
        client.protocolVersion = object.getInt(Keyword.CLIENT_PROTOCOL_VERSION);
        client.protocolVersionMin = object.getInt(Keyword.CLIENT_PROTOCOL_VERSION_MIN);

        if (client.username.length() > LENGTH_CLIENT_USERNAME)
            client.username = client.username.substring(0, LENGTH_CLIENT_USERNAME);

        persistenceProvider.save(client);

        byte[] clientPicture;
        try {
            clientPicture = Base64.getDecoder().decode(object.getString(Keyword.CLIENT_PICTURE));
        } catch (Exception ignored) {
            clientPicture = new byte[0];
        }

        persistenceProvider.saveClientPicture(client.uid, clientPicture);
    }

    /**
     * Load a client's details by connection to its internet address.
     *
     * @param connectionFactory   That will set up the connection.
     * @param persistenceProvider That stores the persistent data.
     * @param address             To connect to.
     * @param listener            That listens for successful attempts. Pass it as null if unneeded.
     */
    public static void load(ConnectionFactory connectionFactory, PersistenceProvider persistenceProvider,
                            InetAddress address, OnClientResolvedListener listener)
    {
        new Thread(() -> {
            try (CommunicationBridge bridge = CommunicationBridge.connect(connectionFactory, persistenceProvider,
                    persistenceProvider.createClientAddressFor(address), null, 0)) {
                bridge.sendResult(false);
                if (listener != null)
                    listener.onClientResolved(bridge.getRemoteClient(), bridge.getRemoteClientAddress());
            } catch (Exception ignored) {
            }
        }).start();
    }

    /**
     * Callback for delivering the details for a client when successful.
     */
    public interface OnClientResolvedListener
    {
        /**
         * Called after the connection is successful.
         *
         * @param client  The client that the loader found.
         * @param address The address where the client resides.
         */
        void onClientResolved(Client client, ClientAddress address);
    }
}
