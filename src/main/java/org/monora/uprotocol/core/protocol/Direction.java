package org.monora.uprotocol.core.protocol;

import org.jetbrains.annotations.NotNull;
import org.monora.uprotocol.core.spec.v1.Keyword;

/**
 * Denotes the direction at which the operation is going to occur.
 * <p>
 * It will be relative most of the time, meaning, a receiver will see it as incoming whereas the identical data
 * on the opposite side will be outgoing.
 */
public enum Direction
{
    /**
     * The item is incoming
     */
    Incoming(Keyword.DIRECTION_INCOMING),

    /**
     * The item is outgoing.
     */
    Outgoing(Keyword.DIRECTION_OUTGOING);

    /**
     * The value that the protocol specifies which is different from the platform-based enum value.
     */
    public final @NotNull String protocolValue;

    Direction(@NotNull String protocolValue)
    {
        this.protocolValue = protocolValue;
    }

    public static @NotNull Direction from(String value)
    {
        for (Direction direction : values())
            if (direction.protocolValue.equals(value))
                return direction;

        throw new IllegalArgumentException("Unknown type: " + value);
    }
}
