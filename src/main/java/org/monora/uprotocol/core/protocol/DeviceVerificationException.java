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

package org.monora.uprotocol.core.protocol;

import org.monora.uprotocol.core.network.Device;

/**
 * Thrown when a device becomes due to a change on the security field.
 */
public class DeviceVerificationException extends DeviceInsecureException
{
    public final int receiveKey;

    public DeviceVerificationException(String message, Device device, int receiveKey)
    {
        super(message, device);
        this.receiveKey = receiveKey;
    }
}
