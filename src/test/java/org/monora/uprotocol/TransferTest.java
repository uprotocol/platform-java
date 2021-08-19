package org.monora.uprotocol;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.monora.uprotocol.core.CommunicationBridge;
import org.monora.uprotocol.core.io.StreamDescriptor;
import org.monora.uprotocol.core.protocol.Client;
import org.monora.uprotocol.core.protocol.Direction;
import org.monora.uprotocol.core.protocol.communication.ProtocolException;
import org.monora.uprotocol.core.protocol.communication.client.UntrustedClientException;
import org.monora.uprotocol.core.transfer.TransferItem;
import org.monora.uprotocol.core.transfer.Transfers;
import org.monora.uprotocol.variant.holder.MemoryStreamDescriptor;
import org.monora.uprotocol.variant.holder.TransferHolder;
import org.monora.uprotocol.variant.test.DefaultTestBase;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

/**
 * This test class ensures that two different sides can communicate.
 * <p>
 * Because we can't run two servers at the same time due to the static port being in use, we are starting one at a time
 * with the two sides having their own instances of objects separated as primary and secondary.
 */
public class TransferTest extends DefaultTestBase
{
    private @NotNull TransferItem demoTransferItem1;
    private @NotNull TransferItem demoTransferItem2;

    private final static byte[] data1 = "This is the first demo data".getBytes();
    private final static byte[] data2 = "This is the second demo data".getBytes();
    private final long groupId = 1;

    @Before
    public void setUp() throws IOException
    {
        demoTransferItem1 = secondaryPersistence.createTransferItemFor(groupId, 1, "File1",
                "text/plain", data1.length, null, Direction.Outgoing);
        demoTransferItem2 = secondaryPersistence.createTransferItemFor(groupId, 2, "File2",
                "text/plain", data2.length, null, Direction.Outgoing);

        StreamDescriptor descriptor1 = secondaryPersistence.getDescriptorFor(demoTransferItem1);
        StreamDescriptor descriptor2 = secondaryPersistence.getDescriptorFor(demoTransferItem2);

        secondaryPersistence.openOutputStream(descriptor1).write(data1);
        secondaryPersistence.openOutputStream(descriptor2).write(data2);
    }

    @Before
    public void setUpInitialTransferItems() throws IOException, InterruptedException, ProtocolException,
            CertificateException
    {
        primarySession.start();

        final List<TransferItem> itemList = new ArrayList<>();
        itemList.add(demoTransferItem1);
        itemList.add(demoTransferItem2);

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, clientAddress)) {
            bridge.requestFileTransfer(groupId, itemList, null);
        } finally {
            primarySession.stop();
        }
    }

    @Test
    public void sendFilesTest() throws IOException, InterruptedException, ProtocolException, CertificateException
    {
        secondarySession.start();

        try (CommunicationBridge bridge = openConnection(primaryPersistence, clientAddress)) {
            Assert.assertTrue("The result should be positive", bridge.requestFileTransferStart(groupId,
                    Direction.Incoming));

            Transfers.receive(bridge, transferOperation, groupId);
        } finally {
            secondarySession.stop();
        }

        // Compare the received data with the original data.
        List<MemoryStreamDescriptor> primaryList = primaryPersistence.getStreamDescriptorList();
        List<MemoryStreamDescriptor> secondaryList = secondaryPersistence.getStreamDescriptorList();

        for (MemoryStreamDescriptor descriptor : primaryList) {
            boolean dataMatched = false;
            for (MemoryStreamDescriptor secondaryDescriptor : secondaryList) {
                if (descriptor.transferItem.getItemGroupId() == secondaryDescriptor.transferItem.getItemGroupId()
                        && descriptor.transferItem.getItemId() == secondaryDescriptor.transferItem.getItemId()) {
                    Assert.assertEquals("The data should match", descriptor.data.toString(),
                            secondaryDescriptor.data.toString());
                    dataMatched = true;
                }
            }

            Assert.assertTrue("There should not be a missing data", dataMatched);
        }
    }

    @Test
    public void flagItemAsDoneTest() throws IOException, InterruptedException, ProtocolException, CertificateException
    {
        secondarySession.start();

        try (CommunicationBridge bridge = openConnection(primaryPersistence, clientAddress)) {
            bridge.requestFileTransferStart(groupId, Direction.Incoming);
            Transfers.receive(bridge, transferOperation, groupId);
        } finally {
            secondarySession.stop();
        }

        final List<TransferHolder> itemList = new ArrayList<>();
        itemList.addAll(primaryPersistence.getTransferHolderList());
        itemList.addAll(secondaryPersistence.getTransferHolderList());

        Assert.assertEquals("Items should not overwrite their counterparts", itemList.size(),
                primaryPersistence.getTransferHolderList().size() + secondaryPersistence.getTransferHolderList().size());

        for (TransferHolder holder : itemList) {
            Assert.assertEquals("The item should be marked as done", TransferItem.State.Done, holder.state);
        }
    }

    @Test(expected = UntrustedClientException.class)
    public void senderFailsToStartTransferIfNotTrusted() throws IOException, InterruptedException,
            ProtocolException, CertificateException
    {
        primarySession.start();

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, clientAddress)) {
            bridge.requestFileTransferStart(groupId, Direction.Outgoing);
            Transfers.receive(bridge, transferOperation, groupId);
        } finally {
            primarySession.stop();
        }
    }

    @Test
    public void senderStartsTransferIfTrusted() throws IOException, InterruptedException, ProtocolException,
            CertificateException
    {
        Client secondaryOnPrimary = primaryPersistence.getClientFor(secondaryPersistence.getClientUid());

        Assert.assertNotNull(secondaryOnPrimary);

        secondaryOnPrimary.setClientTrusted(true);
        primaryPersistence.persist(secondaryOnPrimary, true);

        primarySession.start();

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, clientAddress)) {
            bridge.requestFileTransferStart(groupId, Direction.Outgoing);
            Transfers.receive(bridge, transferOperation, groupId);
        } finally {
            primarySession.stop();
        }
    }
}
