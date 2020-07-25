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

package org.monora.uprotocol;

import com.genonbeta.CoolSocket.ActiveConnection;
import com.genonbeta.CoolSocket.CoolSocket;
import com.genonbeta.CoolSocket.Response;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;
import org.monora.uprotocol.network.NetworkAdapter;
import org.monora.uprotocol.persistence.NotFoundException;
import org.monora.uprotocol.persistence.PersistenceProvider;
import org.monora.uprotocol.persistence.object.Device;
import org.monora.uprotocol.persistence.object.DeviceConnection;
import org.monora.uprotocol.persistence.object.DeviceInfo;
import org.monora.uprotocol.spec.alpha.Config;
import org.monora.uprotocol.spec.alpha.Keyword;
import org.monora.uprotocol.spec.alpha.Utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeoutException;

/**
 * created by: Veli
 * date: 11.02.2018 15:07
 */

public abstract class CommunicationBridge extends CoolSocket.Client
{
    public static final String TAG = CommunicationBridge.class.getSimpleName();

    private final PersistenceProvider persistenceProvider;
    private final Device thisDevice;
    private Device targetDevice;
    private int pin = -1;

    public CommunicationBridge(PersistenceProvider persistenceProvider, Device thisDevice)
    {
        this.persistenceProvider = persistenceProvider;
        this.thisDevice = thisDevice;
    }

    public CommunicationBridge(PersistenceProvider persistenceProvider, Device thisDevice, int pin)
    {
        this(persistenceProvider, thisDevice);
        setPin(pin);
    }

    public ActiveConnection communicate(Device targetDevice, DeviceConnection targetConnection)
            throws IOException, TimeoutException, CommunicationException, JSONException
    {
        return communicate(targetDevice, targetConnection, false);
    }

    public ActiveConnection communicate(Device targetDevice, DeviceConnection targetConnection, boolean handshakeOnly)
            throws IOException, TimeoutException, CommunicationException, JSONException
    {
        setTargetDevice(targetDevice);
        return communicate(targetConnection.toInetAddress(), handshakeOnly);
    }

    public ActiveConnection communicate(InetAddress address, boolean handshakeOnly) throws IOException,
            TimeoutException, CommunicationException, JSONException
    {
        ActiveConnection activeConnection = connectWithHandshake(address, handshakeOnly);
        communicate(activeConnection, handshakeOnly);
        return activeConnection;
    }

    public void communicate(ActiveConnection activeConnection, boolean handshakeOnly) throws IOException,
            TimeoutException, CommunicationException, JSONException
    {
        boolean keyNotSent = getTargetDevice() == null;
        updateDeviceIfOkay(activeConnection);

        if (!handshakeOnly && keyNotSent) {
            activeConnection.reply(new JSONObject().put(Keyword.DEVICE_INFO_KEY, getTargetDevice().getSecureKey())
                    .toString());
            activeConnection.receive(); // STUB
        }
    }

    public ActiveConnection connect(InetAddress inetAddress) throws IOException
    {
        if (!inetAddress.isReachable(1000))
            throw new IOException("Ping test before connection to the address has failed");

        return openConnection(this, inetAddress);
    }

    public ActiveConnection connect(DeviceConnection connection) throws IOException
    {
        return connect(connection.toInetAddress());
    }

    public ActiveConnection connectWithHandshake(DeviceConnection connection, boolean handshakeOnly)
            throws IOException, TimeoutException, JSONException
    {
        return connectWithHandshake(connection.toInetAddress(), handshakeOnly);
    }

    public ActiveConnection connectWithHandshake(InetAddress inetAddress, boolean handshakeOnly)
            throws IOException, TimeoutException, JSONException
    {
        return handshake(connect(inetAddress), handshakeOnly);
    }

    public PersistenceProvider getPersistenceProvider()
    {
        return persistenceProvider;
    }

    public Device getTargetDevice()
    {
        return targetDevice;
    }

    public ActiveConnection handshake(ActiveConnection activeConnection, boolean handshakeOnly) throws IOException,
            TimeoutException, JSONException
    {
        JSONObject reply = new JSONObject()
                .put(Keyword.HANDSHAKE_REQUIRED, true)
                .put(Keyword.HANDSHAKE_ONLY, handshakeOnly)
                .put(Keyword.DEVICE_INFO_SERIAL, thisDevice.getUid())
                .put(Keyword.DEVICE_PIN, pin);

        Utils.loadFrom(persistenceProvider, getContext(), reply, targetDevice != null ? targetDevice.secureKey : -1);
        activeConnection.reply(reply.toString());

        return activeConnection;
    }

    public Device loadDevice(ActiveConnection activeConnection) throws TimeoutException, IOException,
            CommunicationException
    {
        try {
            Response response = activeConnection.receive();
            JSONObject responseJSON = new JSONObject(response.index);

            return loadFrom(getPersistenceProvider(), responseJSON);
        } catch (JSONException e) {
            throw new CommunicationException("Cannot read the device from JSON");
        }
    }

