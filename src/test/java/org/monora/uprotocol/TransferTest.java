package org.monora.uprotocol;

import org.junit.Before;
import org.junit.Test;
import org.monora.uprotocol.core.CommunicationBridge;
import org.monora.uprotocol.core.TransportSeat;
import org.monora.uprotocol.core.TransportSession;
import org.monora.uprotocol.core.network.DeviceAddress;
import org.monora.uprotocol.core.network.TransferItem;
import org.monora.uprotocol.core.persistence.PersistenceProvider;
import org.monora.uprotocol.core.persistence.StreamDescriptor;
import org.monora.uprotocol.core.protocol.ConnectionProvider;
import org.monora.uprotocol.core.protocol.communication.CommunicationException;
import org.monora.uprotocol.variant.DefaultConnectionProvider;
import org.monora.uprotocol.variant.DefaultTransportSeat;
import org.monora.uprotocol.variant.persistence.PrimaryPersistenceProvider;
import org.monora.uprotocol.variant.persistence.SecondaryPersistenceProvider;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class TransferTest
{
    private final ConnectionProvider connectionProvider = new DefaultConnectionProvider();
    private final PersistenceProvider primaryPersistence = new PrimaryPersistenceProvider();
    private final PersistenceProvider secondaryPersistence = new SecondaryPersistenceProvider();

    private TransportSeat transportSeat;
    private TransportSession transportSession;
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

    private void startSession(PersistenceProvider persistenceProvider) throws IOException, InterruptedException
    {
        transportSeat = new DefaultTransportSeat(persistenceProvider);
        transportSession = new TransportSession(persistenceProvider, transportSeat);

        transportSession.start();
    }

    private void stopSession() throws InterruptedException
    {
        transportSession.stop();
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
        startSession(primaryPersistence);

        final List<TransferItem> itemList = new ArrayList<>();
        itemList.add(demoTransfer1);
        itemList.add(demoTransfer2);

        try (CommunicationBridge bridge = openConnection(secondaryPersistence)) {
            bridge.requestFileTransfer(transferId, itemList);
        }

        stopSession();
    }

    @Test
    public void sendFilesTest() throws IOException, InterruptedException, CommunicationException
    {
        startSession(secondaryPersistence);

        try (CommunicationBridge bridge = openConnection(primaryPersistence)) {
            bridge.requestFileTransferStart(transferId, TransferItem.Type.INCOMING);
        }

        stopSession();
    }
}
