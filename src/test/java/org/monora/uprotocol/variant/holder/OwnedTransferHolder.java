package org.monora.uprotocol.variant.holder;

import org.monora.uprotocol.core.transfer.Transfer;
import org.monora.uprotocol.core.persistence.PersistenceProvider;

public class OwnedTransferHolder
{
    public Transfer item;

    public String clientUid;

    public int state = PersistenceProvider.STATE_PENDING;

    public OwnedTransferHolder(Transfer item)
    {
        this.item = item;
    }

    public OwnedTransferHolder(Transfer item, String clientUid)
    {
        this(item);
        this.clientUid = clientUid;
    }
}
