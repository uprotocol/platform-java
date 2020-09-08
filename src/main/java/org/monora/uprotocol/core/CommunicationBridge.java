/*
 * Copyright (C) 2019 Veli Tasalı
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.monora.uprotocol.core;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.monora.coolsocket.core.session.ActiveConnection;
import org.monora.uprotocol.core.network.Device;
import org.monora.uprotocol.core.network.DeviceAddress;
import org.monora.uprotocol.core.network.TransferItem;
import org.monora.uprotocol.core.persistence.PersistenceException;
import org.monora.uprotocol.core.persistence.PersistenceProvider;
import org.monora.uprotocol.core.protocol.ConnectionProvider;
import org.monora.uprotocol.core.protocol.DeviceBlockedException;
import org.monora.uprotocol.core.protocol.DeviceVerificationException;
import org.monora.uprotocol.core.protocol.communication.*;
import org.monora.uprotocol.core.spec.alpha.Keyword;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.List;

/**
 * created by: Veli
 * date: 11.02.2018 15:07
 */

public class CommunicationBridge implements Closeable
{
    public static final String TAG = CommunicationBridge.class.getSimpleName();

    public static final int TIMEOUT_SOCKET_DEFAULT = 5000;

    public static final int PORT_UPROTOCOL = 1128;

    public static final int LENGTH_DEVICE_NAME = 32;


    private final PersistenceProvider persistenceProvider;

    private final ActiveConnection activeConnection;

    private final Device device;

    private final DeviceAddress deviceAddress;

    public CommunicationBridge(PersistenceProvider persistenceProvider, ActiveConnection activeConnection,
                               Device device, DeviceAddress deviceAddress)
    {
        this.persistenceProvider = persistenceProvider;
        this.activeConnection = activeConnection;
        this.device = device;
        this.deviceAddress = deviceAddress;
    }

    @Override
    public void close()
    {
        try {
            getActiveConnection().closeSafely();
        } catch (Exception ignored) {
        }
    }

    public static CommunicationBridge connect(ConnectionProvider connectionProvider,
                                              PersistenceProvider persistenceProvider, List<DeviceAddress> addressList,
                                              Device device, int pin)
            throws IOException, CommunicationException, JSONException
    {
        for (DeviceAddress address : addressList) {
            try {
                return connect(connectionProvider, persistenceProvider, address, device, pin);
            } catch (IOException ignored) {
            }
        }

        throw new SocketException("Failed to connect to the socket address.");
    }

    public static CommunicationBridge connect(ConnectionProvider connectionProvider,
                                              PersistenceProvider persistenceProvider, DeviceAddress deviceAddress,
                                              Device device, int pin)
            throws IOException, JSONException, CommunicationException
    {
        ActiveConnection activeConnection = connectionProvider.openConnection(deviceAddress.inetAddress);
        String remoteDeviceId = activeConnection.receive().getAsString();

        if (device != null && device.uid != null && !device.uid.equals(remoteDeviceId)) {
            activeConnection.closeSafely();
            throw new DifferentClientException(device, remoteDeviceId);
        }

        if (device == null)
            device = persistenceProvider.createDeviceFor(remoteDeviceId);

        try {
            persistenceProvider.sync(device);
        } catch (PersistenceException e) {
            device.sendKey = persistenceProvider.generateKey();
        }

        activeConnection.reply(PersistenceProvider.toJson(persistenceProvider, device.sendKey, pin));
        persistenceProvider.save(device, deviceAddress);
        DeviceLoader.loadAsClient(persistenceProvider, receiveSecure(activeConnection, device), device);
        CommunicationBridge.receiveResult(activeConnection, device);

        return new CommunicationBridge(persistenceProvider, activeConnection, device, deviceAddress);
    }

    public ActiveConnection getActiveConnection()
    {
        return activeConnection;
    }

    public Device getDevice()
    {
        return device;
    }

    public DeviceAddress getDeviceAddress()
    {
        return deviceAddress;
    }

    public PersistenceProvider getPersistenceProvider()
    {
        return persistenceProvider;
    }

    public static ActiveConnection openConnection(InetAddress inetAddress) throws IOException
    {
        return ActiveConnection.connect(new InetSocketAddress(inetAddress, PORT_UPROTOCOL), TIMEOUT_SOCKET_DEFAULT);
    }

    public void requestAcquaintance() throws JSONException, IOException
    {
        getActiveConnection().reply(new JSONObject().put(Keyword.REQUEST, Keyword.REQUEST_ACQUAINTANCE));
    }

    public void requestFileTransfer(long transferId, JSONArray files) throws JSONException, IOException
    {
        getActiveConnection().reply(new JSONObject()
                .put(Keyword.REQUEST, Keyword.REQUEST_TRANSFER)
                .put(Keyword.TRANSFER_ID, transferId)
                .put(Keyword.INDEX, files));
    }

