package org.monora.uprotocol;

import org.junit.Assert;
import org.junit.Test;
import org.monora.uprotocol.core.CommunicationBridge;
import org.monora.uprotocol.variant.test.DefaultTestBase;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class RealWorldTest extends DefaultTestBase
{
    @Test
    public void connectToZen() throws UnknownHostException
    {
        final InetAddress inetAddress = InetAddress.getByName("zen.omg.cloudns.cl");

        System.out.println(inetAddress.getHostAddress());

        try (CommunicationBridge bridge = openConnection(primaryPersistence, inetAddress)) {
            Assert.assertTrue(bridge.requestAcquaintance());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
