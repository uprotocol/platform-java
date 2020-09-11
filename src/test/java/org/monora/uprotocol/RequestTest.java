package org.monora.uprotocol;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.monora.uprotocol.core.CommunicationBridge;
import org.monora.uprotocol.core.TransportSeat;
import org.monora.uprotocol.core.TransportSession;
import org.monora.uprotocol.core.network.Device;
import org.monora.uprotocol.core.network.DeviceAddress;
import org.monora.uprotocol.core.persistence.PersistenceException;
import org.monora.uprotocol.core.persistence.PersistenceProvider;
import org.monora.uprotocol.core.protocol.ConnectionProvider;
import org.monora.uprotocol.core.protocol.communication.CommunicationException;
import org.monora.uprotocol.variant.DefaultConnectionProvider;
import org.monora.uprotocol.variant.DefaultPersistenceProvider;
import org.monora.uprotocol.variant.DefaultTransportSeat;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class RequestTest
{
    private final ConnectionProvider connectionProvider = new DefaultConnectionProvider();
    private final PersistenceProvider persistenceProvider = new DefaultPersistenceProvider();
    private final TransportSeat transportSeat = new DefaultTransportSeat(persistenceProvider);
    private final TransportSession transportSession = new TransportSession(persistenceProvider, transportSeat);
    private DeviceAddress deviceAddress;

    @Before
    public void setUp() throws UnknownHostException
    {
        deviceAddress = persistenceProvider.createDeviceAddressFor(InetAddress.getLocalHost());
    }

    @Test
    public void requestAcquaintanceTest() throws IOException, InterruptedException, CommunicationException,
            PersistenceException
    {
        transportSession.start();

        try (CommunicationBridge bridge = CommunicationBridge.connect(connectionProvider, persistenceProvider,
                deviceAddress, null, 0)) {
            Assert.assertTrue("Remote should send a positive message.", bridge.requestAcquaintance());

            Device persistentDevice = persistenceProvider.createDeviceFor(bridge.getDevice().uid);
            persistenceProvider.sync(persistentDevice);

            Assert.assertEquals("Devices should be same.", bridge.getDevice(), persistentDevice);
            Assert.assertEquals("Devices should have the same username.", bridge.getDevice().username,
                    persistentDevice.username);
        } finally {
            transportSession.stop();
        }
    }
}
