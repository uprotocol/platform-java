package org.monora.uprotocol.core;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.monora.coolsocket.core.CoolSocket;
import org.monora.coolsocket.core.session.ActiveConnection;
import org.monora.coolsocket.core.session.CancelledException;
import org.monora.coolsocket.core.session.ClosedException;
import org.monora.uprotocol.core.persistence.PersistenceException;
import org.monora.uprotocol.core.persistence.PersistenceProvider;
import org.monora.uprotocol.core.protocol.Client;
import org.monora.uprotocol.core.protocol.ClientAddress;
import org.monora.uprotocol.core.protocol.ConnectionFactory;
import org.monora.uprotocol.core.protocol.communication.ContentException;
import org.monora.uprotocol.core.protocol.communication.ProtocolException;
import org.monora.uprotocol.core.protocol.communication.SecurityException;
import org.monora.uprotocol.core.spec.v1.Config;
import org.monora.uprotocol.core.spec.v1.Keyword;
import org.monora.uprotocol.core.transfer.TransferItem;

import java.io.IOException;
import java.util.logging.Level;

/**
 * The server that accepts requests from clients.
 * <p>
 * There can only be one session that is started. You can check if a session started using
 * {@link TransportSession#isListening()}.
 * <p>
 * uprotocol sessions run on {@link Config#PORT_UPROTOCOL}.
 * <p>
 * They don't support different range of ports. For this reason, a session should be closed as soon as it becomes stale.
 * <p>
 * This will take {@link PersistenceProvider} and {@link TransportSeat} to save.
 */
public class TransportSession extends CoolSocket
{
    private final @NotNull ConnectionFactory connectionFactory;

    private final @NotNull PersistenceProvider persistenceProvider;

    private final @NotNull TransportSeat transportSeat;

    /**
     * Create a new session instance.
     *
     * @param connectionFactory   To start and set up connections with.
     * @param persistenceProvider Where persistent data will be stored.
     * @param transportSeat       Which will manage the requests and do appropriate actions.
     */
    public TransportSession(@NotNull ConnectionFactory connectionFactory,
                            @NotNull PersistenceProvider persistenceProvider, @NotNull TransportSeat transportSeat)
    {
        super(Config.PORT_UPROTOCOL);

        getConfigFactory().setReadTimeout(Config.TIMEOUT_SOCKET_DEFAULT);
        this.connectionFactory = connectionFactory;
        this.persistenceProvider = persistenceProvider;
        this.transportSeat = transportSeat;
    }

    @Override
    public void onConnected(@NotNull ActiveConnection activeConnection)
    {
        final JSONObject clientIndex = persistenceProvider.clientAsJson(0);

        try {
            activeConnection.reply(persistenceProvider.getClientUid());

            final JSONObject response = activeConnection.receive().getAsJson();
            final int activePin = persistenceProvider.getNetworkPin();
            final boolean hasPin = activePin != 0 && activePin == response.getInt(Keyword.CLIENT_PIN);

            if (hasPin) {
                persistenceProvider.revokeNetworkPin();
            }

            final String clientUid = response.getString(Keyword.CLIENT_UID);
            final Client client = ClientLoader.loadAsServer(persistenceProvider, response, clientUid, hasPin);
            final ClientAddress clientAddress = persistenceProvider.createClientAddressFor(
                    activeConnection.getAddress(), clientUid);

            Responses.send(activeConnection, true, clientIndex);

            CommunicationBridge.convertToSSL(connectionFactory, persistenceProvider, activeConnection, client,
                    false);

            activeConnection.setInternalCacheSize(0x500000); // 5MiB

            JSONObject request = activeConnection.receive().getAsJson();
            if (!Responses.getResult(request)) {
                return;
            }

            handleRequest(new CommunicationBridge(persistenceProvider, activeConnection, client, clientAddress),
                    client, clientAddress, hasPin, request);
        } catch (SecurityException e) {
            if (!persistenceProvider.hasRequestForInvalidationOfCredentials(e.client.getClientUid())) {
                persistenceProvider.saveRequestForInvalidationOfCredentials(e.client.getClientUid());
                transportSeat.notifyClientCredentialsChanged(e.client);
            }
        } catch (ClosedException e) {
            getLogger().log(Level.INFO, "Closed successfully by " + (e.remoteRequested ? "remote" : "you"));
        } catch (CancelledException e) {
            getLogger().log(Level.INFO, "Cancelled successfully by " + (e.remoteRequested ? "remote" : "you"));
        } catch (Exception e) {
            try {
                Responses.send(activeConnection, e, clientIndex);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private void handleRequest(@NotNull CommunicationBridge bridge, @NotNull Client client,
                               @NotNull ClientAddress clientAddress, boolean hasPin, @NotNull JSONObject response)
            throws JSONException, IOException, PersistenceException, ProtocolException
    {
        switch (response.getString(Keyword.REQUEST)) {
            case (Keyword.REQUEST_TRANSFER): {
                long groupId = response.getLong(Keyword.TRANSFER_GROUP_ID);
                String jsonIndex = response.getString(Keyword.INDEX);

                if (transportSeat.hasOngoingIndexingFor(groupId) || persistenceProvider.containsTransfer(groupId)) {
                    throw new ContentException(ContentException.Error.AlreadyExists);
                } else {
                    transportSeat.handleFileTransferRequest(client, hasPin, groupId, jsonIndex);
                    bridge.send(true);
                }
                return;
            }
            case (Keyword.REQUEST_NOTIFY_TRANSFER_STATE): {
                long groupId = response.getLong(Keyword.TRANSFER_GROUP_ID);
                boolean isAccepted = response.getBoolean(Keyword.TRANSFER_IS_ACCEPTED);

                transportSeat.handleFileTransferState(client, groupId, isAccepted);
                bridge.send(true);
                return;
            }
            case (Keyword.REQUEST_TRANSFER_TEXT):
                transportSeat.handleTextTransfer(client, response.getString(Keyword.TRANSFER_TEXT));
                bridge.send(true);
                return;
            case (Keyword.REQUEST_ACQUAINTANCE):
                transportSeat.handleAcquaintanceRequest(client, clientAddress);
                bridge.send(true);
                return;
            case (Keyword.REQUEST_TRANSFER_JOB):
                long groupId = response.getLong(Keyword.TRANSFER_GROUP_ID);
                TransferItem.Type type = TransferItem.Type.from(response.getString(Keyword.TRANSFER_TYPE));

                // The type is reversed to match our side
                if (TransferItem.Type.Incoming.equals(type)) {
                    type = TransferItem.Type.Outgoing;
                } else if (TransferItem.Type.Outgoing.equals(type)) {
                    type = TransferItem.Type.Incoming;
                }

                if (TransferItem.Type.Incoming.equals(type) && !client.isClientTrusted()) {
                    bridge.send(Keyword.ERROR_NOT_TRUSTED);
                } else if (transportSeat.hasOngoingTransferFor(groupId, client.getClientUid(), type)) {
                    throw new ContentException(ContentException.Error.NotAccessible);
                } else if (!persistenceProvider.containsTransfer(groupId)) {
                    throw new ContentException(ContentException.Error.NotFound);
                } else {
                    bridge.send(true);
                    transportSeat.beginFileTransfer(bridge, client, groupId, type);
                }
                return;
            default:
                bridge.send(false);
        }
    }
}
