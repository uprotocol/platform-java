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
import org.monora.coolsocket.core.session.CancelledException;
import org.monora.coolsocket.core.session.ClosedException;
import org.monora.uprotocol.core.io.DefectiveAddressListException;
import org.monora.uprotocol.core.persistence.OnPrepareListener;
import org.monora.uprotocol.core.persistence.PersistenceProvider;
import org.monora.uprotocol.core.protocol.*;
import org.monora.uprotocol.core.protocol.communication.CredentialsException;
import org.monora.uprotocol.core.protocol.communication.GuidanceResult;
import org.monora.uprotocol.core.protocol.communication.ProtocolException;
import org.monora.uprotocol.core.protocol.communication.SecurityException;
import org.monora.uprotocol.core.protocol.communication.client.BlockedRemoteClientException;
import org.monora.uprotocol.core.protocol.communication.client.DifferentRemoteClientException;
import org.monora.uprotocol.core.spec.v1.Keyword;
import org.monora.uprotocol.core.transfer.TransferItem;
import org.monora.uprotocol.core.transfer.Transfers;

import javax.net.ssl.SSLException;
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
import java.util.Collections;
import java.util.List;

import static org.monora.uprotocol.core.spec.v1.Config.PORT_UPROTOCOL;
import static org.monora.uprotocol.core.spec.v1.Config.TIMEOUT_SOCKET_DEFAULT;

public class CommunicationBridge implements Closeable
{
    private final @NotNull PersistenceProvider persistenceProvider;

    private final @NotNull ActiveConnection activeConnection;

    private final @NotNull Client client;

    private final @NotNull ClientAddress clientAddress;

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
    public void close() throws IOException
    {
        getActiveConnection().close();
    }

