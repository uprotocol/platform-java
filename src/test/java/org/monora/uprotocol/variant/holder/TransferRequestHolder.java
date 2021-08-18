package org.monora.uprotocol.variant.holder;

import org.jetbrains.annotations.NotNull;
import org.monora.uprotocol.core.transfer.TransferItem;

import java.util.Collections;
import java.util.List;

public class TransferRequestHolder
{
    public final long groupId;

    public final @NotNull List<@NotNull TransferItem> list;

    public TransferRequestHolder(long groupId, @NotNull List<@NotNull TransferItem> list)
    {
        this.groupId = groupId;
        this.list = Collections.unmodifiableList(list);
    }
}
