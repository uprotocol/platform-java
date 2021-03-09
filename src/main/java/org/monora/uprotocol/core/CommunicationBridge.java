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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import org.monora.coolsocket.core.session.ActiveConnection;
import org.monora.uprotocol.core.persistence.PersistenceProvider;
import org.monora.uprotocol.core.protocol.Client;
import org.monora.uprotocol.core.protocol.ClientAddress;
import org.monora.uprotocol.core.protocol.ConnectionFactory;
import org.monora.uprotocol.core.protocol.communication.CommunicationException;
import org.monora.uprotocol.core.protocol.communication.ProtocolException;
import org.monora.uprotocol.core.protocol.communication.SecurityException;
import org.monora.uprotocol.core.protocol.communication.client.BlockedRemoteClientException;
import org.monora.uprotocol.core.protocol.communication.client.DifferentRemoteClientException;
import org.monora.uprotocol.core.spec.v1.Keyword;
import org.monora.uprotocol.core.transfer.TransferItem;
import org.monora.uprotocol.core.transfer.Transfers;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import static org.monora.uprotocol.core.spec.v1.Config.PORT_UPROTOCOL;
import static org.monora.uprotocol.core.spec.v1.Config.TIMEOUT_SOCKET_DEFAULT;

public class CommunicationBridge implements Closeable
{
    private final PersistenceProvider persistenceProvider;

    private final ActiveConnection activeConnection;

    private final Client client;

    private final ClientAddress clientAddress;

