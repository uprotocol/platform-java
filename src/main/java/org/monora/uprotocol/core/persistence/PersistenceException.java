package org.monora.uprotocol.core.persistence;

/**
 * This exception is thrown when a target with a specifier is queried but not exist.
 */
public class PersistenceException extends Exception
{
    public PersistenceException(String message)
    {
        super(message);
    }
}
