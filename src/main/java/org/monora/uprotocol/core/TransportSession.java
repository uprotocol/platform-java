package org.monora.uprotocol.core;

import org.json.JSONException;
import org.json.JSONObject;
import org.monora.coolsocket.core.CoolSocket;
import org.monora.coolsocket.core.session.ActiveConnection;
import org.monora.uprotocol.core.network.Device;
import org.monora.uprotocol.core.network.DeviceAddress;
import org.monora.uprotocol.core.network.TransferItem;
import org.monora.uprotocol.core.persistence.PersistenceException;
import org.monora.uprotocol.core.persistence.PersistenceProvider;
import org.monora.uprotocol.core.protocol.DeviceVerificationException;
import org.monora.uprotocol.core.protocol.communication.CommunicationException;
import org.monora.uprotocol.core.protocol.communication.ContentException;
import org.monora.uprotocol.core.spec.alpha.Config;
import org.monora.uprotocol.core.spec.alpha.Keyword;

import java.io.IOException;

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
    private final PersistenceProvider persistenceProvider;

    private final TransportSeat transportSeat;

    /**
     * Create a new session instance.
     *
     * @param persistenceProvider Where persistent data will be stored.
     * @param transportSeat       Which will manage the requests and do appropriate actions.
     */
    public TransportSession(PersistenceProvider persistenceProvider, TransportSeat transportSeat)
    {
        super(Config.PORT_UPROTOCOL);

        getConfigFactory().setReadTimeout(Config.TIMEOUT_SOCKET_DEFAULT);
        this.persistenceProvider = persistenceProvider;
        this.transportSeat = transportSeat;
    }

    @Override
    public void onConnected(ActiveConnection activeConnection)
    {
        try {
            activeConnection.reply(persistenceProvider.getDeviceUid());

            JSONObject response = activeConnection.receive().getAsJson();
            final int activePin = persistenceProvider.getNetworkPin();
            final boolean hasPin = activePin != 0 && activePin == response.getInt(Keyword.DEVICE_PIN);
            final Device device = persistenceProvider.createDevice();
            final DeviceAddress deviceAddress = persistenceProvider.createDeviceAddressFor(
                    activeConnection.getAddress());

            if (hasPin)
                persistenceProvider.revokeNetworkPin();

            try {
                DeviceLoader.loadAsServer(persistenceProvider, response, device, hasPin);
                CommunicationBridge.sendSecure(activeConnection, true,
                        persistenceProvider.deviceAsJson(device.senderKey, 0));
            } catch (DeviceVerificationException e) {
                int newSenderKey = persistenceProvider.generateKey();

                persistenceProvider.saveKeyInvalidationRequest(device.uid, e.receiverKey, newSenderKey);
                transportSeat.notifyDeviceKeyChanged(device, e.receiverKey, newSenderKey);
                CommunicationBridge.sendSecure(activeConnection, true,
                        persistenceProvider.deviceAsJson(newSenderKey, 0));

                throw e;
            } finally {
                persistenceProvider.broadcast();
            }

            CommunicationBridge.sendResult(activeConnection, true);

            activeConnection.setInternalCacheLimit(1073741824); // 1MB

            JSONObject request = activeConnection.receive().getAsJson();
            if (!CommunicationBridge.resultOf(request))
                return;

            handleRequest(new CommunicationBridge(persistenceProvider, activeConnection, device, deviceAddress,
                            false), device, deviceAddress, hasPin, request);
        } catch (Exception e) {
            try {
                CommunicationBridge.sendError(activeConnection, e);
            } catch (Exception ignored) {
            }
        }
    }

    private void handleRequest(CommunicationBridge bridge, Device device, DeviceAddress deviceAddress,
                               boolean hasPin, JSONObject response) throws JSONException, IOException,
            PersistenceException, CommunicationException
    {
        switch (response.getString(Keyword.REQUEST)) {
            case (Keyword.REQUEST_TRANSFER): {
                long transferId = response.getLong(Keyword.TRANSFER_ID);
                String jsonIndex = response.getString(Keyword.INDEX);

                if (transportSeat.hasOngoingIndexingFor(transferId) || persistenceProvider.containsTransfer(transferId))
                    throw new ContentException(ContentException.Error.AlreadyExists);
                else {
                    transportSeat.handleFileTransferRequest(device, hasPin, transferId, jsonIndex);
                    bridge.sendResult(true);
                }
                return;
            }
            case (Keyword.REQUEST_NOTIFY_TRANSFER_STATE): {
                int transferId = response.getInt(Keyword.TRANSFER_ID);
                boolean isAccepted = response.getBoolean(Keyword.TRANSFER_IS_ACCEPTED);

                transportSeat.handleFileTransferState(device, transferId, isAccepted);
                bridge.sendResult(true);
                return;
            }
            case (Keyword.REQUEST_TRANSFER_TEXT):
                transportSeat.handleTextTransfer(device, response.getString(Keyword.TRANSFER_TEXT));
                bridge.sendResult(true);
                return;
            case (Keyword.REQUEST_ACQUAINTANCE):
                transportSeat.handleAcquaintanceRequest(device, deviceAddress);
                bridge.sendResult(true);
                return;
            case (Keyword.REQUEST_TRANSFER_JOB):
                int transferId = response.getInt(Keyword.TRANSFER_ID);
                TransferItem.Type type = response.getEnum(TransferItem.Type.class, Keyword.TRANSFER_TYPE);

                // The type is reversed to match our side
                if (TransferItem.Type.INCOMING.equals(type))
                    type = TransferItem.Type.OUTGOING;
                else if (TransferItem.Type.OUTGOING.equals(type))
                    type = TransferItem.Type.INCOMING;

                if (TransferItem.Type.INCOMING.equals(type) && !device.isTrusted)
                    bridge.sendError(Keyword.ERROR_NOT_TRUSTED);
                else if (transportSeat.hasOngoingTransferFor(transferId, device.uid, type))
                    throw new ContentException(ContentException.Error.NotAccessible);
                else {
                    bridge.sendResult(true);
                    transportSeat.beginFileTransfer(bridge, device, transferId, type);
                }
                return;
            default:
                bridge.sendResult(false);
        }
    }
}
