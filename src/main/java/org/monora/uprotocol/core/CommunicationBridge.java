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

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;

import static org.monora.uprotocol.core.spec.alpha.Config.PORT_UPROTOCOL;
import static org.monora.uprotocol.core.spec.alpha.Config.TIMEOUT_SOCKET_DEFAULT;

/**
 * created by: Veli
 * date: 11.02.2018 15:07
 */

public class CommunicationBridge implements Closeable
{
    private final PersistenceProvider persistenceProvider;

    private final ActiveConnection activeConnection;

    private final Device device;

    private final DeviceAddress deviceAddress;

    private final boolean isClient;

    /**
     * Create a new instance.
     * <p>
     * This assumes the connection is valid and open. If you need to open a connection, use {@link #connect}.
     *
     * @param persistenceProvider Where the persistent data is stored and queried.
     * @param activeConnection    Represents a valid connection with the said device.
     * @param device              We are connected to.
     * @param deviceAddress       Where the device is located at.
     * @param isClient            Whether the socket instance belongs to the client or server.
     */
    public CommunicationBridge(PersistenceProvider persistenceProvider, ActiveConnection activeConnection,
                               Device device, DeviceAddress deviceAddress, boolean isClient)
    {
        this.persistenceProvider = persistenceProvider;
        this.activeConnection = activeConnection;
        this.device = device;
        this.deviceAddress = deviceAddress;
        this.isClient = isClient;
    }

    /**
     * This class is autocloseable. You can use it in a try-with-resources block.
     * <p>
     * Note that {@link ActiveConnection#closeSafely()} needs another write/read operation to work as intended.
     */
    @Override
    public void close()
    {
        try {
            getActiveConnection().closeSafely();
        } catch (Exception ignored) {
        }
    }

    /**
     * Open a connection using the given {@link DeviceAddress} list.
     * <p>
     * This will try each address one by one until one of them works.
     * <p>
     * If connection opens but the remote rejects the communication request, this will throw that error and will not
     * try rest of the addresses.
     * <p>
     * If all addresses fail, this will still throw an error to simulate what
     * {@link #connect(ConnectionProvider, PersistenceProvider, DeviceAddress, Device, int)} does.
     * <p>
     * The rest of the behavior is the same with
     * {@link #connect(ConnectionProvider, PersistenceProvider, DeviceAddress, Device, int)}.
     *
     * @param connectionProvider  That will handle opening connections when there is a need to bypass firewalls/NAT.
     * @param persistenceProvider To store and query objects.
     * @param addressList         To try.
     * @param device              That we are going to open a connection with. If the connected device is different,
     *                            it will try other connections. If you don't know who you are connecting to, just leave
     *                            this field null.
     * @param pin                 To bypass errors (i.e. you are blocked on the other device), and to be flagged as
     *                            trusted. Pass '0' if you don't have a PIN.
     * @return The communication bridge to communicate with the remote.
     * @throws IOException            If an IO error occurs.
     * @throws JSONException          If something goes wrong when creating JSON object.
     * @throws CommunicationException When there is a communication error due to misconfiguration.
     */
    public static CommunicationBridge connect(ConnectionProvider connectionProvider,
                                              PersistenceProvider persistenceProvider, List<DeviceAddress> addressList,
                                              Device device, int pin) throws JSONException, IOException,
            CommunicationException
    {
        if (addressList.size() < 1)
            throw new IllegalArgumentException("The address list should contain at least one item.");

        for (DeviceAddress address : addressList) {
            try {
                return connect(connectionProvider, persistenceProvider, address, device, pin);
            } catch (IOException | DifferentClientException ignored) {
            }
        }

        throw new SocketException("Failed to connect to the socket address.");
    }

    public void convertToSSL() throws IOException
    {
        Socket socket = activeConnection.getSocket();
        SSLSocketFactory sslSocketFactory = persistenceProvider.getSSLContextFor(device).getSocketFactory();
        SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(socket, socket.getInetAddress().getHostAddress(),
                activeConnection.getSocket().getPort(), true);
        ArrayList<String> supportedCiphers = new ArrayList<>();

        // TODO: 1/6/21 Remove this and allow the implementer to enable these ciphers.
        supportedCiphers.add("TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384");  // API 20+
        supportedCiphers.add("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256");  // API 20+
        supportedCiphers.add("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA");       // API 11+
        sslSocket.setEnabledCipherSuites(supportedCiphers.toArray(new String[0]));

        if (isClient) {
            sslSocket.setUseClientMode(true);
        } else {
            sslSocket.setUseClientMode(false);

            if (device.certificate == null) {
                sslSocket.setWantClientAuth(true);
            } else {
                sslSocket.setNeedClientAuth(true);
            }
        }

        activeConnection.setSocket(sslSocket);

        sslSocket.addHandshakeCompletedListener(event -> {
            try {
                Certificate certificate = event.getPeerCertificates()[0];
            } catch (Exception e) {
                device.certificate = null;
                e.printStackTrace();
            }
        });
        sslSocket.startHandshake();
    }