    public void requestFileTransferStart(long transferId, TransferItem.Type type) throws JSONException, IOException
    {
        getActiveConnection().reply(new JSONObject()
                .put(Keyword.REQUEST, Keyword.REQUEST_TRANSFER_JOB)
                .put(Keyword.TRANSFER_ID, transferId)
                .put(Keyword.TRANSFER_TYPE, type));
    }

    public void requestNotifyTransferState(long transferId, boolean accepted) throws JSONException, IOException
    {
        getActiveConnection().reply(new JSONObject()
                .put(Keyword.REQUEST, Keyword.REQUEST_NOTIFY_TRANSFER_STATE)
                .put(Keyword.TRANSFER_ID, transferId)
                .put(Keyword.TRANSFER_IS_ACCEPTED, accepted));
    }

    public void requestTextTransfer(String text) throws JSONException, IOException
    {
        getActiveConnection().reply(new JSONObject()
                .put(Keyword.REQUEST, Keyword.REQUEST_CLIPBOARD)
                .put(Keyword.TRANSFER_TEXT, text));
    }

    public boolean receiveResult() throws JSONException, IOException, CommunicationException
    {
        return receiveResult(getActiveConnection(), getDevice());
    }

    public static JSONObject receiveSecure(ActiveConnection connection, Device targetDevice) throws IOException,
            JSONException, CommunicationException
    {
        JSONObject jsonObject = connection.receive().getAsJson();
        if (jsonObject.has(Keyword.ERROR)) {
            final String errorCode = jsonObject.getString(Keyword.ERROR);
            switch (errorCode) {
                case Keyword.ERROR_NOT_ALLOWED:
                    throw new NotAllowedException(targetDevice);
                case Keyword.ERROR_NOT_TRUSTED:
                    throw new NotTrustedException(targetDevice);
                case Keyword.ERROR_NOT_ACCESSIBLE:
                    throw new ContentException(ContentException.Error.NotAccessible);
                case Keyword.ERROR_ALREADY_EXISTS:
                    throw new ContentException(ContentException.Error.AlreadyExists);
                case Keyword.ERROR_NOT_FOUND:
                    throw new ContentException(ContentException.Error.NotFound);
                case Keyword.ERROR_UNKNOWN:
                    throw new CommunicationException();
                default:
                    throw new UnknownCommunicationErrorException(errorCode);
            }
        }
        return jsonObject;
    }

    public static boolean receiveResult(ActiveConnection connection, Device targetDevice) throws IOException,
            JSONException, CommunicationException
    {
        return receiveSecure(connection, targetDevice).getBoolean(Keyword.RESULT);
    }

    public static void sendError(ActiveConnection connection, Exception exception) throws IOException, JSONException,
            UnhandledCommunicationException
    {
        try {
            throw exception;
        } catch (NotTrustedException e) {
            CommunicationBridge.sendError(connection, Keyword.ERROR_NOT_TRUSTED);
        } catch (DeviceBlockedException | DeviceVerificationException e) {
            CommunicationBridge.sendError(connection, Keyword.ERROR_NOT_ALLOWED);
        } catch (PersistenceException e) {
            CommunicationBridge.sendError(connection, Keyword.ERROR_NOT_FOUND);
        } catch (ContentException e) {
            CommunicationBridge.sendError(connection, e);
        } catch (Exception e) {
            throw new UnhandledCommunicationException("An unknown error was thrown during the communication", e);
        }
    }

    public static void sendError(ActiveConnection connection, ContentException e) throws IOException, JSONException
    {
        switch (e.error) {
            case NotFound:
                CommunicationBridge.sendError(connection, Keyword.ERROR_NOT_FOUND);
                break;
            case NotAccessible:
                CommunicationBridge.sendError(connection, Keyword.ERROR_NOT_ACCESSIBLE);
                break;
            case AlreadyExists:
                CommunicationBridge.sendError(connection, Keyword.ERROR_ALREADY_EXISTS);
                break;
            default:
                CommunicationBridge.sendError(connection, Keyword.ERROR_UNKNOWN);
        }
    }

    public void sendError(String errorCode) throws IOException, JSONException
    {
        sendError(getActiveConnection(), errorCode);
    }

    public static void sendError(ActiveConnection connection, String errorCode) throws IOException, JSONException
    {
        connection.reply(new JSONObject().put(Keyword.ERROR, errorCode));
    }

    public void sendResult(boolean result) throws IOException, JSONException
    {
        sendResult(getActiveConnection(), result);
    }

    public static void sendResult(ActiveConnection connection, boolean result) throws IOException, JSONException
    {
        sendSecure(connection, result, new JSONObject());
    }

    public static void sendSecure(ActiveConnection connection, boolean result, JSONObject jsonObject)
            throws JSONException, IOException
    {
        connection.reply(jsonObject.put(Keyword.RESULT, result));
    }
}