    /**
     * Create a new instance.
     * <p>
     * This assumes the connection is valid and open. If you need to open a connection, use {@link #connect}.
     *
     * @param persistenceProvider Where the persistent data is stored and queried.
     * @param activeConnection    Represents a valid connection to the remote client.
     * @param client              The remote that this client has connected to.
     * @param clientAddress       Where the remote client resides on the network.
     */
    public CommunicationBridge(@NotNull PersistenceProvider persistenceProvider,
                               @NotNull ActiveConnection activeConnection, @NotNull Client client,
                               @NotNull ClientAddress clientAddress)
    {
        this.persistenceProvider = persistenceProvider;
        this.activeConnection = activeConnection;
        this.client = client;
        this.clientAddress = clientAddress;
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
     * Open a connection with a remote using the default builder.
     *
     * @param connectionFactory   To start and set up connections with.
     * @param persistenceProvider To store and query objects with.
     * @param inetAddress         To connect to.
     * @return The communication bridge to communicate with the remote.
     * @throws IOException                    If an IO error occurs.
     * @throws JSONException                  If something goes wrong when creating JSON object.
     * @throws ProtocolException              When there is a communication error due to misconfiguration.
     * @throws SecurityException              If something goes wrong while establishing a secure connection.
     * @throws DifferentRemoteClientException If the connected client is different from the one that was provided
     * @throws CertificateException           If an error related to encryption or authentication occurs.
     * @see Builder#connect
     */
    public static @NotNull CommunicationBridge connect(@NotNull ConnectionFactory connectionFactory,
                                                       @NotNull PersistenceProvider persistenceProvider,
                                                       @NotNull InetAddress inetAddress)
            throws IOException, JSONException, ProtocolException, CertificateException
    {
        return new Builder(connectionFactory, persistenceProvider, inetAddress).connect();
    }

    static void convertToSSL(@NotNull ConnectionFactory connectionFactory,
                             @NotNull PersistenceProvider persistenceProvider,
                             @NotNull ActiveConnection activeConnection, @NotNull Client client, boolean isClient)
            throws IOException, CommunicationException, CertificateException
    {
        Socket socket = activeConnection.getSocket();
        SSLSocketFactory sslSocketFactory = persistenceProvider.getSSLContextFor(client).getSocketFactory();
        SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(socket, socket.getInetAddress().getHostAddress(),
                activeConnection.getSocket().getPort(), true);
        ArrayList<String> enabledCiphersSuites = new ArrayList<>();

        enabledCiphersSuites.add("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA");
        connectionFactory.enableCipherSuites(sslSocket.getSupportedCipherSuites(), enabledCiphersSuites);
        sslSocket.setEnabledCipherSuites(enabledCiphersSuites.toArray(new String[0]));

        if (isClient) {
            sslSocket.setUseClientMode(true);
        } else {
            sslSocket.setUseClientMode(false);

            if (client.getClientCertificate() == null) {
                sslSocket.setWantClientAuth(true);
            } else {
                sslSocket.setNeedClientAuth(true);
            }
        }

        sslSocket.addHandshakeCompletedListener(event -> {
            try {
                Certificate certificate = event.getPeerCertificates()[0];

                if (certificate instanceof X509Certificate) {
                    if (!certificate.equals(client.getClientCertificate())) {
                        client.setClientCertificate((X509Certificate) certificate);
                        persistenceProvider.persist(client, true);
                    }
                } else {
                    throw new CertificateException("The certificate is not in X.509 format");
                }
            } catch (Exception e) {
                client.setClientCertificate(null);
                persistenceProvider.persist(client, true);
                e.printStackTrace();
            }
        });

        activeConnection.setSocket(sslSocket);

        try {
            sslSocket.startHandshake();
        } catch (Exception e) {
            throw new SecurityException(client, e);
        }
    }

    /**
     * Returns the active connection instance.
     * <p>
     * Even though this is available to use, the recommended approach is to use the available features.
     *
     * @return The active connection instance.
     */
    public @NotNull ActiveConnection getActiveConnection()
    {
        return activeConnection;
    }

    /**
     * Get the persistence provider instance that stores the persistent data.
     *
     * @return The persistence provider instance.
     */
    public @NotNull PersistenceProvider getPersistenceProvider()
    {
        return persistenceProvider;
    }

    /**
     * Returns the client that the bridge is connected to.
     *
     * @return The connected remote client.
     */
    public @NotNull Client getRemoteClient()
    {
        return client;
    }

    /**
     * Returns the address where the connected remote client resides.
     *
     * @return The client address.
     */
    public @NotNull ClientAddress getRemoteClientAddress()
    {
        return clientAddress;
    }

    /**
     * Open a CoolSocket connection using the default uprotocol port and timeout.
     *
     * @param inetAddress To connect to.
     * @return The object representing a valid connection.
     * @throws IOException If an IO error occurs.
     */
    public static @NotNull ActiveConnection openConnection(@NotNull InetAddress inetAddress) throws IOException
    {
        return ActiveConnection.connect(new InetSocketAddress(inetAddress, PORT_UPROTOCOL), TIMEOUT_SOCKET_DEFAULT);
    }

    /**
     * Inform the remote that it should choose you (this client) if it's about to choose one.
     * <p>
     * For instance, the remote is setting up a file transfer request and is about to pick a client. If you make this
     * request in that timespan, this will invoke {@link TransportSeat#handleAcquaintanceRequest(Client, ClientAddress)}
     * method on the remote and it will choose you.
     *
     * @return True if successful.
     * @throws IOException       If an IO error occurs.
     * @throws JSONException     If something goes wrong when creating JSON object.
     * @throws ProtocolException When there is a communication error due to misconfiguration.
     */
    public boolean requestAcquaintance() throws JSONException, IOException, ProtocolException
    {
        send(true, new JSONObject().put(Keyword.REQUEST, Keyword.REQUEST_ACQUAINTANCE));
        return receiveResult();
    }

    /**
     * Request a file transfer operation by informing the remote that you will send files.
     * <p>
     * This request doesn't guarantee that the request will be processed immediately. You should close the connection
     * after making this request. If everything goes right, the remote will reach you using
     * {@link #requestFileTransferStart(long, TransferItem.Type)}, which will end up in your
     * {@link TransportSeat#beginFileTransfer(CommunicationBridge, Client, long, TransferItem.Type)} method.
     * <p>
     * If the initial response is positive, the items will be saved to the persistence provider using
     * {@link PersistenceProvider#persist(String, List)}.
     *
     * @param groupId          That ties a group of {@link TransferItem} as in {@link TransferItem#getItemGroupId()}.
     * @param transferItemList That you will send.
     * @return True if successful.
     * @throws IOException       If an IO error occurs.
     * @throws JSONException     If something goes wrong when creating JSON object.
     * @throws ProtocolException When there is a communication error due to misconfiguration.
     */
    public boolean requestFileTransfer(long groupId, @NotNull List<@NotNull TransferItem> transferItemList)
            throws JSONException, IOException, ProtocolException
    {
        send(true, new JSONObject()
                .put(Keyword.REQUEST, Keyword.REQUEST_TRANSFER)
                .put(Keyword.TRANSFER_GROUP_ID, groupId)
                .put(Keyword.INDEX, getPersistenceProvider().toJson(transferItemList).toString()));

        boolean result = receiveResult();

        if (result) {
            getPersistenceProvider().persist(getRemoteClient().getClientUid(), transferItemList);
        }

        return result;
    }

    /**
     * Ask remote to start file transfer.
     * <p>
     * The transfer request, in this case, has already been sent with {@link #requestFileTransfer(long, List)}.
     *
     * After the method returns positive, the rest of the operation can be carried on with {@link Transfers#receive}
     * or {@link Transfers#send} depending on the type of the transfer.
     *
     * @param groupId That ties a group of {@link TransferItem} as in {@link TransferItem#getItemGroupId()}.
     * @param type    Of the transfer as in {@link TransferItem#getItemType()}.
     * @return True if successful.
     * @throws IOException       If an IO error occurs.
     * @throws JSONException     If something goes wrong when creating JSON object.
     * @throws ProtocolException When there is a communication error due to misconfiguration.
     * @see Transfers#receive
     * @see Transfers#send
     */
    public boolean requestFileTransferStart(long groupId, @NotNull TransferItem.Type type) throws JSONException,
            IOException, ProtocolException
    {
        send(true, new JSONObject()
                .put(Keyword.REQUEST, Keyword.REQUEST_TRANSFER_JOB)
                .put(Keyword.TRANSFER_GROUP_ID, groupId)
                .put(Keyword.TRANSFER_TYPE, type.protocolValue));
        return receiveResult();
    }

    /**
     * Inform remote about the state of a transfer request it sent previously.
     *
     * @param groupId  The transfer id that you are informing about.
     * @param accepted True if the transfer request was accepted.
     * @return True if the request was processed successfully.
     * @throws IOException       If an IO error occurs.
     * @throws JSONException     If something goes wro when creating JSON object.
     * @throws ProtocolException When there is a communication error due to misconfiguration.
     */
    public boolean requestNotifyTransferState(long groupId, boolean accepted) throws JSONException, IOException,
            ProtocolException
    {
        send(true, new JSONObject()
                .put(Keyword.REQUEST, Keyword.REQUEST_NOTIFY_TRANSFER_STATE)
                .put(Keyword.TRANSFER_GROUP_ID, groupId)
                .put(Keyword.TRANSFER_IS_ACCEPTED, accepted));
        return receiveResult();
    }

    /**
     * Request a text transfer.
     *
     * @param text To send.
     * @return True if the request was processed successfully.
     * @throws IOException       If an IO error occurs.
     * @throws JSONException     If something goes wrong when creating JSON object.
     * @throws ProtocolException When there is a communication error due to misconfiguration.
     */
    public boolean requestTextTransfer(@NotNull String text) throws JSONException, IOException, ProtocolException
    {
        send(true, new JSONObject()
                .put(Keyword.REQUEST, Keyword.REQUEST_TRANSFER_TEXT)
                .put(Keyword.TRANSFER_TEXT, text));
        return receiveResult();
    }

    /**
     * Receive a response from remote and validate it.
     * <p>
     * This will throw the appropriate error when something is not right.
     * <p>
     * The active connection defaults to {@link #getActiveConnection()}.
     * <p>
     * The client defaults to {@link #getRemoteClient()}.
     *
     * @return The JSON data that doesn't seem to contain an error.
     * @throws IOException       If an IO error occurs.
     * @throws JSONException     If something goes wrong when creating JSON object.
     * @throws ProtocolException When there is a communication error due to misconfiguration.
     */
    public @NotNull JSONObject receiveChecked() throws IOException, JSONException, ProtocolException
    {
        return Responses.receiveChecked(getActiveConnection(), getRemoteClient());
    }

    /**
     * Receive and validate a response. If it doesn't contain an error, get the result.
     * <p>
     * The active connection defaults to {@link #getActiveConnection()}.
     * <p>
     * The client defaults to {@link #getRemoteClient()}.
     *
     * @return True if the result is positive.
     * @throws IOException       If an IO error occurs.
     * @throws JSONException     If something goes wrong when creating JSON object.
     * @throws ProtocolException When there is a communication error due to misconfiguration.
     */
    public boolean receiveResult() throws IOException, JSONException, ProtocolException
    {
        return Responses.receiveResult(getActiveConnection(), getRemoteClient());
    }

    /**
     * Send a JSON data that includes the result.
     * <p>
     * The active connection defaults to {@link #getActiveConnection()}.
     *
     * @param result     Of the operation to send.
     * @param jsonObject To send along with the result.
     * @throws IOException   If an IO error occurs.
     * @throws JSONException If something goes wrong when creating JSON object.
     */
    public void send(boolean result, @NotNull JSONObject jsonObject) throws JSONException, IOException
    {
        Responses.send(getActiveConnection(), result, jsonObject);
    }

    /**
     * Send a JSON data that includes the result.
     * <p>
     * The active connection defaults to {@link #getActiveConnection()}.
     *
     * @param result Of the operation to send.
     * @throws IOException   If an IO error occurs.
     * @throws JSONException If something goes wrong when creating JSON object.
     */
    public void send(boolean result) throws JSONException, IOException
    {
        send(result, new JSONObject());
    }

    /**
     * Send error to remote.
     * <p>
     * The active connection defaults to {@link #getActiveConnection()}.
     *
     * @param exception  With which this will decide which error code to send.
     * @param jsonObject To send along with the error.
     * @throws IOException       If an IO error occurs.
     * @throws JSONException     If something goes wrong when creating JSON object.
     * @throws ProtocolException With the cause exception if the error is not known.
     */
    public void send(@NotNull Exception exception, @NotNull JSONObject jsonObject) throws IOException, JSONException,
            ProtocolException
    {
        Responses.send(getActiveConnection(), exception, jsonObject);
    }

    /**
     * Send error to remote.
     * <p>
     * The active connection defaults to {@link #getActiveConnection()}.
     * <p>
     * The JSON object defaults to a new instance.
     *
     * @param exception With which this will decide which error code to send.
     * @throws IOException       If an IO error occurs.
     * @throws JSONException     If something goes wrong when creating JSON object.
     * @throws ProtocolException With the cause exception if the error is not known.
     */
    public void send(@NotNull Exception exception) throws IOException, JSONException, ProtocolException
    {
        send(exception, new JSONObject());
    }

    /**
     * Send error to remote.
     * <p>
     * The active connection defaults to {@link #getActiveConnection()}.
     *
     * @param errorCode  To send.
     * @param jsonObject To send along with the error.
     * @throws IOException   If an IO error occurs.
     * @throws JSONException If something goes wrong when creating JSON object.
     */
    public void send(@NotNull String errorCode, @NotNull JSONObject jsonObject) throws IOException, JSONException
    {
        Responses.send(getActiveConnection(), errorCode, jsonObject);
    }

    /**
     * Send error to remote.
     * <p>
     * The active connection defaults to {@link #getActiveConnection()}.
     * <p>
     * The JSON object defaults to a new instance.
     *
     * @param errorCode To send.
     * @throws IOException   If an IO error occurs.
     * @throws JSONException If something goes wrong when creating JSON object.
     */
    public void send(@NotNull String errorCode) throws IOException, JSONException
    {
        send(errorCode, new JSONObject());
    }

    /**
     * Build a bridge that connects to a remote.
     * <p>
     * This keeps optional arguments in separate methods.
     *
     * @see Connections#shouldTryAnotherConnection(Exception)
     * @see CommunicationBridge#connect
     */
    public static class Builder
    {
        private final @NotNull ConnectionFactory connectionFactory;

        private final @NotNull PersistenceProvider persistenceProvider;

        private final @NotNull InetAddress inetAddress;

        private @Nullable String clientUid;

        private boolean clearBlockedStatus;

        private int pin;

        /**
         * Creates a new builder instance.
         *
         * @param connectionFactory   To start and set up connections with.
         * @param persistenceProvider To store and query objects with.
         * @param inetAddress         To connect to.
         */
        public Builder(@NotNull ConnectionFactory connectionFactory,
                       @NotNull PersistenceProvider persistenceProvider,
                       @NotNull InetAddress inetAddress)
        {
            this.connectionFactory = connectionFactory;
            this.persistenceProvider = persistenceProvider;
            this.inetAddress = inetAddress;
            this.clearBlockedStatus = true;
        }

        /**
         * Open the connection with the remote.
         *
         * @return The communication bridge to communicate with the remote.
         * @throws IOException                    If an IO error occurs.
         * @throws ProtocolException              When there is a communication error due to misconfiguration.
         * @throws SecurityException              If something goes wrong while establishing a secure connection.
         * @throws DifferentRemoteClientException If the connected client is different from the one that was provided.
         * @throws BlockedRemoteClientException   If the remote is blocked on the side and unblocking is disallowed.
         * @throws CertificateException           If an error related to encryption or authentication occurs.
         */
        public CommunicationBridge connect() throws IOException, JSONException, ProtocolException, CertificateException
        {
            ActiveConnection activeConnection = connectionFactory.openConnection(inetAddress);
            String remoteClientUid = activeConnection.receive().getAsString();

            if (clientUid != null && !clientUid.equals(remoteClientUid)) {
                activeConnection.closeSafely();
                throw new DifferentRemoteClientException(clientUid, remoteClientUid);
            }

            activeConnection.reply(persistenceProvider.clientAsJson(pin));
            JSONObject jsonObject = activeConnection.receive().getAsJson();
            Client client = ClientLoader.loadAsClient(persistenceProvider, jsonObject, remoteClientUid,
                    clearBlockedStatus);
            ClientAddress clientAddress = persistenceProvider.createClientAddressFor(inetAddress, remoteClientUid);

            persistenceProvider.persist(clientAddress);

            Responses.checkError(client, jsonObject);
            convertToSSL(connectionFactory, persistenceProvider, activeConnection, client, true);

            return new CommunicationBridge(persistenceProvider, activeConnection, client, clientAddress);
        }

        /**
         * Sets whether the initial communication process should fail when the remote is flagged as blocked on this
         * side.
         * <p>
         * Setting this to true will clear the blocked status of the remote.
         *
         * @param clear True to clear the blocked status of the remote instead of failing.
         * @see Client#isClientBlocked()
         */
        public void setClearBlockedStatus(boolean clear)
        {
            this.clearBlockedStatus = clear;
        }

        /**
         * Sets the client UID that this should connect to.
         * <p>
         * Passing null means the remote is not yet known. While not null, if the remote UID differs, this will cause
         * {@link DifferentRemoteClientException}.
         *
         * @param clientUid With which a connection will be established.
         * @see Client#getClientUid()
         */
        public void setClientUid(@Nullable String clientUid)
        {
            this.clientUid = clientUid;
        }

        /**
         * Sets the PIN to bypass errors (i.e. this client is blocked on the remote client), and to be flagged
         * as trusted. Pass '0' if no PIN is available.
         *
         * @param pin The PIN remote gave you through 3rd party means.
         */
        public void setPin(int pin)
        {
            this.pin = pin;
        }
    }
}