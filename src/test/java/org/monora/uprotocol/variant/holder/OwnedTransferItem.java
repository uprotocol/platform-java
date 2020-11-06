package org.monora.uprotocol.variant.holder;

import org.monora.uprotocol.core.network.TransferItem;
import org.monora.uprotocol.core.persistence.PersistenceProvider;

public class OwnedTransferItem
{
    public TransferItem item;

    public String deviceId;

    public int state = PersistenceProvider.STATE_PENDING;

    public OwnedTransferItem(TransferItem item)
    {
        this.item = item;
    }
}