    public static DeviceInfo loadFrom(PersistenceProvider persistenceProvider, JSONObject object) throws JSONException
    {
        JSONObject deviceInfo = object.getJSONObject(Keyword.DEVICE_INFO);
        JSONObject appInfo = object.getJSONObject(Keyword.APP_INFO);
        String uid = deviceInfo.getString(Keyword.DEVICE_INFO_SERIAL);

        try {
            return persistenceProvider.findDevice(uid).toDeviceInfo();
        } catch (Exception ignored) {
        }

        DeviceInfo resultInfo = new DeviceInfo();
        resultInfo.brand = deviceInfo.getString(Keyword.DEVICE_INFO_BRAND);
        resultInfo.model = deviceInfo.getString(Keyword.DEVICE_INFO_MODEL);
        resultInfo.nickname = deviceInfo.getString(Keyword.DEVICE_INFO_USER);
        resultInfo.secureKey = deviceInfo.has(Keyword.DEVICE_INFO_KEY) ? deviceInfo.getInt(Keyword.DEVICE_INFO_KEY) : -1;
        resultInfo.lastUsageTime = System.currentTimeMillis();
        resultInfo.versionCode = appInfo.getInt(Keyword.APP_INFO_VERSION_CODE);
        resultInfo.versionName = appInfo.getString(Keyword.APP_INFO_VERSION_NAME);
        resultInfo.protocolVersion = appInfo.has(Keyword.APP_INFO_CLIENT_VERSION)
                ? appInfo.getInt(Keyword.APP_INFO_CLIENT_VERSION) : 0;

        if (resultInfo.nickname.length() > Config.NICKNAME_LENGTH_MAX)
            resultInfo.nickname = resultInfo.nickname.substring(0, Config.NICKNAME_LENGTH_MAX - 1);

        Device device = persistenceProvider.getObjectFactory().createDevice();
        device.loadFrom(resultInfo);

        if (deviceInfo.has(Keyword.DEVICE_INFO_PICTURE)) {
            try {
                persistenceProvider.saveDeviceAvatar(device, Base64.decodeBase64(deviceInfo.getString(
                        Keyword.DEVICE_INFO_PICTURE)));
            } catch (IOException e) {
                // TODO: 7/23/20 Log avatar save error. 
            }
        }

        return resultInfo;
    }

    public static ActiveConnection openConnection(CoolSocket.Client client, InetAddress inetAddress)
            throws IOException
    {
        return client.connect(new InetSocketAddress(inetAddress, Config.SERVER_PORT_COMMUNICATION),
                Config.DEFAULT_SOCKET_TIMEOUT);
    }


    public static DeviceConnection processConnection(PersistenceProvider persistenceProvider, Device device,
                                                     String ipAddress)
    {
        DeviceConnection connection = persistenceProvider.getObjectFactory().createDeviceConnection();
        processConnection(persistenceProvider, device, connection);
        return connection;
    }

    public static void processConnection(PersistenceProvider persistenceProvider, Device device,
                                         DeviceConnection connection)
    {
        try {
            persistenceProvider.sync(connection);
        } catch (NotFoundException e) {
            NetworkAdapter.applyAdapterName(connection);
        }

        connection.setLastUsageTime(System.currentTimeMillis());
        connection.setDeviceUid(device.getUid());

        kuick.remove(new SQLQuery.Select(Kuick.TABLE_DEVICECONNECTION)
                .setWhere(Kuick.FIELD_DEVICECONNECTION_DEVICEID + "=? AND "
                                + Kuick.FIELD_DEVICECONNECTION_ADAPTERNAME + " =? AND "
                                + Kuick.FIELD_DEVICECONNECTION_IPADDRESS + " != ?",
                        connection.deviceId, connection.adapterName, connection.ipAddress));

        kuick.publish(connection);
    }

    public void setTargetDevice(Device targetDevice)
    {
        this.targetDevice = targetDevice;
    }

    public void setPin(int pin)
    {
        this.pin = pin;
    }

    protected void updateDeviceIfOkay(ActiveConnection activeConnection) throws IOException,
            TimeoutException, org.monora.uprotocol.CommunicationException
    {
        Device loadedDevice = loadDevice(activeConnection);

        getPersistenceProvider().process(loadedDevice, activeConnection.getClientAddress());

        if (getTargetDevice() != null && !getTargetDevice().getUid().equals(loadedDevice.getUid()))
            throw new DifferentClientException(getTargetDevice(), loadedDevice);

        if (loadedDevice.getProtocolVersion() >= 1) {
            if (getTargetDevice() == null) {
                try {
                    Device existingDevice = new Device(loadedDevice.getUid());

                    getPersistenceProvider().reconstruct(existingDevice);
                    setTargetDevice(existingDevice);
                } catch (ReconstructionFailedException ignored) {
                    loadedDevice.secureKey = AppUtils.generateKey();
                }
            }

            if (getTargetDevice() != null) {
                loadedDevice.applyPreferences(getTargetDevice());

                loadedDevice.secureKey = getTargetDevice().secureKey;
                loadedDevice.isRestricted = false;
            } else
                loadedDevice.isLocal = AppUtils.getDeviceId(getContext()).equals(loadedDevice.id);
        }

        loadedDevice.lastUsageTime = System.currentTimeMillis();

        getPersistenceProvider().saveDevice(loadedDevice);
        getPersistenceProvider().broadcast();
        setTargetDevice(loadedDevice);
    }
}
