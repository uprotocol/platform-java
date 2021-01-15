package org.monora.uprotocol.variant.holder;

import org.monora.uprotocol.core.persistence.PersistenceProvider;
import org.monora.uprotocol.core.transfer.TransferItem;

public class OwnedTransferHolder
{
    public TransferItem item;

    public String clientUid;

    public int state = PersistenceProvider.STATE_PENDING;

    public OwnedTransferHolder(TransferItem item)
    {
        this.item = item;
    }

    public OwnedTransferHolder(TransferItem item, String clientUid)
    {
        this(item);
        this.clientUid = clientUid;
    }
}
