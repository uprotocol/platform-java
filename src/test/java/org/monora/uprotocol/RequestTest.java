package org.monora.uprotocol;

import org.junit.Assert;
import org.junit.Test;
import org.monora.coolsocket.core.session.CancelledException;
import org.monora.coolsocket.core.session.ClosedException;
import org.monora.uprotocol.core.ClientLoader;
import org.monora.uprotocol.core.CommunicationBridge;
import org.monora.uprotocol.core.protocol.Client;
import org.monora.uprotocol.core.protocol.communication.ContentException;
import org.monora.uprotocol.core.protocol.communication.ProtocolException;
import org.monora.uprotocol.core.protocol.communication.SecurityException;
import org.monora.uprotocol.core.protocol.communication.client.BlockedRemoteClientException;
import org.monora.uprotocol.core.protocol.communication.client.UnauthorizedClientException;
import org.monora.uprotocol.core.transfer.TransferItem;
import org.monora.uprotocol.core.transfer.Transfers;
import org.monora.uprotocol.variant.holder.MemoryStreamDescriptor;
import org.monora.uprotocol.variant.holder.TransferHolder;
import org.monora.uprotocol.variant.test.DefaultTestBase;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RequestTest extends DefaultTestBase
{
    @Test
    public void requestAcquaintanceTest() throws IOException, InterruptedException, ProtocolException,
            CertificateException
    {
        primarySession.start();

        try (CommunicationBridge bridge = CommunicationBridge.connect(connectionFactory, secondaryPersistence,
                clientAddress)) {
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

        transferItemList.add(secondaryPersistence.createTransferItemFor(groupId, 1, "1.mp4",
                "video/mp4", MemoryStreamDescriptor.MAX_SIZE, null, TransferItem.Type.Outgoing));
        transferItemList.add(secondaryPersistence.createTransferItemFor(groupId, 2, "2.jpg",
                "image/jpeg", 8196, null, TransferItem.Type.Outgoing));
        transferItemList.add(secondaryPersistence.createTransferItemFor(groupId, 3, "3.jpg",
                "image/jpeg", 0, "doggos", TransferItem.Type.Outgoing));

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, clientAddress)) {
            Assert.assertTrue("The request should be successful", bridge.requestFileTransfer(groupId,
                    transferItemList, null));
        } finally {
            primarySession.stop();
        }

        // CoolSocket does blocking close
        for (TransferHolder remoteTransferHolder : primaryPersistence.getTransferHolderList()) {
            TransferItem targetTransferItem = null;
            for (TransferItem transferItem : transferItemList) {
                if (remoteTransferHolder.item.getItemId() == transferItem.getItemId()) {
                    targetTransferItem = transferItem;
                    break;
                }
            }
            Assert.assertNotNull("Target transfer item should not be empty", targetTransferItem);
            Assert.assertEquals("Group IDs should be the same", targetTransferItem.getItemGroupId(),
                    remoteTransferHolder.item.getItemGroupId());
            Assert.assertEquals("MIME-Types should be the same", targetTransferItem.getItemMimeType(),
                    remoteTransferHolder.item.getItemMimeType());
            Assert.assertEquals("Lengths should be the same", targetTransferItem.getItemSize(),
                    remoteTransferHolder.item.getItemSize());
        }
    }

    @Test
    public void transferStateRemovedAppliedIfSenderNotifiesNotFound() throws IOException, InterruptedException,
            ProtocolException, CertificateException
    {
        primarySession.start();

        final List<TransferItem> transferItemList = new ArrayList<>();
        final long groupId = 1;

        transferItemList.add(secondaryPersistence.createTransferItemFor(groupId, 1, "1.mp4",
                "video/mp4", MemoryStreamDescriptor.MAX_SIZE, null, TransferItem.Type.Outgoing));

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, clientAddress)) {
            Assert.assertTrue("The request should be successful", bridge.requestFileTransfer(groupId,
                    transferItemList, null));
        } finally {
            primarySession.stop();
        }

        TransferHolder secondaryTransferHolder = secondaryPersistence.getTransferHolderList().get(0);

        Assert.assertNotNull("Secondary transfer holder should exist", secondaryTransferHolder);

        secondaryTransferHolder.item.setItemId(secondaryTransferHolder.item.getItemId() + 1);

        secondarySession.start();

        TransferHolder transferHolder = primaryPersistence.getTransferHolderList().get(0);

        Assert.assertNotNull("Transfer holder should not be null", transferHolder);

        try (CommunicationBridge bridge = openConnection(primaryPersistence, clientAddress)) {
            if (bridge.requestFileTransferStart(transferHolder.item.getItemGroupId(),
                    transferHolder.item.getItemType())) {
                Transfers.receive(bridge, transferOperation, transferHolder.item.getItemGroupId());
            } else {
                Assert.fail("Request for start should not fail");
            }

            Assert.assertEquals("The removed state should match", TransferItem.State.Invalidated,
                    transferHolder.state);
        } finally {
            secondarySession.stop();
        }
    }

    @Test(expected = ContentException.class)
    public void remoteNotifiesAsNotFoundWhenGroupNotExists() throws ProtocolException, CertificateException,
            IOException, InterruptedException
    {
        secondarySession.start();

        final long randomGroupId = 42;

        try (CommunicationBridge bridge = openConnection(primaryPersistence, clientAddress)) {
            if (bridge.requestFileTransferStart(randomGroupId, TransferItem.Type.Incoming)) {
                Assert.fail("The above scope should have failed with an exception");
            }
        } catch (ContentException e) {
            Assert.assertEquals("The error code should be 'NOT FOUND'.", e.error,
                    ContentException.Error.NotFound);
            throw e;
        } finally {
            secondarySession.stop();
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

    @Test(expected = CancelledException.class)
    public void cancelsSuccessfully() throws ProtocolException, CertificateException, IOException, InterruptedException
    {
        primarySession.start();

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, clientAddress)) {
            bridge.getActiveConnection().cancel();
            bridge.requestAcquaintance();
        } finally {
            primarySession.stop();
        }
    }

    @Test(expected = ClosedException.class)
    public void earlyClosesSuccessfully() throws ProtocolException, CertificateException, IOException,
            InterruptedException
    {
        primarySession.start();

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, clientAddress)) {
            bridge.closeSafely();
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
                clientAddress)) {
            Assert.assertTrue("Remote should send a positive message.", bridge.requestAcquaintance());
        }

        Client secondaryOnPrimary = primaryPersistence.getClientFor(secondaryPersistence.getClientUid());

        Assert.assertNotNull("Secondary client on primary persistence should exist", secondaryOnPrimary);

        secondaryOnPrimary.setClientBlocked(true);
        primaryPersistence.persist(secondaryOnPrimary, true);

        try (CommunicationBridge bridge = CommunicationBridge.connect(connectionFactory, secondaryPersistence,
                clientAddress)) {
            Assert.fail("This scope should not get executed as the above scope should fail.");
        } finally {
            primarySession.stop();
        }
    }

    @Test
    public void clientFlaggedAsTrustedWithPin() throws IOException, InterruptedException, CertificateException,
            ProtocolException
    {
        primarySession.start();

        CommunicationBridge.Builder builder = new CommunicationBridge.Builder(connectionFactory, secondaryPersistence,
                clientAddress);
        builder.setPin(primaryPersistence.getNetworkPin());

        try (CommunicationBridge bridge = builder.connect()) {
            bridge.requestAcquaintance();
        } finally {
            primarySession.stop();
        }

        Client secondaryOnPrimary = primaryPersistence.getClientFor(secondaryPersistence.getClientUid());

        Assert.assertNotNull("Secondary client should exist on primary persistence.", secondaryOnPrimary);
        Assert.assertTrue("The device should be trusted", secondaryOnPrimary.isClientTrusted());
    }

    @Test(expected = BlockedRemoteClientException.class)
    public void clientConnectionFailsWhenClearingUnblockIsDisabled() throws IOException, InterruptedException,
            CertificateException, ProtocolException
    {
        primarySession.start();

        Client primaryClient = primaryPersistence.getClient();
        primaryClient.setClientBlocked(true);

        secondaryPersistence.persist(primaryClient, false);

        CommunicationBridge.Builder builder = new CommunicationBridge.Builder(connectionFactory, secondaryPersistence,
                clientAddress);
        builder.setClearBlockedStatus(false);

        try (CommunicationBridge bridge = builder.connect()) {
            bridge.requestAcquaintance();
        } finally {
            primarySession.stop();
        }
    }

    @Test
    public void clientUnblockedWithPin() throws IOException, InterruptedException, CertificateException,
            ProtocolException
    {
        primarySession.start();

        Client secondaryClient = secondaryPersistence.getClient();
        secondaryClient.setClientBlocked(true);

        primaryPersistence.persist(secondaryClient, false);

        CommunicationBridge.Builder builder = new CommunicationBridge.Builder(connectionFactory, secondaryPersistence,
                clientAddress);
        builder.setPin(primaryPersistence.getNetworkPin());

        try (CommunicationBridge bridge = builder.connect()) {
            bridge.requestAcquaintance();
        } finally {
            primarySession.stop();
        }

        Client secondaryOnPrimary = primaryPersistence.getClientFor(secondaryPersistence.getClientUid());

        Assert.assertNotNull("Secondary client should exist on primary persistence.", secondaryOnPrimary);
        Assert.assertFalse("The device should be unblocked", secondaryOnPrimary.isClientBlocked());
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

    @Test
    public void primaryClientPictureDeliveredSuccessfully() throws IOException, InterruptedException,
            CertificateException, ProtocolException
    {
        primarySession.start();

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, clientAddress)) {
            bridge.requestAcquaintance();
        } finally {
            primarySession.stop();
        }

        Client primaryOnSecondary = secondaryPersistence.getClientFor(primaryPersistence.getClientUid());
        Assert.assertNotNull("The primary client should exist on secondary", primaryOnSecondary);
        Assert.assertTrue("The primary client should have a picture", primaryOnSecondary.hasPicture());
        Assert.assertEquals("Primary picture data should match",
                Arrays.hashCode(primaryOnSecondary.getClientPictureData()),
                Arrays.hashCode(primaryPersistence.getClient().getClientPictureData()));
        Assert.assertEquals("Primary picture checksum should persist",
                primaryOnSecondary.getClientPictureChecksum(),
                primaryPersistence.getClient().getClientPictureChecksum());
    }

    @Test
    public void secondaryClientPictureDeliveredSuccessfully() throws IOException, InterruptedException,
            CertificateException, ProtocolException
    {
        primarySession.start();

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, clientAddress)) {
            bridge.requestAcquaintance();
        } finally {
            primarySession.stop();
        }

        Client secondaryOnPrimary = primaryPersistence.getClientFor(secondaryPersistence.getClientUid());
        Assert.assertNotNull("Secondary should not be null on primary", secondaryOnPrimary);
        Assert.assertTrue("The secondary client should have a picture", secondaryOnPrimary.hasPicture());
        Assert.assertEquals("Secondary picture data should match",
                Arrays.hashCode(secondaryOnPrimary.getClientPictureData()),
                Arrays.hashCode(secondaryPersistence.getClient().getClientPictureData()));
        Assert.assertEquals("Secondary picture checksum should persist",
                secondaryOnPrimary.getClientPictureChecksum(),
                secondaryPersistence.getClient().getClientPictureChecksum());
    }

    @Test
    public void rejectionRemovesTransfers() throws IOException, InterruptedException,
            ProtocolException, CertificateException
    {
        primarySession.start();

        final List<TransferItem> transferItemList = new ArrayList<>();
        final long groupId = 1;

        transferItemList.add(secondaryPersistence.createTransferItemFor(groupId, 1, "Cats.mp4",
                "video/mp4", MemoryStreamDescriptor.MAX_SIZE, null, TransferItem.Type.Outgoing));

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, clientAddress)) {
            Assert.assertTrue("The request should be successful", bridge.requestFileTransfer(groupId,
                    transferItemList, null));
        } finally {
            primarySession.stop();
        }

        Assert.assertTrue("Request should exist", primaryPersistence.containsTransfer(groupId));

        primarySession.start();

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, clientAddress)) {
            Assert.assertTrue("The rejection request should return positive",
                    bridge.requestNotifyTransferRejection(groupId));
        } finally {
            primarySession.stop();
        }

        Assert.assertFalse("Transfer should not exist after rejection",
                primaryPersistence.containsTransfer(groupId));
    }
}