    /**
     * Open a connection using the given {@link DeviceAddress}.
     * <p>
     * If connection opens but the remote rejects the communication request, this will throw that error.
     *
     * @param connectionProvider  That will handle opening connections when there is a need to bypass firewalls/NAT.
     * @param persistenceProvider To store and query objects.
     * @param deviceAddress       To try.
     * @param device              That we are going to open a connection with. If the connected device is different,
     *                            this will throw error. If you don't know who you are connecting to, just leave
     *                            this field as 'null'.
     * @param pin                 To bypass errors (i.e. you are blocked on the other device), and to be flagged as
     *                            trusted. Pass '0' if you don't have a PIN.
     * @return The communication bridge to communicate with the remote.
     * @throws IOException            If an IO error occurs.
     * @throws JSONException          If something goes wrong when creating JSON object.
     * @throws CommunicationException When there is a communication error due to misconfiguration.
     */
    public static CommunicationBridge connect(ConnectionProvider connectionProvider,
                                              PersistenceProvider persistenceProvider, DeviceAddress deviceAddress,
                                              Device device, int pin)
            throws IOException, JSONException, CommunicationException
    {
        ActiveConnection activeConnection = connectionProvider.openConnection(deviceAddress.inetAddress);
        String remoteDeviceUid = activeConnection.receive().getAsString();

        deviceAddress.deviceUid = remoteDeviceUid;
        persistenceProvider.save(deviceAddress);

        if (device != null && device.uid != null && !device.uid.equals(remoteDeviceUid)) {
            activeConnection.closeSafely();
            throw new DifferentClientException(device, remoteDeviceUid);
        }

        if (device == null)
            device = persistenceProvider.createDeviceFor(remoteDeviceUid);

        try {
            persistenceProvider.sync(device);
        } catch (PersistenceException e) {
            device.senderKey = persistenceProvider.generateKey();
        }

        activeConnection.reply(persistenceProvider.deviceAsJson(device.senderKey, pin));

        DeviceLoader.loadAsClient(persistenceProvider, receiveSecure(activeConnection, device), device);
        receiveResult(activeConnection, device);

        CommunicationBridge bridge = new CommunicationBridge(persistenceProvider, activeConnection, device,
                deviceAddress, true);
        bridge.convertToSSL();
        return bridge;
    }

    /**
     * Returns the active connection instance.
     * <p>
     * Even though this is available to use, the recommend approach is to use the available features.
     *
     * @return The active connection instance.
     */
    public ActiveConnection getActiveConnection()
    {
        return activeConnection;
    }

    /**
     * Returns the device that we are connected to.
     *
     * @return The connected device.
     */
    public Device getDevice()
    {
        return device;
    }

    /**
     * Returns the device address with which we connected to the remote.
     *
     * @return The device address.
     */
    public DeviceAddress getDeviceAddress()
    {
        return deviceAddress;
    }

    /**
     * Get the persistence provider instance that stores the persistent data.
     *
     * @return The persistence provider instance.
     */
    public PersistenceProvider getPersistenceProvider()
    {
        return persistenceProvider;
    }

    /**
     * Open a CoolSocket connection using the default uprotocol port and timeout.
     *
     * @param inetAddress To connect to.
     * @return The object representing a valid connection.
     * @throws IOException If an IO error occurs.
     */
    public static ActiveConnection openConnection(InetAddress inetAddress) throws IOException
    {
        return ActiveConnection.connect(new InetSocketAddress(inetAddress, PORT_UPROTOCOL), TIMEOUT_SOCKET_DEFAULT);
    }

