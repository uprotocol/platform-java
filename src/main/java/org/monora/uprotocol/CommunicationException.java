package org.monora.uprotocol;

public class CommunicationException extends Exception
{
    public CommunicationException()
    {
        super();
    }

    public CommunicationException(String message)
    {
        super(message);
    }

    public CommunicationException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public CommunicationException(Throwable cause)
    {
        super(cause);
    }
}

