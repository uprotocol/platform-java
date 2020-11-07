package org.monora.uprotocol;

import org.junit.After;
import org.junit.Before;
import org.monora.uprotocol.core.TransportSeat;
import org.monora.uprotocol.core.TransportSession;
import org.monora.uprotocol.core.network.DeviceAddress;
import org.monora.uprotocol.core.persistence.PersistenceProvider;
import org.monora.uprotocol.core.protocol.ConnectionProvider;
import org.monora.uprotocol.variant.DefaultConnectionProvider;
import org.monora.uprotocol.variant.DefaultTransportSeat;
import org.monora.uprotocol.variant.persistence.PrimaryPersistenceProvider;

import java.io.IOException;

public class TransferTest
{
    private final ConnectionProvider connectionProvider = new DefaultConnectionProvider();
    private final PersistenceProvider persistenceProvider = new PrimaryPersistenceProvider();
    private final TransportSeat transportSeat = new DefaultTransportSeat(persistenceProvider);
    private final TransportSession transportSession = new TransportSession(persistenceProvider, transportSeat);
    private DeviceAddress deviceAddress;

    @Before
    public void setUp() throws IOException, InterruptedException
    {
        transportSession.start();
    }

    @After
    public void tearUp() throws InterruptedException
    {
        transportSession.stop();
    }
}
