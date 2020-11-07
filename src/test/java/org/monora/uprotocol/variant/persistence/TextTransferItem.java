package org.monora.uprotocol.variant.persistence;

import org.monora.uprotocol.core.network.Device;

public class TextTransferItem
{
    public final Device sender;

    public final String text;

    public final long time;

    public TextTransferItem(Device sender, String text, long time)
    {
        this.sender = sender;
        this.text = text;
        this.time = time;
    }
}