    /**
     * Close a connection with mutual agreement.
     * <p>
     * Difference between this and {@link #close()} is that this will also inform the remote that connection
     * will be closed and both sides will throw a {@link ClosedException} as soon as that happens.
     * <p>
     * This can be useful when you want to close the connection outside side agreement of points (where you exchange
     * results), i.e., you want to close the connection out of blue.
     * <p>
     * Note that this needs another write/read operation to work as intended.
     *
     * @throws IOException If an IO error occurs.
     */
    public void closeSafely() throws IOException
    {
        getActiveConnection().closeSafely();
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
            throws IOException, ProtocolException, CertificateException
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
        } catch (SSLException e) {
            boolean firstTime = !persistenceProvider.hasRequestForInvalidationOfCredentials(client.getClientUid());
            if (firstTime) {
                persistenceProvider.saveRequestForInvalidationOfCredentials(client.getClientUid());
            }

            throw new CredentialsException(client, e, firstTime);
        } catch (Exception e) {
            throw new ProtocolException(e);
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
     * Proceed to process an {@link GuidanceResult} generated using {@link #requestGuidance(Direction)} in
     * which case its {@link GuidanceResult#result} must be 'true'.
     * <p>
     * This invocation may take long to complete as it will behave like a {@link TransportSession}.
     *
     * @param transportSeat  That will manage the requests and do appropriate actions.
     * @param guidanceResult That the remote sent and should be processed.
     * @throws ProtocolException When there is a communication error due to misconfiguration.
     * @throws IOException       If an IO related error occurs.
     */
    public void proceed(TransportSeat transportSeat, GuidanceResult guidanceResult) throws ProtocolException,
            IOException
    {
        if (!guidanceResult.result) throw new IllegalStateException("The result should be true");

        try {
            Responses.handleRequest(persistenceProvider, transportSeat, this, getRemoteClient(),
                    getRemoteClientAddress(), true, guidanceResult.response);
        } catch (CancelledException ignored) {
        } catch (Exception e) {
            Responses.send(activeConnection, e, persistenceProvider.clientAsJson(0));
        }
    }

    /**
     * Request a text-based content transfer.
     * <p>
     * This will invoke the {@link TransportSeat#handleClipboardRequest(Client, String, ClipboardType)} method on the
     * remote.
     *
     * @param content To send.
     * @param type    Of the content.
     * @return True if the request was successful.
     * @throws IOException       If an IO error occurs.
     * @throws JSONException     If something goes wrong when creating JSON object.
     * @throws ProtocolException When there is a communication error due to misconfiguration.
     */
    public boolean requestClipboard(@NotNull String content, @NotNull ClipboardType type) throws JSONException,
            IOException, ProtocolException
    {
        send(true, new JSONObject()
                .put(Keyword.REQUEST, Keyword.REQUEST_CLIPBOARD)
                .put(Keyword.CLIPBOARD_CONTENT, content)
                .put(Keyword.CLIPBOARD_TYPE, type.protocolValue));
        return receiveResult();
    }

    /**
     * Request a file transfer operation by informing the remote that you will send files.
     * <p>
     * The remote may choose to start the transfer without prompting the user. In that case, the return value of this
     * method call will be true, and should be preceded by a
     * {@link TransportSeat#beginFileTransfer(CommunicationBridge, Client, long, Direction)} method call.
     * <p>
     * The remote may also choose to prompt the user. In that case, it will reach out to you using
     * {@link #requestFileTransferStart(long, Direction)}, which will end up in your
     * {@link TransportSeat#beginFileTransfer(CommunicationBridge, Client, long, Direction)} method. That request will
     * happen separately and will not concern this invocation.
     * <p>
     * Finally, if the response is positive (that is the remote doesn't report any errors), the items will be saved to
     * the persistence database using {@link PersistenceProvider#persist(String, List)}.
     *
     * @param groupId          That ties a group of {@link TransferItem} as in {@link TransferItem#getItemGroupId()}.
     * @param transferItemList That you will send.
     * @param prepareListener  To call on success to prepare dependencies.
     * @return True if the transfer should be started now, or false the prompted the user.
     * @throws IOException       If an IO error occurs.
     * @throws JSONException     If something goes wrong when creating JSON object.
     * @throws ProtocolException When there is a communication error due to misconfiguration.
     */
    public boolean requestFileTransfer(long groupId, @NotNull List<@NotNull TransferItem> transferItemList,
                                       @Nullable OnPrepareListener prepareListener)
            throws JSONException, IOException, ProtocolException
    {
        send(true, new JSONObject()
                .put(Keyword.REQUEST, Keyword.REQUEST_TRANSFER)
                .put(Keyword.TRANSFER_GROUP_ID, groupId)
                .put(Keyword.INDEX, Transfers.toJson(transferItemList).toString()));

        boolean result = receiveResult();

        if (prepareListener != null) {
            prepareListener.onPrepare();
        }

        getPersistenceProvider().persist(getRemoteClient().getClientUid(), transferItemList);

        return result;
    }

    /**
     * Request the remote to start file transfer.
     * <p>
     * The transfer request, in this case, has already been sent with
     * {@link #requestFileTransfer(long, List, OnPrepareListener)}.
     * <p>
     * After the method returns positive, the rest of the operation can be carried on with {@link Transfers#receive}
     * or {@link Transfers#send} depending on the direction of the transfer.
     *
     * @param groupId   That ties a group of {@link TransferItem} as in {@link TransferItem#getItemGroupId()}.
     * @param direction Of the transfer as in {@link TransferItem#getItemDirection()}.
     * @return True if successful.
     * @throws IOException       If an IO error occurs.
     * @throws JSONException     If something goes wrong when creating JSON object.
     * @throws ProtocolException When there is a communication error due to misconfiguration.
     * @see Transfers#receive
     * @see Transfers#send
     */
    public boolean requestFileTransferStart(long groupId, @NotNull Direction direction) throws JSONException,
            IOException, ProtocolException
    {
        send(true, new JSONObject()
                .put(Keyword.REQUEST, Keyword.REQUEST_TRANSFER_START)
                .put(Keyword.TRANSFER_GROUP_ID, groupId)
                .put(Keyword.DIRECTION, direction.protocolValue));
        return receiveResult();
    }

    /**
     * Request the remote to choose you if it's about to choose pick a client.
     * <p>
     * For instance, if the remote is setting up a file transfer request and is about to pick a client and if you make
     * this request in that timespan, this will invoke
     * {@link TransportSeat#handleGuidanceRequest(CommunicationBridge, Client, ClientAddress, Direction)} method on
     * the remote, and it will choose you.
     *
     * @param direction Of 'yours' (not reversed) that the remote should respond to.
     * @return The result which also contains the response the remote sent. The actual result will be
     * {@link GuidanceResult#result} which should be preceded by {@link #proceed(TransportSeat, GuidanceResult)}
     * in order to complete.
     * @throws IOException       If an IO error occurs.
     * @throws JSONException     If something goes wrong when creating JSON object.
     * @throws ProtocolException When there is a communication error due to misconfiguration.
     */
    public GuidanceResult requestGuidance(@NotNull Direction direction) throws JSONException, IOException,
            ProtocolException
    {
        send(true, new JSONObject()
                .put(Keyword.REQUEST, Keyword.REQUEST_GUIDANCE)
                .put(Keyword.DIRECTION, direction.protocolValue));

        JSONObject response = Responses.receiveChecked(getActiveConnection(), getRemoteClient());

        return new GuidanceResult(Responses.getResult(response), response);
    }

    /**
     * Inform the remote that its transfer request was rejected.
     * <p>
     * The persistence database can be cleared of the records that belongs to the transfer operation after a
     * successful return.
     *
     * @param groupId Of the transfer that you are informing about.
     * @return True if the request was processed successfully.
     * @throws IOException       If an IO error occurs.
     * @throws JSONException     If something goes wro when creating JSON object.
     * @throws ProtocolException When there is a communication error due to misconfiguration.
     * @see TransportSeat#handleFileTransferRejection(Client, long)
     */
    public boolean requestNotifyTransferRejection(long groupId) throws JSONException, IOException, ProtocolException
    {
        send(true, new JSONObject()
                .put(Keyword.REQUEST, Keyword.REQUEST_NOTIFY_TRANSFER_REJECTION)
                .put(Keyword.TRANSFER_GROUP_ID, groupId));
        return receiveResult();
    }

    /**
     * Request a dummy result for testing purposes.
     * <p>
     * The request will be processed by the remote without notifying the responsible {@link TransportSeat} instance.
     *
     * @return True if everything is okay.
     * @throws IOException       If an IO error occurs.
     * @throws ProtocolException When there is a communication error due to misconfiguration.
     */
    public boolean requestTest() throws IOException, ProtocolException
    {
        send(true, new JSONObject()
                .put(Keyword.REQUEST, Keyword.REQUEST_TEST));
        return receiveResult();
    }

    /**
     * Receive a response from remote and validate it.
     * <p>
     * This will throw the appropriate {@link ProtocolException} when something is not right.
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

        private final @NotNull List<InetAddress> addressList;

        private @Nullable String clientUid;

        private boolean clearBlockedStatus = true;

        private int pin;

        /**
         * Creates a new builder instance.
         *
         * @param connectionFactory   To start and set up connections with.
         * @param persistenceProvider To store and query objects with.
         * @param addressList         To connect to.
         */
        public Builder(@NotNull ConnectionFactory connectionFactory,
                       @NotNull PersistenceProvider persistenceProvider,
                       @NotNull List<InetAddress> addressList)
        {
            this.connectionFactory = connectionFactory;
            this.persistenceProvider = persistenceProvider;
            this.addressList = addressList;
        }

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
            this(connectionFactory, persistenceProvider, Collections.singletonList(inetAddress));
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
        public @NotNull CommunicationBridge connect() throws IOException, JSONException, ProtocolException,
                CertificateException
        {
            ActiveConnection activeConnection = openConnection();
            InetAddress address = activeConnection.getAddress();
            String remoteClientUid = activeConnection.receive().getAsString();

            if (clientUid != null && !clientUid.equals(remoteClientUid)) {
                Responses.send(activeConnection, false, new JSONObject());
                throw new DifferentRemoteClientException(clientUid, remoteClientUid, address);
            }

            Responses.send(activeConnection, true, persistenceProvider.clientAsJson(pin));

            JSONObject jsonObject = activeConnection.receive().getAsJson();
            ClientAddress clientAddress = persistenceProvider.createClientAddressFor(address, remoteClientUid);
            Client client = ClientLoader.loadAsClient(persistenceProvider, jsonObject, remoteClientUid, clientAddress,
                    clearBlockedStatus);

            Responses.checkError(client, jsonObject);

            try {
                convertToSSL(connectionFactory, persistenceProvider, activeConnection, client, true);
            } catch (SecurityException e) {
                if (!persistenceProvider.hasRequestForInvalidationOfCredentials(client.getClientUid())) {
                    persistenceProvider.saveRequestForInvalidationOfCredentials(client.getClientUid());
                }

                throw e;
            }
            return new CommunicationBridge(persistenceProvider, activeConnection, client, clientAddress);
        }

        private @NotNull ActiveConnection openConnection() throws IOException
        {
            List<IOException> underlyingExceptionList = new ArrayList<>();

            for (InetAddress address : addressList) {
                try {
                    return connectionFactory.openConnection(address);
                } catch (IOException e) {
                    underlyingExceptionList.add(e);
                }
            }

            throw new DefectiveAddressListException(underlyingExceptionList, addressList);
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
