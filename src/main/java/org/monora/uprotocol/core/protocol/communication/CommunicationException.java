package org.monora.uprotocol.core.protocol.communication;

import org.monora.uprotocol.core.network.Device;

/**
 * This is a high-level error that is thrown when communication with a peer fails in some way.
 * <p>
 * The main difference from {@link ProtocolException} is this exception also shows with which client it occurred.
 */
public class CommunicationException extends ProtocolException
{
    public final Device device;

    public CommunicationException(Device device)
    {
        super();
        this.device = device;
    }

    public CommunicationException(Device device, Throwable cause)
    {
        super(cause);
        this.device = device;
    }
}
