package org.monora.uprotocol;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.monora.uprotocol.core.CommunicationBridge;
import org.monora.uprotocol.core.network.Client;
import org.monora.uprotocol.core.network.ClientAddress;
import org.monora.uprotocol.core.transfer.Transfer;
import org.monora.uprotocol.core.persistence.PersistenceException;
import org.monora.uprotocol.core.persistence.PersistenceProvider;
import org.monora.uprotocol.core.persistence.StreamDescriptor;
import org.monora.uprotocol.core.protocol.communication.ProtocolException;
import org.monora.uprotocol.core.protocol.communication.client.UntrustedClientException;
import org.monora.uprotocol.variant.holder.MemoryStreamDescriptor;
import org.monora.uprotocol.variant.holder.OwnedTransferHolder;
import org.monora.uprotocol.variant.test.DefaultTestBase;

import java.io.IOException;
import java.net.InetAddress;
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
    private ClientAddress clientAddress;
    private Transfer demoTransfer1;
    private Transfer demoTransfer2;

    private final static byte[] data1 = "This is the first demo data".getBytes();
    private final static byte[] data2 = "This is the second demo data".getBytes();
    private final long groupId = 1;

    @Before
    public void setUp() throws IOException
    {
        demoTransfer1 = secondaryPersistence.createTransferFor(groupId, 1, "File1",
                "text/plain", data1.length, null, Transfer.Type.OUTGOING);
        demoTransfer2 = secondaryPersistence.createTransferFor(groupId, 2, "File2",
                "text/plain", data2.length, null, Transfer.Type.OUTGOING);

        StreamDescriptor descriptor1 = secondaryPersistence.getDescriptorFor(demoTransfer1);
        StreamDescriptor descriptor2 = secondaryPersistence.getDescriptorFor(demoTransfer2);

        secondaryPersistence.openOutputStream(descriptor1).write(data1);
        secondaryPersistence.openOutputStream(descriptor2).write(data2);

        clientAddress = primaryPersistence.createClientAddressFor(InetAddress.getLocalHost());
    }

    @Before
    public void setUpInitialTransferItems() throws IOException, InterruptedException, ProtocolException
    {
        primarySession.start();

        final List<Transfer> itemList = new ArrayList<>();
        itemList.add(demoTransfer1);
        itemList.add(demoTransfer2);

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, clientAddress)) {
            bridge.requestFileTransfer(groupId, itemList);
        }

        primarySession.stop();
    }

    @Test
    public void sendFilesTest() throws IOException, InterruptedException, ProtocolException
    {
        secondarySession.start();

        try (CommunicationBridge bridge = openConnection(primaryPersistence, clientAddress)) {
            Assert.assertTrue("The result should be positive", bridge.requestFileTransferStart(groupId,
                    Transfer.Type.INCOMING));

            primarySeat.receiveFiles(bridge, groupId);
        }

        secondarySession.stop();

        // Compare the received data with the original data.
        List<MemoryStreamDescriptor> primaryList = primaryPersistence.getStreamDescriptorList();
        List<MemoryStreamDescriptor> secondaryList = secondaryPersistence.getStreamDescriptorList();

        for (MemoryStreamDescriptor descriptor : primaryList) {
            boolean dataMatched = false;
            for (MemoryStreamDescriptor secondaryDescriptor : secondaryList) {
                if (descriptor.transfer.getTransferGroupId() == secondaryDescriptor.transfer.getTransferGroupId()
                        && descriptor.transfer.getTransferId() == secondaryDescriptor.transfer.getTransferId()) {
                    Assert.assertEquals("The data should match", descriptor.data.toString(),
                            secondaryDescriptor.data.toString());
                    dataMatched = true;
                }
            }

            Assert.assertTrue("There should not be a missing data", dataMatched);
        }
    }

    @Test
    public void flagItemAsDoneTest() throws IOException, InterruptedException, ProtocolException
    {
        secondarySession.start();

        try (CommunicationBridge bridge = openConnection(primaryPersistence, clientAddress)) {
            bridge.requestFileTransferStart(groupId, Transfer.Type.INCOMING);
            primarySeat.receiveFiles(bridge, groupId);
        }

        secondarySession.stop();

        final List<OwnedTransferHolder> itemList = new ArrayList<>();
        itemList.addAll(primaryPersistence.getTransferHolderList());
        itemList.addAll(secondaryPersistence.getTransferHolderList());

        Assert.assertEquals("Items should not overwrite their counterparts", itemList.size(),
                primaryPersistence.getTransferHolderList().size() + secondaryPersistence.getTransferHolderList().size());

        for (OwnedTransferHolder holder : itemList) {
            Assert.assertEquals("The item should be marked as done", PersistenceProvider.STATE_DONE,
                    holder.state);
        }
    }

    @Test(expected = UntrustedClientException.class)
    public void senderFailsToStartTransferIfNotTrusted() throws IOException, InterruptedException,
            ProtocolException
    {
        primarySession.start();

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, clientAddress)) {
            bridge.requestFileTransferStart(groupId, Transfer.Type.OUTGOING);
            secondarySeat.receiveFiles(bridge, groupId);
        } finally {
            primarySession.stop();
        }
    }

    @Test
    public void senderStartsTransferIfTrusted() throws IOException, InterruptedException, ProtocolException,
            PersistenceException
    {
        Client secondaryOnPrimary = primaryPersistence.createClientFor(secondaryPersistence.getClientUid());
        primaryPersistence.sync(secondaryOnPrimary);
        secondaryOnPrimary.setClientTrusted(true);
        primaryPersistence.save(secondaryOnPrimary);

        primarySession.start();

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, clientAddress)) {
            bridge.requestFileTransferStart(groupId, Transfer.Type.OUTGOING);
            secondarySeat.receiveFiles(bridge, groupId);
        }

        primarySession.stop();
    }
}
