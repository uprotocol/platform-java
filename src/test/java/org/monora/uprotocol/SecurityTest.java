package org.monora.uprotocol;

import org.junit.Assert;
import org.junit.Test;
import org.monora.uprotocol.core.CommunicationBridge;
import org.monora.uprotocol.core.network.Device;
import org.monora.uprotocol.core.persistence.PersistenceException;
import org.monora.uprotocol.core.protocol.communication.CommunicationException;
import org.monora.uprotocol.variant.test.DefaultTestBase;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;

public class SecurityTest extends DefaultTestBase
{
    @Test(expected = SSLHandshakeException.class)
    public void differentCertificateFailsTest() throws IOException, InterruptedException, CommunicationException,
            PersistenceException
    {
        primarySession.start();

        try (CommunicationBridge bridge = CommunicationBridge.connect(connectionFactory, secondaryPersistence,
                deviceAddress, null, 0)) {
            Assert.assertTrue("Remote should send a positive message.", bridge.requestAcquaintance());

            Device persistentDevice = secondaryPersistence.createDeviceFor(bridge.getDevice().uid);
            secondaryPersistence.sync(persistentDevice);
        }

        primaryPersistence.regenerateSecrets();
        primaryPersistence.remove(secondaryPersistence.getDevice());

        try (CommunicationBridge bridge = CommunicationBridge.connect(connectionFactory, secondaryPersistence,
                deviceAddress, null, 0)) {
            Assert.assertTrue("Remote should send a positive message.", bridge.requestAcquaintance());

            Device persistentDevice = secondaryPersistence.createDeviceFor(bridge.getDevice().uid);
            secondaryPersistence.sync(persistentDevice);
        } finally {
            primarySession.stop();
        }
    }
}
