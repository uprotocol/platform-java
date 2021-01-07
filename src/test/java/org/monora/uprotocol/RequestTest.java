package org.monora.uprotocol;

import org.junit.Assert;
import org.junit.Test;
import org.monora.uprotocol.core.CommunicationBridge;
import org.monora.uprotocol.core.network.Device;
import org.monora.uprotocol.core.network.TransferItem;
import org.monora.uprotocol.core.persistence.PersistenceException;
import org.monora.uprotocol.core.protocol.communication.CommunicationException;
import org.monora.uprotocol.core.protocol.communication.NotAllowedException;
import org.monora.uprotocol.variant.test.DefaultTestBase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RequestTest extends DefaultTestBase
{
    @Test
    public void requestAcquaintanceTest() throws IOException, InterruptedException, CommunicationException,
            PersistenceException
    {
        primarySession.start();

        try (CommunicationBridge bridge = CommunicationBridge.connect(connectionFactory, secondaryPersistence,
                deviceAddress, null, 0)) {
            Assert.assertTrue("Remote should send a positive message.", bridge.requestAcquaintance());

            Device persistentDevice = secondaryPersistence.createDeviceFor(bridge.getDevice().uid);
            secondaryPersistence.sync(persistentDevice);

            Assert.assertEquals("Devices should be same.", bridge.getDevice(), persistentDevice);
            Assert.assertEquals("Devices should have the same username.", bridge.getDevice().username,
                    persistentDevice.username);
        } finally {
            primarySession.stop();
        }
    }

    @Test
    public void requestFileTransferTest() throws IOException, InterruptedException, CommunicationException
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

    @Test(expected = NotAllowedException.class)
    public void failsWithKeyMismatchTest() throws IOException, InterruptedException, CommunicationException,
            PersistenceException
    {
        primarySession.start();

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, deviceAddress)) {
            bridge.requestAcquaintance();
        }

        Device primaryOnSecondary = secondaryPersistence.createDeviceFor(primaryPersistence.getDeviceUid());
        secondaryPersistence.sync(primaryOnSecondary);
        primaryOnSecondary.senderKey = 1;
        secondaryPersistence.save(primaryOnSecondary);

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, deviceAddress)) {
            bridge.requestAcquaintance();
        } finally {
            primarySession.stop();
        }
    }

    @Test
    public void connectsAfterKeyMismatchWithRightKey() throws IOException, CommunicationException, InterruptedException,
            PersistenceException
    {
        primarySession.start();

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, deviceAddress)) {
            bridge.requestAcquaintance();
        }

        Device primaryOnSecondary = secondaryPersistence.createDeviceFor(primaryPersistence.getDeviceUid());
        secondaryPersistence.sync(primaryOnSecondary);
        int originalKey = primaryOnSecondary.senderKey;
        primaryOnSecondary.senderKey = 1;
        secondaryPersistence.save(primaryOnSecondary);

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, deviceAddress)) {
            bridge.requestAcquaintance();
        } catch (NotAllowedException ignored) {
        }

        primaryOnSecondary.senderKey = originalKey;
        secondaryPersistence.save(primaryOnSecondary);

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, deviceAddress)) {
            bridge.requestAcquaintance();
        } finally {
            primarySession.stop();
        }
    }

    @Test
    public void acceptNewKeysTest() throws IOException, InterruptedException, CommunicationException,
            PersistenceException
    {
        primarySession.start();

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, deviceAddress)) {
            bridge.requestAcquaintance();
        }

        primarySeat.setAutoAcceptNewKeys(true);

        Device primaryOnSecondary = secondaryPersistence.createDeviceFor(primaryPersistence.getDeviceUid());
        secondaryPersistence.sync(primaryOnSecondary);
        primaryOnSecondary.senderKey = 1;
        secondaryPersistence.save(primaryOnSecondary);

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, deviceAddress)) {
            bridge.requestAcquaintance();
        } catch (NotAllowedException ignored) {
        }

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, deviceAddress)) {
            bridge.requestAcquaintance();
        } finally {
            primarySession.stop();
        }
    }
}
