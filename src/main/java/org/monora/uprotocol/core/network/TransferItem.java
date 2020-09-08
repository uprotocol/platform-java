package org.monora.uprotocol.core.network;

public class TransferItem
{
    /**
     * The unique identifier for this transfer item.
     */
    public long id;

    /**
     * The transfer ID.
     */
    public long transferId;

    /**
     * The name of the file. It includes the file format along with the name.
     * <p>
     * For instance, "Rick Astley - Never Gonna Give You Up.mp3".
     * <p>
     * The name here should never be altered. If you need to point to the file name, use {@link #file} field for that.
     */
    public String name;

    /**
     * If this is a {@link Type#INCOMING} transfer item, this will be the relative path to the save path and the
     * {@link #directory}. For instance, let us say that the save path is "/home/pi/" and {@link #directory} is
     * "cakes" and the file name is "birthday_cake_0024.jpg". When all those three are merged, the resulting path
     * will be "/home/pi/cakes/birthday_cake_0024.jpg". In other words, you should only keep "birthday_cake_0024.jpg"
     * in this field. As a good practice, you can keep a temporary name until the file is fully received, i.e.
     * ".4124-4454-4532-6566.tshare" and change it to the {@link #name} when the file is saved.
     * <p>
     * If this is a {@link Type#OUTGOING} transfer item, this will hold the fully resolved path/URI to the file that is
     * sent.
     */
    public String file;

    /**
     * Where this file should store in a give save path. Null when it should be stored in the root folder (save path).
     */
    public String directory;

    /**
     * The MIME-Type which tells the actual file type. It could be "video/mp4".
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
        OUTGOING
    }
}
