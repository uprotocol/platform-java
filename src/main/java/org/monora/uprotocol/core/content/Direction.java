package org.monora.uprotocol.core.content;

import org.jetbrains.annotations.NotNull;
import org.monora.uprotocol.core.spec.v1.Keyword;

/**
 * Whether a content is received or sent.
 */
public enum Direction
{
    /**
     * The content is incoming
     */
    Incoming(Keyword.DIRECTION_INCOMING),

    /**
     * The content is outgoing.
     */
    Outgoing(Keyword.DIRECTION_OUTGOING);

    /**
     * The value that the protocol specifies which is different from the platform-based enum value.
     */
    public final String protocolValue;

    Direction(String protocolValue)
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
