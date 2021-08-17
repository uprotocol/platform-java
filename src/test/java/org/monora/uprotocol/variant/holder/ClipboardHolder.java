package org.monora.uprotocol.variant.holder;

import org.jetbrains.annotations.NotNull;
import org.monora.uprotocol.core.protocol.ClipboardType;

public class ClipboardHolder
{
    public final @NotNull String content;

    public final @NotNull ClipboardType type;

    public ClipboardHolder(@NotNull String content, @NotNull ClipboardType type)
    {
        this.content = content;
        this.type = type;
    }
}
