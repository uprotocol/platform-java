package org.monora.uprotocol;

import org.junit.Assert;
import org.junit.Test;
import org.monora.uprotocol.core.ClientLoader;
import org.monora.uprotocol.core.CommunicationBridge;
import org.monora.uprotocol.core.protocol.Client;
import org.monora.uprotocol.core.protocol.communication.ProtocolException;
import org.monora.uprotocol.core.protocol.communication.SecurityException;
import org.monora.uprotocol.core.protocol.communication.client.UnauthorizedClientException;
import org.monora.uprotocol.core.transfer.TransferItem;
import org.monora.uprotocol.variant.test.DefaultTestBase;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

public class RequestTest extends DefaultTestBase
{
    @Test
    public void requestAcquaintanceTest() throws IOException, InterruptedException, ProtocolException,
            CertificateException
    {
        primarySession.start();

        try (CommunicationBridge bridge = CommunicationBridge.connect(connectionFactory, secondaryPersistence,
                clientAddress, null, 0)) {
            Assert.assertTrue("Remote should send a positive message.", bridge.requestAcquaintance());

            Client persistentClient = secondaryPersistence.getClientFor(bridge.getRemoteClient().getClientUid());

            Assert.assertNotNull("The client should not be null on the remote db", persistentClient);
            Assert.assertEquals("Clients should be same.", bridge.getRemoteClient(), persistentClient);
            Assert.assertEquals("Clients should have the same username.", bridge.getRemoteClient().getClientNickname(),
                    persistentClient.getClientNickname());
            Assert.assertEquals("Clients should be of the same type.", bridge.getRemoteClient().getClientType(),
                    persistentClient.getClientType());
            Assert.assertEquals("Client types should have the same protocol value.",
                    bridge.getRemoteClient().getClientType().getProtocolValue(),
                    persistentClient.getClientType().getProtocolValue());
        } finally {
            primarySession.stop();
        }
    }

    @Test
    public void requestFileTransferTest() throws IOException, InterruptedException, ProtocolException,
            CertificateException
    {
        primarySession.start();

        final List<TransferItem> transferItemList = new ArrayList<>();
        final long groupId = 1;

        transferItemList.add(secondaryPersistence.createTransferItemFor(groupId, 1, "1.jpg",
                "image/jpeg", 0, null, TransferItem.Type.Outgoing));
        transferItemList.add(secondaryPersistence.createTransferItemFor(groupId, 2, "2.jpg",
                "image/jpeg", 0, null, TransferItem.Type.Outgoing));
        transferItemList.add(secondaryPersistence.createTransferItemFor(groupId, 3, "3.jpg",
                "image/jpeg", 0, null, TransferItem.Type.Outgoing));

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, clientAddress)) {
            Assert.assertTrue("The request should be successful", bridge.requestFileTransfer(groupId,
                    transferItemList));
        } finally {
            primarySession.stop();
        }
    }

    @Test(expected = SecurityException.class)
    public void failsWithKeyMismatchTest() throws IOException, InterruptedException, ProtocolException,
            CertificateException
    {
        primarySession.start();

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, clientAddress)) {
            bridge.requestAcquaintance();
        }

        primaryPersistence.regenerateSecrets();

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, clientAddress)) {
            bridge.requestAcquaintance();
        } finally {
            primarySession.stop();
        }
    }

    @Test
    public void connectsAfterKeyMismatchWithRightKey() throws IOException, ProtocolException, InterruptedException,
            CertificateException
    {
        primarySession.start();

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, clientAddress)) {
            bridge.requestAcquaintance();
        }

        primaryPersistence.regenerateSecrets();

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, clientAddress)) {
            bridge.requestAcquaintance();
        } catch (SecurityException ignored) {
        }

        primaryPersistence.restoreSecrets();

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, clientAddress)) {
            bridge.requestAcquaintance();
        } finally {
            primarySession.stop();
        }
    }

    @Test
    public void acceptNewKeysTest() throws IOException, InterruptedException, ProtocolException, CertificateException
    {
        primarySession.start();

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, clientAddress)) {
            bridge.requestAcquaintance();
        }

        primarySeat.setAutoInvalidationOfCredentials(true);
        primaryPersistence.regenerateSecrets();

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, clientAddress)) {
            bridge.requestAcquaintance();
        } catch (SecurityException e) {
            if (e.getCause() instanceof SSLHandshakeException) {
                e.client.setClientCertificate(null);
                secondaryPersistence.persist(e.client, true);
            } else
                throw e;
        }

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, clientAddress)) {
            bridge.requestAcquaintance();
        } finally {
            primarySession.stop();
        }
    }

    @Test(expected = UnauthorizedClientException.class)
    public void throwsAppropriateErrorWhenBlocked() throws IOException, InterruptedException, CertificateException,
            ProtocolException
    {
        primarySession.start();

        try (CommunicationBridge bridge = CommunicationBridge.connect(connectionFactory, secondaryPersistence,
                clientAddress, null, 0)) {
            Assert.assertTrue("Remote should send a positive message.", bridge.requestAcquaintance());
        }

        Client secondaryOnPrimary = primaryPersistence.getClientFor(secondaryPersistence.getClientUid());

        Assert.assertNotNull("Secondary client on primary persistence should exist", secondaryOnPrimary);

        secondaryOnPrimary.setClientBlocked(true);
        primaryPersistence.persist(secondaryOnPrimary, true);

        try (CommunicationBridge bridge = CommunicationBridge.connect(connectionFactory, secondaryPersistence,
                clientAddress, null, 0)) {
            Assert.fail("This scope should not get executed as the above scope should fail.");
        } finally {
            primarySession.stop();
        }
    }

    @Test
    public void loadDeviceTest() throws IOException, InterruptedException, ProtocolException, CertificateException
    {
        primarySession.start();

        try {
            Client primaryAsRemote = ClientLoader.load(connectionFactory, secondaryPersistence, clientAddress);
            Assert.assertEquals("The UIDs should be the same.", primaryAsRemote.getClientUid(),
                    primaryPersistence.getClientUid());
        } finally {
            primarySession.stop();
        }
    }
}
