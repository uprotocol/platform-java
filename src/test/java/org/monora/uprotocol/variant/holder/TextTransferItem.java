package org.monora.uprotocol.variant.holder;

import org.jetbrains.annotations.NotNull;
import org.monora.uprotocol.core.protocol.Client;

public class TextTransferItem
{
    public final @NotNull Client sender;

    public final @NotNull String text;

    public final long time;

    public TextTransferItem(@NotNull Client sender, @NotNull String text, long time)
    {
        this.sender = sender;
        this.text = text;
        this.time = time;
    }
}
