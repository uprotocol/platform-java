package org.monora.uprotocol.core.io;

import org.jetbrains.annotations.NotNull;
import org.monora.uprotocol.core.protocol.Client;

public interface ClientPicture
{
    /**
     * The client that owns this picture.
     *
     * @return The client uid.
     */
    @NotNull String getClientUid();

    /**
     * Loads and returns the non-null byte array that contains the picture bitmap data.
     * <p>
     * The idea is to spare the loading part to the invocation of this method.
     * <p>
     * The invocation of this method will take place after {@link #hasPicture()} returns positive.
     *
     * @return The non-zero length byte array that contains picture bitmap data.
     */
    byte @NotNull [] getPictureData();

    /**
     * Calculated hash of the picture data.
     * <p>
     * This should be pre-calculated for performance benefits.
     * <p>
     * The invocation of this method will take place after {@link #hasPicture()} returns positive.
     *
     * @return The hash code of the picture bitmap data.
     */
    int getPictureChecksum();

    /**
     * Invoked when the picture is about to be sent to a remote client to check whether the client has a
     * picture.
     *
     * @return True if the client has a picture.
     */
    boolean hasPicture();

    /**
     * Creates a new instance where the provided data is static and unchanging.
     *
     * @param clientUid That owns the picture.
     * @param data      The non-null (zero-length if null) bitmap data.
     * @param checksum  The precalculated has of the data.
     * @return The new client picture instance.
     */
    static @NotNull ClientPicture newInstance(@NotNull String clientUid, byte @NotNull [] data, int checksum)
    {
        return new ClientPictureImpl(clientUid, data, checksum);
    }

    /**
     * Creates an empty client picture that belongs to a certain {@link Client}.
     *
     * @param clientUid That owns the picture.
     * @return An empty picture
     */
    static @NotNull ClientPicture newEmptyInstance(@NotNull String clientUid) {
        return newInstance(clientUid, new byte[0], 0);
    }
}