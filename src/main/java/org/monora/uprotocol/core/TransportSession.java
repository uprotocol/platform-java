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
import org.monora.uprotocol.core.protocol.communication.NotAllowedException;
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
        // check if the same address has other connections and limit that to 5
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
                getNotificationHelper().notifyKeyChanged(device, e.receiveKey, persistenceProvider.generateKey());
                throw e;
            } catch (Exception e) {
                sendInfo = false;
                throw e;
            } finally {
                persistenceProvider.save(device, deviceAddress);
                getKuick().broadcast();

                if (sendInfo)
                    CommunicationBridge.sendSecure(activeConnection, true, PersistenceProvider.toJson(
                            persistenceProvider, device.sendKey, 0));
            }

            CommunicationBridge.sendResult(activeConnection, true);

            if (hasPin) // pin is known, should be changed. Warn the listeners.
                persistenceProvider.revokeNetworkPin();

            getKuick().broadcast();
            activeConnection.setInternalCacheLimit(1073741824); // 1MB
            response = activeConnection.receive().getAsJson();

            handleRequest(activeConnection, device, deviceAddress, hasPin, response);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                CommunicationBridge.sendError(activeConnection, e);
            } catch (Exception ignored) {
            }
        }
    }

    private void handleRequest(ActiveConnection activeConnection, Device device, DeviceAddress deviceAddress,
                               boolean hasPin, JSONObject response) throws JSONException, IOException,
            PersistenceException, CommunicationException
    {
        switch (response.getString(Keyword.REQUEST)) {
            case (Keyword.REQUEST_TRANSFER):
                if (mApp.hasTaskOf(IndexTransferTask.class))
                    throw new NotAllowedException(device);
                else {
                    long transferId = response.getLong(Keyword.TRANSFER_ID);
                    String jsonIndex = response.getString(Keyword.INDEX);

                    try {
                        getKuick().reconstruct(new Transfer(transferId));
                        throw new ContentException(ContentException.Error.AlreadyExists);
                    } catch (PersistenceException e) {
                        CommunicationBridge.sendResult(activeConnection, true);
                        mApp.run(new IndexTransferTask(transferId, jsonIndex, device, hasPin));
                    }
                }
                return;
            case (Keyword.REQUEST_NOTIFY_TRANSFER_STATE): {
                int transferId = response.getInt(Keyword.TRANSFER_ID);
                boolean isAccepted = response.getBoolean(Keyword.TRANSFER_IS_ACCEPTED);
                Transfer transfer = new Transfer(transferId);
                TransferMember member = new TransferMember(transfer, device, TransferItem.Type.OUTGOING);

                getKuick().reconstruct(transfer);
                getKuick().reconstruct(member);

                if (!isAccepted) {
                    getKuick().remove(member);
                    getKuick().broadcast();
                }

                CommunicationBridge.sendResult(activeConnection, true);
                return;
            }
            case (Keyword.REQUEST_CLIPBOARD):
                TextStreamObject textStreamObject = new TextStreamObject(AppUtils.getUniqueNumber(),
                        response.getString(Keyword.TRANSFER_TEXT));

                getKuick().publish(textStreamObject);
                getKuick().broadcast();
                getNotificationHelper().notifyClipboardRequest(device, textStreamObject);

                CommunicationBridge.sendResult(activeConnection, true);
                return;
            case (Keyword.REQUEST_ACQUAINTANCE):
                sendBroadcast(new Intent(ACTION_DEVICE_ACQUAINTANCE)
                        .putExtra(EXTRA_DEVICE, device)
                        .putExtra(EXTRA_DEVICE_ADDRESS, deviceAddress));
                CommunicationBridge.sendResult(activeConnection, true);
                return;
            case (Keyword.REQUEST_TRANSFER_JOB):
                int transferId = response.getInt(Keyword.TRANSFER_ID);
                String typeValue = response.getString(Keyword.TRANSFER_TYPE);
                TransferItem.Type type = TransferItem.Type.valueOf(typeValue);

                // The type is reversed to match our side
                if (TransferItem.Type.INCOMING.equals(type))
                    type = TransferItem.Type.OUTGOING;
                else if (TransferItem.Type.OUTGOING.equals(type))
                    type = TransferItem.Type.INCOMING;

                Transfer transfer = new Transfer(transferId);
                getKuick().reconstruct(transfer);

                Log.d(BackgroundService.TAG, "CommunicationServer.onConnected(): "
                        + "transferId=" + transferId + " typeValue=" + typeValue);

                if (TransferItem.Type.INCOMING.equals(type) && !device.isTrusted)
                    CommunicationBridge.sendError(activeConnection, Keyword.ERROR_NOT_TRUSTED);
                else if (isProcessRunning(transferId, device.uid, type))
                    throw new ContentException(ContentException.Error.NotAccessible);
                else {
                    FileTransferTask task = new FileTransferTask();
                    task.activeConnection = activeConnection;
                    task.transfer = transfer;
                    task.device = device;
                    task.type = type;
                    task.member = new TransferMember(transfer, device, type);
                    task.index = new TransferIndex(transfer);

                    getKuick().reconstruct(task.member);
                    CommunicationBridge.sendResult(activeConnection, true);

                    mApp.attach(task);
                    return;
                }
            default:
                CommunicationBridge.sendResult(activeConnection, false);
        }
    }
}
