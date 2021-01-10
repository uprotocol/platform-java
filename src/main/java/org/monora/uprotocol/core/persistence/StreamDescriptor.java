package org.monora.uprotocol.core.persistence;

import org.monora.uprotocol.core.transfer.Transfer;

/**
 * This represents an input or output stream depending on the use case.
 * <p>
 * The difference from {@link Transfer} is that this keeps what the file is
 * whereas {@link Transfer} keeps what the file was before it was exchanged
 * between networked clients.
 */
public interface StreamDescriptor
{
    /**
     * Produce the total length of this descriptor.
     * <p>
     * This should return 0 or a positive value. Negative values will not be considered valid.
     *
     * @return The total byte length of this descriptor.
     */
    long length();
}
