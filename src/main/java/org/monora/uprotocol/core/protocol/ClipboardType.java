package org.monora.uprotocol.core.protocol;

import org.jetbrains.annotations.NotNull;
import org.monora.uprotocol.core.spec.v1.Keyword;

/**
 * Denotes how the remote should treat the {@link Keyword#CLIPBOARD_CONTENT}.
 */
public enum ClipboardType
{
    /**
     * The content is a plain text.
     */
    Text(Keyword.CLIPBOARD_TYPE_TEXT),

    /**
     * The content is a URL.
     */
    Link(Keyword.CLIPBOARD_TYPE_LINK);

    /**
     * The value that the protocol specifies which is different from the platform-based enum value.
     */
    public final @NotNull String protocolValue;

    ClipboardType(@NotNull String protocolValue)
    {
        this.protocolValue = protocolValue;
    }

    public static @NotNull ClipboardType from(String value)
    {
        for (ClipboardType clipboardType : values())
            if (clipboardType.protocolValue.equals(value))
                return clipboardType;

        throw new IllegalArgumentException("Unknown type: " + value);
    }
}
