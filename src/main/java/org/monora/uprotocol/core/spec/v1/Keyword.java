/*
 * Copyright (C) 2019 Veli Tasalı
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

package org.monora.uprotocol.core.spec.v1;

/**
 * uprotocol keywords as constants.
 */
public class Keyword
{
    public static final String
            CLIENT_MANUFACTURER = "manufacturer",
            CLIENT_NICKNAME = "nickname",
            CLIENT_PICTURE = "picture",
            CLIENT_PICTURE_CHECKSUM = "pictureChecksum",
            CLIENT_PIN = "pin",
            CLIENT_PRODUCT = "product",
            CLIENT_PROTOCOL_VERSION = "protocolVersion",
            CLIENT_PROTOCOL_VERSION_MIN = "minimumProtocolVersion",
            CLIENT_TYPE = "clientType",
            CLIENT_TYPE_ANY = "any",
            CLIENT_TYPE_DESKTOP = "desktop",
            CLIENT_TYPE_IOT = "iot",
            CLIENT_TYPE_PORTABLE = "portable",
            CLIENT_TYPE_WEB = "web",
            CLIENT_UID = "clientUid",
            CLIENT_VERSION_NAME = "versionName",
            CLIENT_VERSION_CODE = "versionCode",
            DIRECTION = "direction",
            DIRECTION_INCOMING = "incoming",
            DIRECTION_OUTGOING = "outgoing",
            ERROR = "error",
            ERROR_NOT_ALLOWED = "notAllowed",
            ERROR_NOT_FOUND = "notFound",
            ERROR_UNKNOWN = "unknown",
            ERROR_NOT_ACCESSIBLE = "notAccessible",
            ERROR_NOT_TRUSTED = "notTrusted",
            ERROR_ALREADY_EXISTS = "alreadyExists",
            INDEX = "index",
            INDEX_FILE_NAME = "name",
            INDEX_FILE_SIZE = "size",
            INDEX_FILE_MIME = "mime",
            INDEX_DIRECTORY = "directory",
            REQUEST = "request",
            REQUEST_ACQUAINTANCE = "acquaintance",
            REQUEST_NOTIFY_TRANSFER_STATE = "notifyTransferState",
            REQUEST_TRANSFER = "transfer",
            REQUEST_TRANSFER_JOB = "transferJob",
            REQUEST_TRANSFER_TEXT = "transferText",
            RESULT = "result",
            TRANSFER_CURRENT_POSITION = "currentPosition",
            TRANSFER_GROUP_ID = "groupId",
            TRANSFER_ID = "id",
            TRANSFER_IS_ACCEPTED = "isAccepted",
            TRANSFER_TEXT = "text";
}