    /**
     * Inform the remote that it should choose you (this client) if it's about to choose a device.
     * <p>
     * For instance, the remote is setting up a file transfer request and is about to pick a device. If you make this
     * request in that timespan, this will invoke {@link TransportSeat#handleAcquaintanceRequest(Device, DeviceAddress)}
     * method on the remote and it will choose you.
     *
     * @return True if successful.
     * @throws IOException            If an IO error occurs.
     * @throws JSONException          If something goes wrong when creating JSON object.
     * @throws CommunicationException When there is a communication error due to misconfiguration.
     */
    public boolean requestAcquaintance() throws JSONException, IOException, CommunicationException
    {
        sendSecure(true, new JSONObject().put(Keyword.REQUEST, Keyword.REQUEST_ACQUAINTANCE));
        return receiveResult();
    }

    /**
     * Request a file transfer operation by informing the remote that you will send files.
     * <p>
     * This request doesn't guarantee that the request will be processed immediately. You should close the connection
     * after making this request. If everything goes right, the remote will reach you using
     * {@link #requestFileTransferStart(long, TransferItem.Type)}, which will end up in your
     * {@link TransportSeat#beginFileTransfer(CommunicationBridge, Device, long, TransferItem.Type)} method.
     * <p>
     * If the initial response is positive, the items will be saved to the persistence provider using
     * {@link PersistenceProvider#save(String, List)}.
     *
     * @param transferId That ties a group of {@link TransferItem} as in {@link TransferItem#transferId}.
     * @param itemList   Items that you will send.
     * @return True if successful.
     * @throws IOException            If an IO error occurs.
     * @throws JSONException          If something goes wrong when creating JSON object.
     * @throws CommunicationException When there is a communication error due to misconfiguration.
     */
    public boolean requestFileTransfer(long transferId, List<TransferItem> itemList) throws JSONException, IOException,
            CommunicationException
    {
        sendSecure(true, new JSONObject()
                .put(Keyword.REQUEST, Keyword.REQUEST_TRANSFER)
                .put(Keyword.TRANSFER_ID, transferId)
                .put(Keyword.INDEX, getPersistenceProvider().toJson(itemList).toString()));

        boolean result = receiveResult();

        if (result)
            getPersistenceProvider().save(getDevice().uid, itemList);

        return result;
    }

    /**
     * Ask remote to start file transfer.
     * <p>
     * The transfer request, in this case, has already been sent with {@link #requestFileTransfer(long, List)}.
     *
     * @param transferId That ties a group of {@link TransferItem} as in {@link TransferItem#transferId}.
     * @param type       Of the transfer as in {@link TransferItem#type}.
     * @return True if successful.
     * @throws IOException            If an IO error occurs.
     * @throws JSONException          If something goes wrong when creating JSON object.
     * @throws CommunicationException When there is a communication error due to misconfiguration.
     */
    public boolean requestFileTransferStart(long transferId, TransferItem.Type type) throws JSONException, IOException,
            CommunicationException
    {
        sendSecure(true, new JSONObject()
                .put(Keyword.REQUEST, Keyword.REQUEST_TRANSFER_JOB)
                .put(Keyword.TRANSFER_ID, transferId)
                .put(Keyword.TRANSFER_TYPE, type));
        return receiveResult();
    }

    /**
     * Inform remote about the state of a transfer request it sent previously.
     *
     * @param transferId The transfer id that you are informing about.
     * @param accepted   True if the transfer request was accepted.
     * @return True if the request was processed successfully.
     * @throws IOException            If an IO error occurs.
     * @throws JSONException          If something goes wrong when creating JSON object.
     * @throws CommunicationException When there is a communication error due to misconfiguration.
     */
    public boolean requestNotifyTransferState(long transferId, boolean accepted) throws JSONException, IOException,
            CommunicationException
    {
        sendSecure(true, new JSONObject()
                .put(Keyword.REQUEST, Keyword.REQUEST_NOTIFY_TRANSFER_STATE)
                .put(Keyword.TRANSFER_ID, transferId)
                .put(Keyword.TRANSFER_IS_ACCEPTED, accepted));
        return receiveResult();
    }

    /**
     * Request a text transfer.
     *
     * @param text To send.
     * @return True if the request was processed successfully.
     * @throws IOException            If an IO error occurs.
     * @throws JSONException          If something goes wrong when creating JSON object.
     * @throws CommunicationException When there is a communication error due to misconfiguration.
     */
    public boolean requestTextTransfer(String text) throws JSONException, IOException, CommunicationException
    {
        sendSecure(true, new JSONObject()
                .put(Keyword.REQUEST, Keyword.REQUEST_TRANSFER_TEXT)
                .put(Keyword.TRANSFER_TEXT, text));
        return receiveResult();
    }

