package org.monora.uprotocol.variant.test;

import org.monora.uprotocol.core.CommunicationBridge;
import org.monora.uprotocol.core.TransportSeat;
import org.monora.uprotocol.core.TransportSession;
import org.monora.uprotocol.core.network.DeviceAddress;
import org.monora.uprotocol.core.persistence.PersistenceProvider;
import org.monora.uprotocol.core.protocol.ConnectionProvider;
import org.monora.uprotocol.core.protocol.communication.CommunicationException;
import org.monora.uprotocol.variant.DefaultConnectionProvider;
import org.monora.uprotocol.variant.DefaultTransportSeat;
import org.monora.uprotocol.variant.persistence.BasePersistenceProvider;
import org.monora.uprotocol.variant.persistence.PrimaryPersistenceProvider;
import org.monora.uprotocol.variant.persistence.SecondaryPersistenceProvider;

import java.io.IOException;

public class DefaultTestBase
{
    protected final ConnectionProvider connectionProvider = new DefaultConnectionProvider();
    protected final BasePersistenceProvider primaryPersistence = new PrimaryPersistenceProvider();
    protected final BasePersistenceProvider secondaryPersistence = new SecondaryPersistenceProvider();
    protected final DefaultTransportSeat primarySeat = new DefaultTransportSeat(primaryPersistence);
    protected final DefaultTransportSeat secondarySeat = new DefaultTransportSeat(secondaryPersistence);
    protected final TransportSession primarySession = new TransportSession(primaryPersistence, primarySeat);
    protected final TransportSession secondarySession = new TransportSession(secondaryPersistence, secondarySeat);

    protected CommunicationBridge openConnection(PersistenceProvider persistenceProvider, DeviceAddress address)
            throws IOException, CommunicationException
    {
        return CommunicationBridge.connect(connectionProvider, persistenceProvider, address, null, 0);
    }
}
