package org.monora.uprotocol.variant;

public class CustomPortConnectionFactory extends DefaultConnectionFactory
{
    private final int port;

    public CustomPortConnectionFactory(int customPort) {
        this.port = customPort;
    }

    @Override
    public int getServicePort()
    {
        return port;
    }
}