    /**
     * Receive a response from remote and validate it.
     * <p>
     * This will throw the appropriate error when something is not right.
     * <p>
     * The error messages are sent using {@link #sendError}.
     *
     * @param activeConnection The active connection instance.
     * @param device           That we are receiving the response from.
     * @return The JSON data that doesn't seem to contain an error.
     * @throws IOException            If an IO error occurs.
     * @throws JSONException          If something goes wrong when creating JSON object.
     * @throws CommunicationException When there is a communication error due to misconfiguration.
     * @see #receiveSecure()
     * @see #sendError(ActiveConnection, String)
     */
    public static JSONObject receiveSecure(ActiveConnection activeConnection, Device device) throws IOException,
            JSONException, CommunicationException
    {
        JSONObject jsonObject = activeConnection.receive().getAsJson();
        if (jsonObject.has(Keyword.ERROR)) {
            final String errorCode = jsonObject.getString(Keyword.ERROR);
            switch (errorCode) {
                case Keyword.ERROR_NOT_ALLOWED:
                    throw new NotAllowedException(device);
                case Keyword.ERROR_NOT_TRUSTED:
                    throw new NotTrustedException(device);
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

    /**
     * Receive a response from remote and validate it.
     * <p>
     * This will throw the appropriate error when something is not right.
     * <p>
     * The error messages are sent using {@link #sendError}.
     * <p>
     * The active connection defaults to {@link #getActiveConnection()}.
     * <p>
     * The device defaults to {@link #getDevice()}.
     *
     * @return The JSON data that doesn't seem to contain an error.
     * @throws IOException            If an IO error occurs.
     * @throws JSONException          If something goes wrong when creating JSON object.
     * @throws CommunicationException When there is a communication error due to misconfiguration.
     * @see #receiveSecure(ActiveConnection, Device)
     * @see #sendError(ActiveConnection, String)
     */
    public JSONObject receiveSecure() throws IOException, JSONException, CommunicationException
    {
        return receiveSecure(getActiveConnection(), getDevice());
    }

    /**
     * Receive and validate a response. If it doesn't contain an error, get the result.
     *
     * @param activeConnection The active connection instance.
     * @param device           That we are receiving the response from.
     * @return True if the result is positive.
     * @throws IOException            If an IO error occurs.
     * @throws JSONException          If something goes wrong when creating JSON object.
     * @throws CommunicationException When there is a communication error due to misconfiguration.
     * @see #sendResult(ActiveConnection, boolean)
     */
    public static boolean receiveResult(ActiveConnection activeConnection, Device device) throws IOException,
            JSONException, CommunicationException
    {
        return resultOf(receiveSecure(activeConnection, device));
    }

    /**
     * Receive and validate a response. If it doesn't contain an error, get the result.
     * <p>
     * The active connection defaults to {@link #getActiveConnection()}.
     * <p>
     * The device defaults to {@link #getDevice()}.
     *
     * @return True if the result is positive.
     * @throws IOException            If an IO error occurs.
     * @throws JSONException          If something goes wrong when creating JSON object.
     * @throws CommunicationException When there is a communication error due to misconfiguration.
     * @see #sendResult(ActiveConnection, boolean)
     */
    public boolean receiveResult() throws IOException, JSONException, CommunicationException
    {
        return receiveResult(getActiveConnection(), getDevice());
    }

    /**
     * Read the result from a JSON object.
     *
     * @param jsonObject Where to read the result from.
     * @return True if it is positive.
     * @throws JSONException If the JSON data does not contain a result.
     * @see #sendResult(ActiveConnection, boolean)
     */
    public static boolean resultOf(JSONObject jsonObject) throws JSONException
    {
        return jsonObject.getBoolean(Keyword.RESULT);
    }

    /**
     * Send an error to remote.
     *
     * @param activeConnection The active connection instance.
     * @param exception        With which this will decide which error code to send.
     * @throws IOException                     If an IO error occurs.
     * @throws JSONException                   If something goes wrong when creating JSON object.
     * @throws UnhandledCommunicationException If the exception is not known.
     * @see #receiveSecure(ActiveConnection, Device)
     */
    public static void sendError(ActiveConnection activeConnection, Exception exception) throws IOException,
            JSONException, UnhandledCommunicationException
    {
        try {
            throw exception;
        } catch (NotTrustedException e) {
            sendError(activeConnection, Keyword.ERROR_NOT_TRUSTED);
        } catch (DeviceBlockedException | DeviceVerificationException e) {
            sendError(activeConnection, Keyword.ERROR_NOT_ALLOWED);
        } catch (PersistenceException e) {
            sendError(activeConnection, Keyword.ERROR_NOT_FOUND);
        } catch (ContentException e) {
            switch (e.error) {
                case NotFound:
                    sendError(activeConnection, Keyword.ERROR_NOT_FOUND);
                    break;
                case NotAccessible:
                    sendError(activeConnection, Keyword.ERROR_NOT_ACCESSIBLE);
                    break;
                case AlreadyExists:
                    sendError(activeConnection, Keyword.ERROR_ALREADY_EXISTS);
                    break;
                default:
                    sendError(activeConnection, Keyword.ERROR_UNKNOWN);
            }
        } catch (Exception e) {
            throw new UnhandledCommunicationException("An unknown error was thrown during the communication", e);
        }
    }

    /**
     * Send an error to remote.
     *
     * @param activeConnection The active connection instance.
     * @param errorCode        To send.
     * @throws IOException   If an IO error occurs.
     * @throws JSONException If something goes wrong when creating JSON object.
     * @see #receiveSecure(ActiveConnection, Device)
     */
    public static void sendError(ActiveConnection activeConnection, String errorCode) throws IOException, JSONException
    {
        sendSecure(activeConnection, false, new JSONObject().put(Keyword.ERROR, errorCode));
    }

    /**
     * Send error to remote.
     * <p>
     * The active connection defaults to {@link #getActiveConnection()}.
     *
     * @param exception With which this will decide which error code to send.
     * @throws IOException                     If an IO error occurs.
     * @throws JSONException                   If something goes wrong when creating JSON object.
     * @throws UnhandledCommunicationException If the exception is not known.
     * @see #receiveSecure(ActiveConnection, Device)
     */
    public void sendError(Exception exception) throws IOException, JSONException, UnhandledCommunicationException
    {
        sendError(getActiveConnection(), exception);
    }

    /**
     * Send an error to remote.
     * <p>
     * The active connection defaults to {@link #getActiveConnection()}.
     *
     * @param errorCode To send.
     * @throws IOException   If an IO error occurs.
     * @throws JSONException If something goes wrong when creating JSON object.
     * @see #receiveSecure(ActiveConnection, Device)
     */
    public void sendError(String errorCode) throws IOException, JSONException
    {
        sendError(getActiveConnection(), errorCode);
    }

    /**
     * Send a result to remote.
     *
     * @param activeConnection The active connection instance.
     * @param result           True if positive.
     * @throws IOException   If an IO error occurs.
     * @throws JSONException If something goes wrong when creating JSON object.
     * @see #receiveResult(ActiveConnection, Device)
     */
    public static void sendResult(ActiveConnection activeConnection, boolean result) throws IOException, JSONException
    {
        sendSecure(activeConnection, result, new JSONObject());
    }

    /**
     * Send a result to remote.
     * <p>
     * The active connection defaults to {@link #getActiveConnection()}.
     *
     * @param result True if positive.
     * @throws IOException   If an IO error occurs.
     * @throws JSONException If something goes wrong when creating JSON object.
     * @see #receiveResult(ActiveConnection, Device)
     */
    public void sendResult(boolean result) throws IOException, JSONException
    {
        sendResult(getActiveConnection(), result);
    }

    /**
     * Send a JSON data that includes the result.
     *
     * @param activeConnection The active connection instance.
     * @param result           If the result is successful.
     * @param jsonObject       To send along with the result.
     * @throws IOException   If an IO error occurs.
     * @throws JSONException If something goes wrong when creating JSON object.
     * @see #receiveResult(ActiveConnection, Device)
     */
    public static void sendSecure(ActiveConnection activeConnection, boolean result, JSONObject jsonObject)
            throws JSONException, IOException
    {
        activeConnection.reply(jsonObject.put(Keyword.RESULT, result));
    }

    /**
     * Send a JSON data that includes the result.
     * <p>
     * The active connection defaults to {@link #getActiveConnection()}.
     *
     * @param result     If the result is successful.
     * @param jsonObject To send along with the result.
     * @throws IOException   If an IO error occurs.
     * @throws JSONException If something goes wrong when creating JSON object.
     * @see #receiveResult(ActiveConnection, Device)
     */
    public void sendSecure(boolean result, JSONObject jsonObject) throws JSONException, IOException
    {
        sendSecure(getActiveConnection(), result, jsonObject);
    }
}