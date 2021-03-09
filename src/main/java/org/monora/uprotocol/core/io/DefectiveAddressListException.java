package org.monora.uprotocol.core.io;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;

/**
 * Thrown when connecting to bunch of connections and each one of them fails.
 */
public class DefectiveAddressListException extends IOException
{
    public final List<IOException> underlyingExceptionList;

    public final List<InetAddress> addressList;

    /**
     * Creates a new instance.
     *
     * @param underlyingExceptionList Exceptions thrown when connecting to each address.
     * @param addressList             The addresses that failed in the order of their exception.
     */
    public DefectiveAddressListException(List<IOException> underlyingExceptionList, List<InetAddress> addressList)
    {
        this.underlyingExceptionList = Collections.unmodifiableList(underlyingExceptionList);
        this.addressList = Collections.unmodifiableList(addressList);
    }
}
