package org.monora.uprotocol.core.protocol.communication;

import org.monora.uprotocol.core.network.Device;

public class ClientCommunicationException extends CommunicationException
{
    public final Device device;

    public ClientCommunicationException(Device device)
    {
        super();
        this.device = device;
    }

    public ClientCommunicationException(Device device, String message)
    {
        super(message);
        this.device = device;
    }

    public ClientCommunicationException(Device device, String message, Throwable cause)
    {
        super(message, cause);
        this.device = device;
    }

    public ClientCommunicationException(Device device, Throwable cause)
    {
        super(cause);
        this.device = device;
    }
}
