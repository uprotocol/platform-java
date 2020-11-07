package org.monora.uprotocol;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.monora.uprotocol.core.CommunicationBridge;
import org.monora.uprotocol.core.TransportSeat;
import org.monora.uprotocol.core.TransportSession;
import org.monora.uprotocol.core.network.Device;
import org.monora.uprotocol.core.network.DeviceAddress;
import org.monora.uprotocol.core.network.TransferItem;
import org.monora.uprotocol.core.persistence.PersistenceException;
import org.monora.uprotocol.core.persistence.PersistenceProvider;
import org.monora.uprotocol.core.persistence.StreamDescriptor;
import org.monora.uprotocol.core.protocol.ConnectionProvider;
import org.monora.uprotocol.core.protocol.communication.CommunicationException;
import org.monora.uprotocol.core.protocol.communication.NotTrustedException;
import org.monora.uprotocol.variant.DefaultConnectionProvider;
import org.monora.uprotocol.variant.DefaultTransportSeat;
import org.monora.uprotocol.variant.holder.MemoryStreamDescriptor;
import org.monora.uprotocol.variant.holder.OwnedTransferHolder;
import org.monora.uprotocol.variant.persistence.BasePersistenceProvider;
import org.monora.uprotocol.variant.persistence.PrimaryPersistenceProvider;
import org.monora.uprotocol.variant.persistence.SecondaryPersistenceProvider;

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
public class TransferTest
{
    private final ConnectionProvider connectionProvider = new DefaultConnectionProvider();
    private final BasePersistenceProvider primaryPersistence = new PrimaryPersistenceProvider();
    private final BasePersistenceProvider secondaryPersistence = new SecondaryPersistenceProvider();
    private final TransportSeat primarySeat = new DefaultTransportSeat(primaryPersistence);
    private final TransportSeat secondarySeat = new DefaultTransportSeat(secondaryPersistence);
    private final TransportSession primarySession = new TransportSession(primaryPersistence, primarySeat);
    private final TransportSession secondarySession = new TransportSession(secondaryPersistence, secondarySeat);

    private DeviceAddress deviceAddress;
    private TransferItem demoTransfer1;
    private TransferItem demoTransfer2;

    private final static byte[] data1 = "This is the first demo data".getBytes();
    private final static byte[] data2 = "This is the second demo data".getBytes();
    private final long transferId = 1;

    private CommunicationBridge openConnection(PersistenceProvider persistenceProvider) throws IOException,
            CommunicationException
    {
        return CommunicationBridge.connect(connectionProvider, persistenceProvider, deviceAddress, null, 0);
    }

    @Before
    public void setUp() throws IOException
    {
        demoTransfer1 = secondaryPersistence.createTransferItemFor(transferId, 1, "File1",
                "text/plain", data1.length, null, TransferItem.Type.OUTGOING);
        demoTransfer2 = secondaryPersistence.createTransferItemFor(transferId, 2, "File2",
                "text/plain", data2.length, null, TransferItem.Type.OUTGOING);

        StreamDescriptor descriptor1 = secondaryPersistence.getDescriptorFor(demoTransfer1);
        StreamDescriptor descriptor2 = secondaryPersistence.getDescriptorFor(demoTransfer2);

        secondaryPersistence.openOutputStream(descriptor1).write(data1);
        secondaryPersistence.openOutputStream(descriptor2).write(data2);

        deviceAddress = primaryPersistence.createDeviceAddressFor(InetAddress.getLocalHost());
    }

    @Before
    public void setUpInitialTransferItems() throws IOException, InterruptedException, CommunicationException
    {
        primarySession.start();

        final List<TransferItem> itemList = new ArrayList<>();
        itemList.add(demoTransfer1);
        itemList.add(demoTransfer2);

        try (CommunicationBridge bridge = openConnection(secondaryPersistence)) {
            bridge.requestFileTransfer(transferId, itemList);
        }

        primarySession.stop();
    }

    @Test
    public void sendFilesTest() throws IOException, InterruptedException, CommunicationException
    {
        secondarySession.start();

        try (CommunicationBridge bridge = openConnection(primaryPersistence)) {
            Assert.assertTrue("The result should be positive", bridge.requestFileTransferStart(transferId,
                    TransferItem.Type.INCOMING));

            primarySeat.receiveFiles(bridge, transferId);
        }

        secondarySession.stop();

        // Compare the received data with the original data.
        List<MemoryStreamDescriptor> primaryList = primaryPersistence.getStreamDescriptorList();
        List<MemoryStreamDescriptor> secondaryList = secondaryPersistence.getStreamDescriptorList();

        for (MemoryStreamDescriptor descriptor : primaryList) {
            boolean dataMatched = false;
            for (MemoryStreamDescriptor secondaryDescriptor : secondaryList) {
                if (descriptor.transferItem.transferId == secondaryDescriptor.transferItem.transferId
                        && descriptor.transferItem.id == secondaryDescriptor.transferItem.id) {
                    Assert.assertEquals("The data should match", descriptor.data.toString(),
                            secondaryDescriptor.data.toString());
                    dataMatched = true;
                }
            }

            Assert.assertTrue("There should not be a missing data", dataMatched);
        }
    }

    @Test
    public void flagItemAsDoneTest() throws IOException, InterruptedException, CommunicationException
    {
        secondarySession.start();

        try (CommunicationBridge bridge = openConnection(primaryPersistence)) {
            bridge.requestFileTransferStart(transferId, TransferItem.Type.INCOMING);
            primarySeat.receiveFiles(bridge, transferId);
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

    @Test(expected = NotTrustedException.class)
    public void senderFailsToStartTransferIfNotTrusted() throws IOException, InterruptedException,
            CommunicationException
    {
        primarySession.start();

        try (CommunicationBridge bridge = openConnection(secondaryPersistence)) {
            bridge.requestFileTransferStart(transferId, TransferItem.Type.OUTGOING);
            secondarySeat.receiveFiles(bridge, transferId);
        } finally {
            primarySession.stop();
        }
    }

    @Test
    public void senderStartsTransferIfTrusted() throws IOException, InterruptedException, CommunicationException,
            PersistenceException
    {
        Device secondaryOnPrimary = primaryPersistence.createDeviceFor(secondaryPersistence.getDeviceUid());
        primaryPersistence.sync(secondaryOnPrimary);
        secondaryOnPrimary.isTrusted = true;
        primaryPersistence.save(secondaryOnPrimary);

        primarySession.start();

        try (CommunicationBridge bridge = openConnection(secondaryPersistence)) {
            bridge.requestFileTransferStart(transferId, TransferItem.Type.OUTGOING);
            secondarySeat.receiveFiles(bridge, transferId);
        }

        primarySession.stop();
    }
}
