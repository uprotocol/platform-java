package org.monora.uprotocol;

import org.monora.uprotocol.persistence.object.Device;

public class DifferentClientException extends CommunicationException
{
    public Device expected;
    public Device got;

    public DifferentClientException(Device expected, Device got)
    {
        super();
        this.expected = expected;
        this.got = got;
    }
}
