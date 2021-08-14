package org.monora.uprotocol.core.protocol.communication;

import org.jetbrains.annotations.NotNull;
import org.monora.uprotocol.core.persistence.PersistenceProvider;
import org.monora.uprotocol.core.protocol.Client;

/**
 * Thrown when an SSL-related error occurs and is considered a result of invalid credentials.
 */
public class CredentialsException extends SecurityException
{
    /**
     * Will be true if {@link PersistenceProvider#hasRequestForInvalidationOfCredentials(String)} returned false and
     * {@link PersistenceProvider#saveRequestForInvalidationOfCredentials(String)} was invoked.
     */
    public final boolean firstTime;

    /**
     * Create a new instance.
     *
     * @param client    With which the error occurred.
     * @param cause     Of the issue.
     * @param firstTime Will be true if {@link PersistenceProvider#hasRequestForInvalidationOfCredentials(String)}
     *                  returned false and {@link PersistenceProvider#saveRequestForInvalidationOfCredentials(String)}
     *                  was invoked.
     */
    public CredentialsException(@NotNull Client client, @NotNull Throwable cause, boolean firstTime)
    {
        super(client, cause);
        this.firstTime = firstTime;
    }
}
