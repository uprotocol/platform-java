package org.monora.uprotocol.core.protocol;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.monora.uprotocol.core.spec.v1.Keyword;

/**
 * This enum determines the type of the client.
 */
public enum ClientType
{
    /**
     * The type of the client is generic.
     */
    Any(Keyword.CLIENT_TYPE_ANY),

    /**
     * Runs on a native desktop environment (UWP, GTK, Qt5).
     */
    Desktop(Keyword.CLIENT_TYPE_DESKTOP),

    /**
     * Runs as a background service on an Internet of Things device.
     */
    IoT(Keyword.CLIENT_TYPE_IOT),

    /**
     * Runs on a portable device (e.g., tablets, smartphones).
     */
    Portable(Keyword.CLIENT_TYPE_PORTABLE),

    /**
     * Runs on a web-based environment (e.g., browsers, Electron, PWA).
     */
    Web(Keyword.CLIENT_TYPE_WEB);

    private final @NotNull String protocolValue;

    private @Nullable String protocolValueCustom;

    ClientType(@NotNull String protocolValue)
    {
        this.protocolValue = protocolValue;
    }

    public static @NotNull ClientType from(@NotNull String value)
    {
        for (ClientType type : values()) {
            if (type.protocolValue.equals(value)) {
                return type;
            }
        }

        ClientType type = Any;
        type.protocolValueCustom = value;
        return type;
    }

    /**
     * The value that the protocol specifies which is different from the platform-based enum value.
     *
     * @return The value as defined in the protocol.
     */
    public @NotNull String getProtocolValue()
    {
        return equals(Any) && protocolValueCustom != null ? protocolValueCustom : protocolValue;
    }

    /**
     * The value that built this {@link ClientType}.
     * <p>
     * This will not the protocol value if the requested value was undefined when invoked {@link #from(String)}.
     *
     * @return The original value that build this client type instance.
     */
    public @NotNull String getOriginalValue()
    {
        return protocolValueCustom == null ? name() : protocolValueCustom;
    }
}
