package org.monora.uprotocol.variant.holder;

import org.monora.uprotocol.core.network.TransferItem;
import org.monora.uprotocol.core.persistence.PersistenceProvider;

public class OwnedTransferHolder
{
    public TransferItem item;

    public String deviceUid;

    public int state = PersistenceProvider.STATE_PENDING;

    public OwnedTransferHolder(TransferItem item)
    {
        this.item = item;
    }

    public OwnedTransferHolder(TransferItem item, String deviceUid)
    {
        this(item);
        this.deviceUid = deviceUid;
    }
}
