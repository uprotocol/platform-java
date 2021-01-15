package org.monora.uprotocol.variant.test;

import org.monora.uprotocol.core.CommunicationBridge;
import org.monora.uprotocol.core.TransportSession;
import org.monora.uprotocol.core.persistence.PersistenceProvider;
import org.monora.uprotocol.core.protocol.ClientAddress;
import org.monora.uprotocol.core.protocol.ConnectionFactory;
import org.monora.uprotocol.core.protocol.communication.ProtocolException;
import org.monora.uprotocol.variant.DefaultConnectionFactory;
import org.monora.uprotocol.variant.DefaultTransportSeat;
import org.monora.uprotocol.variant.persistence.BasePersistenceProvider;
import org.monora.uprotocol.variant.persistence.PrimaryPersistenceProvider;
import org.monora.uprotocol.variant.persistence.SecondaryPersistenceProvider;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;

public class DefaultTestBase
{
    protected final ConnectionFactory connectionFactory = new DefaultConnectionFactory();
    protected final BasePersistenceProvider primaryPersistence = new PrimaryPersistenceProvider();
    protected final BasePersistenceProvider secondaryPersistence = new SecondaryPersistenceProvider();
    protected final DefaultTransportSeat primarySeat = new DefaultTransportSeat(primaryPersistence);
    protected final DefaultTransportSeat secondarySeat = new DefaultTransportSeat(secondaryPersistence);
    protected final TransportSession primarySession = new TransportSession(connectionFactory, primaryPersistence,
            primarySeat);
    protected final TransportSession secondarySession = new TransportSession(connectionFactory, secondaryPersistence,
            secondarySeat);
    protected final ClientAddress clientAddress;

    public DefaultTestBase()
    {
        try {
            clientAddress = primaryPersistence.createClientAddressFor(InetAddress.getLocalHost());
        } catch (UnknownHostException e) {
            throw new RuntimeException("Could not gather the loopback address");
        }
    }

    protected CommunicationBridge openConnection(PersistenceProvider persistenceProvider, ClientAddress address)
            throws IOException, ProtocolException, CertificateException
    {
        return CommunicationBridge.connect(connectionFactory, persistenceProvider, address, null, 0);
    }
}
