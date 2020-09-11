package org.monora.uprotocol.core.persistence;

/**
 * This exception is thrown when a target with a specifier is queried but not exist.
 */
public class PersistenceException extends Exception
{
    /**
     * Create a new instance.
     *
     * @param message The details of the error.
     */
    public PersistenceException(String message)
    {
        super(message);
    }
}
