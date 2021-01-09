package org.monora.uprotocol;

import org.junit.Assert;
import org.junit.Test;
import org.monora.uprotocol.core.CommunicationBridge;
import org.monora.uprotocol.core.network.Client;
import org.monora.uprotocol.core.network.TransferItem;
import org.monora.uprotocol.core.persistence.PersistenceException;
import org.monora.uprotocol.core.protocol.communication.ProtocolException;
import org.monora.uprotocol.core.protocol.communication.SecurityException;
import org.monora.uprotocol.variant.test.DefaultTestBase;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RequestTest extends DefaultTestBase
{
    @Test
    public void requestAcquaintanceTest() throws IOException, InterruptedException, ProtocolException,
            PersistenceException
    {
        primarySession.start();

        try (CommunicationBridge bridge = CommunicationBridge.connect(connectionFactory, secondaryPersistence,
                deviceAddress, null, 0)) {
            Assert.assertTrue("Remote should send a positive message.", bridge.requestAcquaintance());

            Client persistentClient = secondaryPersistence.createDeviceFor(bridge.getDevice().uid);
            secondaryPersistence.sync(persistentClient);

            Assert.assertEquals("Devices should be same.", bridge.getDevice(), persistentClient);
            Assert.assertEquals("Devices should have the same username.", bridge.getDevice().username,
                    persistentClient.username);
        } finally {
            primarySession.stop();
        }
    }

    @Test
    public void requestFileTransferTest() throws IOException, InterruptedException, ProtocolException
    {
        primarySession.start();

        final List<TransferItem> transferItemList = new ArrayList<>();
        final long transferId = 1;

        transferItemList.add(secondaryPersistence.createTransferItemFor(transferId, 1, "1.jpg",
                "image/jpeg", 0, null, TransferItem.Type.OUTGOING));
        transferItemList.add(secondaryPersistence.createTransferItemFor(transferId, 2, "2.jpg",
                "image/jpeg", 0, null, TransferItem.Type.OUTGOING));
        transferItemList.add(secondaryPersistence.createTransferItemFor(transferId, 3, "3.jpg",
                "image/jpeg", 0, null, TransferItem.Type.OUTGOING));

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, deviceAddress)) {
            Assert.assertTrue("The request should be successful", bridge.requestFileTransfer(transferId,
                    transferItemList));
        } finally {
            primarySession.stop();
        }
    }

    @Test(expected = SecurityException.class)
    public void failsWithKeyMismatchTest() throws IOException, InterruptedException, ProtocolException
    {
        primarySession.start();

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, deviceAddress)) {
            bridge.requestAcquaintance();
        }

        primaryPersistence.regenerateSecrets();

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, deviceAddress)) {
            bridge.requestAcquaintance();
        } finally {
            primarySession.stop();
        }
    }

    @Test
    public void connectsAfterKeyMismatchWithRightKey() throws IOException, ProtocolException, InterruptedException
    {
        primarySession.start();

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, deviceAddress)) {
            bridge.requestAcquaintance();
        }

        primaryPersistence.regenerateSecrets();

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, deviceAddress)) {
            bridge.requestAcquaintance();
        } catch (SecurityException ignored) {
        }

        primaryPersistence.restoreSecrets();

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, deviceAddress)) {
            bridge.requestAcquaintance();
        } finally {
            primarySession.stop();
        }
    }

    @Test
    public void acceptNewKeysTest() throws IOException, InterruptedException, ProtocolException
    {
        primarySession.start();

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, deviceAddress)) {
            bridge.requestAcquaintance();
        }

        primarySeat.setAutoInvalidationOfCredentials(true);
        primaryPersistence.regenerateSecrets();

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, deviceAddress)) {
            bridge.requestAcquaintance();
        } catch (SecurityException e) {
            if (e.getCause() instanceof SSLHandshakeException) {
                e.client.certificate = null;
                secondaryPersistence.save(e.client);
            } else
                throw e;
        }

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, deviceAddress)) {
            bridge.requestAcquaintance();
        } finally {
            primarySession.stop();
        }
    }
}
