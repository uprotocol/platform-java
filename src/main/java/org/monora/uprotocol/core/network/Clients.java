package org.monora.uprotocol.core.network;

import org.monora.uprotocol.core.protocol.ClientType;

import java.security.cert.X509Certificate;

/**
 * This class helps move and copy an {@link Client} object.
 */
public class Clients
{
    public static void copy(Client from, Client to)
    {
        fill(to, from.getClientUid(), from.getClientCertificate(), from.getClientNickname(), from.getClientManufacturer(),
                from.getClientProduct(), from.getClientType(), from.getClientVersionName(), from.getClientVersionCode(),
                from.getClientProtocolVersion(), from.getClientProtocolVersionMin(), from.isClientTrusted(),
                from.isClientBlocked());
    }

    public static void fill(Client client, String uid, X509Certificate certificate, String nickname, String manufacturer,
                            String product, ClientType clientType, String versionName,
                            int versionCode, int protocolVersion, int protocolVersionMin, boolean isTrusted,
                            boolean isBlocked)
    {
        client.setClientUid(uid);
        client.setClientCertificate(certificate);
        client.setClientNickname(nickname);
        client.setClientManufacturer(manufacturer);
        client.setClientProduct(product);
        client.setClientType(clientType);
        client.setClientVersionName(versionName);
        client.setClientVersionCode(versionCode);
        client.setClientProtocolVersion(protocolVersion);
        client.setClientProtocolVersionMin(protocolVersionMin);
        client.setClientTrusted(isTrusted);
        client.setClientBlocked(isBlocked);
    }
}
