package org.monora.uprotocol.persistence;

import java.io.IOException;

/**
 * This exception type will be thrown when altering the persistence provider ends with an error. Because persistence
 * in this library depends on a parent implementation, it tries to be as less opinionated as possible.
 */
public class PersistenceException extends IOException
{
    public PersistenceException()
    {
        super();
    }

    public PersistenceException(String message)
    {
        super(message);
    }

    public PersistenceException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public PersistenceException(Throwable cause)
    {
        super(cause);
    }
}
