package org.monora.uprotocol.core.persistence;

/**
 * A convenience class that can be invoked before persistence operations to satisfy relational fields.
 */
public interface OnPrepareListener
{
    /**
     * Invoked before persistence operations.
     */
    void onPrepare();
}
