package org.monora.uprotocol.core.io;

import org.jetbrains.annotations.NotNull;
import org.monora.uprotocol.core.protocol.Client;
import org.monora.uprotocol.core.protocol.ClientAddress;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;

/**
 * Thrown when connecting to bunch of connections and each one of them fails.
 */
public class DefectiveAddressListException extends IOException
{
    public final @NotNull List<@NotNull IOException> underlyingExceptionList;

    public final @NotNull List<@NotNull InetSocketAddress> addressList;

    /**
     * Creates a new instance.
     *
     * @param underlyingExceptionList Exceptions thrown when connecting to each address.
     * @param addressList             The addresses that failed in the order of their exception.
     */
    public DefectiveAddressListException(@NotNull List<@NotNull IOException> underlyingExceptionList,
                                         @NotNull List<@NotNull InetSocketAddress> addressList)
    {
        this.underlyingExceptionList = Collections.unmodifiableList(underlyingExceptionList);
        this.addressList = Collections.unmodifiableList(addressList);
    }
}
