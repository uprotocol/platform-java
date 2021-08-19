package org.monora.uprotocol.variant;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.monora.uprotocol.core.CommunicationBridge;
import org.monora.uprotocol.core.io.StreamDescriptor;
import org.monora.uprotocol.core.protocol.Direction;
import org.monora.uprotocol.core.protocol.communication.ProtocolException;
import org.monora.uprotocol.core.transfer.TransferItem;
import org.monora.uprotocol.variant.holder.TransferHolder;
import org.monora.uprotocol.variant.holder.TransferRequestHolder;
import org.monora.uprotocol.variant.test.DefaultTestBase;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TrustBasedTransferTest extends DefaultTestBase
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

    @Test
    public void startsFileTransferOnRequestTest() throws IOException, InterruptedException, ProtocolException,
            CertificateException
    {
        primarySession.start();
        primarySeat.startTransferByDefault = true;

        final List<TransferItem> itemList = new ArrayList<>();
        itemList.add(demoTransferItem1);
        itemList.add(demoTransferItem2);

        try (CommunicationBridge bridge = openConnection(secondaryPersistence, clientAddress)) {
            if (bridge.requestFileTransfer(groupId, itemList, null)) {
                secondarySeat.beginFileTransfer(bridge, primaryPersistence.getClient(), groupId, Direction.Outgoing);
            } else {
                Assert.fail("The result should be true.");
            }
        } finally {
            primarySession.stop();
        }

        for (TransferHolder remoteTransferHolder : primaryPersistence.getTransferHolderList()) {
            Assert.assertEquals("The item should be marked as done", TransferItem.State.Done,
                    remoteTransferHolder.state);
        }
    }

    @Test
    public void acquaintanceGuidedTransferRequestTest() throws IOException, InterruptedException, ProtocolException,
            CertificateException
    {
        secondarySession.start();

        final List<TransferItem> itemList = new ArrayList<>();
        itemList.add(demoTransferItem1);
        itemList.add(demoTransferItem2);

        secondarySeat.transferRequestOnAcquaintance = new TransferRequestHolder(groupId, itemList);

        try (CommunicationBridge bridge = openConnection(primaryPersistence, clientAddress)) {
            bridge.requestAcquaintance(primarySeat, Direction.Incoming);
        } finally {
            secondarySession.stop();
        }

        final List<@NotNull TransferHolder> primaryTransferHolderList = Collections.unmodifiableList(
                primaryPersistence.getTransferHolderList());
        final List<@NotNull TransferHolder> secondaryTransferHolderList = Collections.unmodifiableList(
                secondaryPersistence.getTransferHolderList());

        Assert.assertTrue("The secondary persistence provider should have transfers.",
                secondaryTransferHolderList.size() > 0);
        Assert.assertEquals("The content size of both the primary and secondary providers should match",
                primaryTransferHolderList.size(), secondaryTransferHolderList.size());
    }

    @Test
    public void acquaintanceGuidedTransferTest() throws IOException, InterruptedException, ProtocolException,
            CertificateException
    {
        secondarySession.start();

        final List<TransferItem> itemList = new ArrayList<>();
        itemList.add(demoTransferItem1);
        itemList.add(demoTransferItem2);

        secondarySeat.transferRequestOnAcquaintance = new TransferRequestHolder(groupId, itemList);
        primarySeat.startTransferByDefault = true;

        try (CommunicationBridge bridge = openConnection(primaryPersistence, clientAddress)) {
            bridge.requestAcquaintance(primarySeat, Direction.Incoming);
        } finally {
            secondarySession.stop();
        }

        for (TransferHolder remoteTransferHolder : primaryPersistence.getTransferHolderList()) {
            Assert.assertEquals("The item should be marked as done", TransferItem.State.Done,
                    remoteTransferHolder.state);
        }
    }
}
