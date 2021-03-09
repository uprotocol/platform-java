/*
 * Copyright (C) 2020 Veli Tasalı
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.monora.uprotocol.core.protocol.communication.client;

import org.jetbrains.annotations.NotNull;
import org.monora.uprotocol.core.CommunicationBridge;
import org.monora.uprotocol.core.protocol.Client;
import org.monora.uprotocol.core.protocol.communication.ProtocolException;

import java.net.InetAddress;

/**
 * This error concerns a remote client and is thrown when {@link CommunicationBridge} communicates with a peer that has
 * a different {@link Client#getClientUid()} mismatching with the one that it intends to connect to.
 */
public class DifferentRemoteClientException extends ProtocolException
{
    public final @NotNull String expectedUid;
    public final @NotNull String gotUid;
    public final @NotNull InetAddress errorCausingAddress;

    public DifferentRemoteClientException(@NotNull String expectedUid, @NotNull String gotUid,
                                          @NotNull InetAddress errorCausingAddress)
    {
        super();
        this.expectedUid = expectedUid;
        this.gotUid = gotUid;
        this.errorCausingAddress = errorCausingAddress;
    }
}
