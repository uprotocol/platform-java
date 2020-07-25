package org.monora.uprotocol.persistence;

/**
 * This exception will be thrown when a publish or exists call made on the persistance provider points to a non-existing
 * row.
 */
public class NotFoundException extends PersistenceException
{
    public NotFoundException()
    {
        super();
    }

    public NotFoundException(String message)
    {
        super(message);
    }

    public NotFoundException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public NotFoundException(Throwable cause)
    {
        super(cause);
    }
}
