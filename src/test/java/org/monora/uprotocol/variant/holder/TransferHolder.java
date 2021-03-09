package org.monora.uprotocol.variant.holder;

import org.jetbrains.annotations.NotNull;
import org.monora.uprotocol.core.persistence.PersistenceProvider;
import org.monora.uprotocol.core.transfer.TransferItem;

public class TransferHolder
{
    public @NotNull TransferItem item;

    public @NotNull String clientUid;

    public int state = PersistenceProvider.STATE_PENDING;

    public TransferHolder(@NotNull TransferItem item, @NotNull String clientUid)
    {
        this.item = item;
        this.clientUid = clientUid;
    }
}
