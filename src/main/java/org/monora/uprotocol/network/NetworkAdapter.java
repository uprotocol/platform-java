package org.monora.uprotocol.network;

import org.monora.uprotocol.persistence.object.DeviceConnection;
import org.monora.uprotocol.spec.alpha.Config;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class NetworkAdapter
{
    public static void applyAdapterName(DeviceConnection connection)
    {
        if (connection.getIpAddress() == 0)
            // TODO: 7/24/20 log: "Connection should be provided with IP address"
            return;

        try {
            NetworkInterface networkInterface = findNetworkInterface(connection.toInetAddress());

            if (networkInterface != null)
                connection.setAdapterName(networkInterface.getDisplayName());
            // TODO: 7/24/20 log: "applyAdapterName(): No network interface found"
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        if (connection.getAdapterName() == null)
            connection.setAdapterName(Config.NETWORK_INTERFACE_UNKNOWN);
    }

    public static boolean compareAddressRanges(NetworkInterface networkInterface, Inet4Address b)
    {
        Enumeration<InetAddress> addressList = networkInterface.getInetAddresses();

        while (addressList.hasMoreElements()) {
            InetAddress address = addressList.nextElement();
            if (!address.isLoopbackAddress() && (address instanceof Inet4Address)
                    && compareAddressRanges((Inet4Address) address, b))
                return true;
        }

        return false;
    }

    public static boolean compareAddressRanges(Inet4Address a, Inet4Address b)
    {
        byte[] ba = a.getAddress();
        byte[] bb = b.getAddress();

        for (int i = 0; i < 2; i++)
            if (ba[i] != bb[i])
                return false;

        return true;
    }

    public static NetworkInterface findNetworkInterface(Inet4Address address)
    {
        List<NetworkInterface> interfaceList = getInterfaces(true, Config.DEFAULT_DISABLED_INTERFACES);

        for (NetworkInterface networkInterface : interfaceList) {
            if (compareAddressRanges(networkInterface, address))
                return networkInterface;
        }

        return null;
    }

    public static List<NetworkInterface> getInterfaces(boolean ipV4only, String[] avoidedInterfaces)
    {
        List<NetworkInterface> filteredInterfaceList = new ArrayList<>();

        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                boolean avoidedInterface = false;

                if (avoidedInterfaces != null && avoidedInterfaces.length > 0)
                    for (String match : avoidedInterfaces)
                        if (networkInterface.getDisplayName().startsWith(match))
                            avoidedInterface = true;

                if (avoidedInterface)
                    continue;

                Enumeration<InetAddress> addressList = networkInterface.getInetAddresses();

                while (addressList.hasMoreElements()) {
                    InetAddress address = addressList.nextElement();
                    if (!address.isLoopbackAddress() && (address instanceof Inet4Address || !ipV4only)) {
                        filteredInterfaceList.add(networkInterface);
                        break;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return filteredInterfaceList;
    }
}
