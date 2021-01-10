package org.monora.uprotocol.variant.persistence;

import org.monora.uprotocol.core.protocol.Client;

public class TextTransferItem
{
    public final Client sender;

    public final String text;

    public final long time;

    public TextTransferItem(Client sender, String text, long time)
    {
        this.sender = sender;
        this.text = text;
        this.time = time;
    }
}
