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
import org.monora.uprotocol.core.protocol.ConnectionProvider;
import org.monora.uprotocol.core.protocol.communication.CommunicationException;
import org.monora.uprotocol.variant.DefaultConnectionProvider;
import org.monora.uprotocol.variant.DefaultTransportSeat;
import org.monora.uprotocol.variant.persistence.PrimaryPersistenceProvider;
import org.monora.uprotocol.variant.persistence.SecondaryPersistenceProvider;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class RequestTest
{
    private final ConnectionProvider connectionProvider = new DefaultConnectionProvider();
    private final PersistenceProvider primaryPersistence = new PrimaryPersistenceProvider();
    private final PersistenceProvider secondaryPersistence = new SecondaryPersistenceProvider();
    private final TransportSeat transportSeat = new DefaultTransportSeat(primaryPersistence);
    private final TransportSession transportSession = new TransportSession(primaryPersistence, transportSeat);
    private DeviceAddress deviceAddress;

    @Before
    public void setUp() throws UnknownHostException
    {
        deviceAddress = primaryPersistence.createDeviceAddressFor(InetAddress.getLocalHost());
    }

    @Test
    public void requestAcquaintanceTest() throws IOException, InterruptedException, CommunicationException,
            PersistenceException
    {
        transportSession.start();

        try (CommunicationBridge bridge = CommunicationBridge.connect(connectionProvider, secondaryPersistence,
                deviceAddress, null, 0)) {
            Assert.assertTrue("Remote should send a positive message.", bridge.requestAcquaintance());

            Device persistentDevice = secondaryPersistence.createDeviceFor(bridge.getDevice().uid);
            secondaryPersistence.sync(persistentDevice);

            Assert.assertEquals("Devices should be same.", bridge.getDevice(), persistentDevice);
            Assert.assertEquals("Devices should have the same username.", bridge.getDevice().username,
                    persistentDevice.username);
        } finally {
            transportSession.stop();
        }
    }

    @Test
    public void requestFileTransferTest() throws IOException, InterruptedException, CommunicationException
    {
        transportSession.start();

        final List<TransferItem> transferItemList = new ArrayList<>();
        final long transferId = 1;

        transferItemList.add(secondaryPersistence.createTransferItemFor(transferId, 1, "1.jpg",
                "image/jpeg", 0, null, TransferItem.Type.OUTGOING));
        transferItemList.add(secondaryPersistence.createTransferItemFor(transferId, 2, "2.jpg",
                "image/jpeg", 0, null, TransferItem.Type.OUTGOING));
        transferItemList.add(secondaryPersistence.createTransferItemFor(transferId, 3, "3.jpg",
                "image/jpeg", 0, null, TransferItem.Type.OUTGOING));

        try (CommunicationBridge bridge = CommunicationBridge.connect(connectionProvider, secondaryPersistence,
                deviceAddress, null, 0)) {
            Assert.assertTrue("The request should be successful", bridge.requestFileTransfer(transferId,
                    transferItemList));
        } finally {
            transportSession.stop();
        }
    }
}
