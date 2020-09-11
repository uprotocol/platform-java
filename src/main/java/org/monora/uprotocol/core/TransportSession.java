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

public class TransportSession extends CoolSocket
{
    private final PersistenceProvider persistenceProvider;
    private final TransportSeat transportSeat;

    public TransportSession(PersistenceProvider persistenceProvider, TransportSeat transportSeat)
    {
        super(Config.SERVER_PORT_COMMUNICATION);

        getConfigFactory().setReadTimeout(CommunicationBridge.TIMEOUT_SOCKET_DEFAULT);
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
            final boolean hasPin = activePin != -1 && activePin == response.getInt(Keyword.DEVICE_PIN);
            final Device device = persistenceProvider.createDevice();
            final DeviceAddress deviceAddress = persistenceProvider.createDeviceAddressFor(
                    activeConnection.getAddress());
            boolean sendInfo = true;

            try {
                DeviceLoader.loadAsServer(persistenceProvider, response, device, hasPin);
            } catch (DeviceVerificationException e) {
                transportSeat.notifyDeviceKeyChanged(device, e.receiverKey, persistenceProvider.generateKey());
                throw e;
            } catch (Exception e) {
                sendInfo = false;
                throw e;
            } finally {
                persistenceProvider.save(device, deviceAddress);
                persistenceProvider.broadcast();

                if (sendInfo)
                    // TODO: 9/9/20 Are we sending a key that should belong to the other user even when a key error is
                    //  the case?
                    CommunicationBridge.sendSecure(activeConnection, true,
                            persistenceProvider.toJson(device.senderKey, 0));
            }

            CommunicationBridge.sendResult(activeConnection, true);

            if (hasPin) // pin is known, should be changed. Warn the listeners.
                persistenceProvider.revokeNetworkPin();

            activeConnection.setInternalCacheLimit(1073741824); // 1MB

            final CommunicationBridge bridge = new CommunicationBridge(persistenceProvider, activeConnection, device,
                    deviceAddress);
            handleRequest(bridge, device, deviceAddress, hasPin, activeConnection.receive().getAsJson());
        } catch (Exception e) {
            e.printStackTrace();
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

                if (transportSeat.hasTransferIndexingFor(transferId))
                    throw new ContentException(ContentException.Error.AlreadyExists);
                else {
                    transportSeat.handleFileTransfer(device, hasPin, transferId, jsonIndex);
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
                else if (transportSeat.hasTransferFor(transferId, device.uid, type))
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
