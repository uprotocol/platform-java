package org.monora.uprotocol;

import org.junit.Assert;
import org.junit.Test;
import org.monora.uprotocol.core.protocol.ClientType;

public class MethodTest
{
    @Test
    public void clientTypeIsAnyWhenNotKnown()
    {
        String spaceGunType = "SpaceGun";
        ClientType type = ClientType.from(spaceGunType);

        Assert.assertEquals("Protocol value should match", type.getProtocolValue(),
                ClientType.Any.getProtocolValue());
        Assert.assertEquals("Custom protocol values should match", type.getOriginalValue(),
                ClientType.Any.getOriginalValue());
    }
}
