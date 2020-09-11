package org.monora.uprotocol.core.protocol;

// TODO: 9/11/20 Link the specs.

/**
 * This enum determines the type of the client.
 */
public enum ClientType
{
    /**
     * The type of the client is generic..
     */
    Any,

    /**
     * Runs on a mobile device (e.g., tablets, smartphones).
     */
    Mobile,

    /**
     * Runs on a web-based environment (e.g., Electron, PWA).
     */
    Web,

    /**
     * Runs on a native desktop environment (UWP, GTK, Qt5).
     */
    Desktop,

    /**
     * Runs as a background service on an Internet of Things device.
     */
    IoT
}
