package org.monora.uprotocol.core.network;

/**
 * Holds the details for a transfer detail.
 */
public abstract class TransferItem
{
    /**
     * The unique identifier for this transfer item.
     */
    public long id;

    /**
     * The transfer ID which is used to tie one or more transfer items together.
     */
    public long transferId;

    /**
     * The original name of the file.
     * <p>
     * This includes the file format along with the name. For instance, "Rick Ashley - Never Gonna Give You Up.mp3".
     */
    public String name;

    /**
     * Where this file should store in a give save path. This will be 'null' when it should be stored in the root folder
     * (save path).
     */
    public String directory;

    /**
     * The MIME-Type, which tells the actual file type. It could be "video/mp4".
     */
    public String mimeType;

    /**
     * The size of the total bytes this file contains.
     */
    public long size = 0;

    /**
     * The last time that this transfer item was altered.
     */
    public long lastChangeDate;

    /**
     * The type of this transfer item.
     */
    public Type type = Type.INCOMING;

    /**
     * This is used to identify whether a transfer item is incoming or outgoing.
     */
    public enum Type
    {
        /**
         * The item is incoming
         */
        INCOMING,

        /**
         * The item is outgoing.
         */
        OUTGOING
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof TransferItem) {
            TransferItem other = (TransferItem) obj;
            return type != null && type.equals(other.type) && transferId == other.transferId && id == other.id;
        }

        return super.equals(obj);
    }
}
