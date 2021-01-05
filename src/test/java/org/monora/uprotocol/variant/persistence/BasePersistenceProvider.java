package org.monora.uprotocol.variant.persistence;

import org.monora.uprotocol.core.network.Device;
import org.monora.uprotocol.core.network.DeviceAddress;
import org.monora.uprotocol.core.network.TransferItem;
import org.monora.uprotocol.core.persistence.PersistenceException;
import org.monora.uprotocol.core.persistence.PersistenceProvider;
import org.monora.uprotocol.core.persistence.StreamDescriptor;
import org.monora.uprotocol.variant.DefaultDevice;
import org.monora.uprotocol.variant.DefaultDeviceAddress;
import org.monora.uprotocol.variant.DefaultTransferItem;
import org.monora.uprotocol.variant.holder.Avatar;
import org.monora.uprotocol.variant.holder.KeyInvalidationRequest;
import org.monora.uprotocol.variant.holder.MemoryStreamDescriptor;
import org.monora.uprotocol.variant.holder.OwnedTransferHolder;
import org.spongycastle.asn1.x500.X500NameBuilder;
import org.spongycastle.asn1.x500.style.BCStyle;
import org.spongycastle.cert.X509v3CertificateBuilder;
import org.spongycastle.cert.jcajce.JcaX509CertificateConverter;
import org.spongycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;

import javax.net.ssl.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * This class provides some level of "persistence" on the level for testing purposes.
 * <p>
 * This will be slow and is not meant to be used in production.
 */
public abstract class BasePersistenceProvider implements PersistenceProvider
{
    private final List<Device> deviceList = new ArrayList<>();
    private final List<DeviceAddress> deviceAddressList = new ArrayList<>();
    private final List<OwnedTransferHolder> transferHolderList = new ArrayList<>();
    private final List<Avatar> avatarList = new ArrayList<>();
    private final List<MemoryStreamDescriptor> streamDescriptorList = new ArrayList<>();
    private final List<KeyInvalidationRequest> invalidationRequestList = new ArrayList<>();
    private final BouncyCastleProvider bouncyCastleProvider = new BouncyCastleProvider();
    private final SecureRandom secureRandom = new SecureRandom();
    private final KeyFactory keyFactory;

    private final KeyPair keyPair;
    private final X509Certificate certificate;

    private int networkPin;

    public BasePersistenceProvider()
    {
        try {
            keyPair = KeyPairGenerator.getInstance("RSA").genKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not generate PKI key.");
        }

        try {
            // don't forget to change the locale to English in production environments when it is set to Persian to fix
            // the issue: https://issuetracker.google.com/issues/37095309
            X500NameBuilder nameBuilder = new X500NameBuilder(BCStyle.INSTANCE);

            nameBuilder.addRDN(BCStyle.CN, getDeviceUid());
            nameBuilder.addRDN(BCStyle.OU, "uprotocol");
            nameBuilder.addRDN(BCStyle.O, "monora");
            final LocalDate localDate = LocalDate.now().minusYears(1);
            final Instant notBefore = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
            final Instant notAfter = localDate.plusYears(10).atStartOfDay(ZoneId.systemDefault()).toInstant();
            X509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(nameBuilder.build(),
                    BigInteger.ONE, Date.from(notBefore), Date.from(notAfter), nameBuilder.build(), keyPair.getPublic());
            ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSAEncryption")
                    .setProvider(bouncyCastleProvider).build(keyPair.getPrivate());
            certificate = new JcaX509CertificateConverter().setProvider(bouncyCastleProvider)
                    .getCertificate(certificateBuilder.build(contentSigner));
        } catch (Exception e) {
            throw new RuntimeException("Could not generate the certificate for this client.", e);
        }

        try {
            keyFactory = KeyFactory.getInstance("RSA");
        } catch (Exception e) {
            throw new RuntimeException("Could not create the key factory instance for RSA encoding.", e);
        }
    }

    @Override
    public boolean approveKeyInvalidationRequest(Device device)
    {
        synchronized (invalidationRequestList) {
            KeyInvalidationRequest matchingRequest = null;

            for (KeyInvalidationRequest request : invalidationRequestList) {
                if (!request.deviceId.equals(device.uid))
                    continue;

                matchingRequest = request;
                break;
            }

            if (matchingRequest != null) {
                device.receiverKey = matchingRequest.receiverKey;
                device.senderKey = matchingRequest.senderKey;
                save(device);
                invalidationRequestList.remove(matchingRequest);
                return true;
            }
        }

        return false;
    }

    @Override
    public void broadcast()
    {

    }

    @Override
    public boolean containsTransfer(long transferId)
    {
        synchronized (transferHolderList) {
            for (OwnedTransferHolder holder : transferHolderList) {
                if (holder.item.transferId == transferId)
                    return true;
            }
        }

        return false;
    }

    @Override
    public DeviceAddress createDeviceAddressFor(InetAddress address)
    {
        return new DefaultDeviceAddress(address);
    }

    @Override
    public DefaultDevice createDevice()
    {
        return new DefaultDevice();
    }

    @Override
    public DefaultDevice createDeviceFor(String uid)
    {
        return new DefaultDevice(uid);
    }

    @Override
    public TransferItem createTransferItemFor(long transferId, long id, String name, String mimeType, long size,
                                              String directory, TransferItem.Type type)
    {
        return new DefaultTransferItem(transferId, id, name, mimeType, size, directory, type);
    }

    @Override
    public int generateKey()
    {
        return (int) (Integer.MAX_VALUE * Math.random());
    }

    @Override
    public byte[] getAvatar()
    {
        return new byte[0];
    }

    @Override
    public byte[] getAvatarFor(Device device)
    {
        synchronized (avatarList) {
            for (Avatar avatar : avatarList) {
                if (avatar.deviceUid.equals(device.uid))
                    return avatar.data;
            }
        }
        return new byte[0];
    }

    @Override
    public X509Certificate getCertificate()
    {
        return certificate;
    }

    @Override
    public StreamDescriptor getDescriptorFor(TransferItem transferItem)
    {
        synchronized (streamDescriptorList) {
            for (MemoryStreamDescriptor streamDescriptor : streamDescriptorList) {
                if (streamDescriptor.transferItem.equals(transferItem))
                    return streamDescriptor;
            }

            MemoryStreamDescriptor descriptor = MemoryStreamDescriptor.newInstance(transferItem);
            streamDescriptorList.add(descriptor);
            return descriptor;
        }
    }

    @Override
    public TransferItem getFirstReceivableItem(long transferId)
    {
        synchronized (transferHolderList) {
            for (OwnedTransferHolder holder : transferHolderList) {
                if (TransferItem.Type.INCOMING.equals(holder.item.type) && holder.item.transferId == transferId
                        && holder.state == STATE_PENDING)
                    return holder.item;
            }
        }
        return null;
    }

    @Override
    public int getNetworkPin()
    {
        if (networkPin == 0)
            networkPin = generateKey();
        return networkPin;
    }

    @Override
    public PrivateKey getPrivateKey()
    {
        try {
            return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyPair.getPrivate().getEncoded()));
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("Could not generate the encoded private key.", e);
        }
    }

    @Override
    public PublicKey getPublicKey()
    {
        try {
            return keyFactory.generatePublic(new X509EncodedKeySpec(keyPair.getPublic().getEncoded()));
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("Could not generate the encoded public key.", e);
        }
    }

    @Override
    public SSLContext getSSLContextFor(Device device)
    {
        try {
            // Get device private key
            PrivateKey privateKey = getPrivateKey();

            char[] password = new char[0];

            // Setup keystore
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setKeyEntry("key", privateKey, password, new Certificate[]{certificate});
            keyStore.setCertificateEntry(device.uid, device.certificate);

            // Setup key manager factory
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, password);

            // Setup default trust manager
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            // Setup custom trust manager if device not trusted
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager()
            {
                public java.security.cert.X509Certificate[] getAcceptedIssuers()
                {
                    return new X509Certificate[0];
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certs, String authType)
                {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] certs, String authType)
                {
                }

            }
            };

            SSLContext tlsContext = SSLContext.getInstance("TLSv1"); //Newer TLS versions are only supported on API 16+
            tlsContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), secureRandom);
            //tlsContext.init(keyManagerFactory.getKeyManagers(), trustAllCerts, secureRandom);
            return tlsContext;
        } catch (Exception e) {
            throw new RuntimeException("Could not create a secure socket context.");
        }
    }

    public List<MemoryStreamDescriptor> getStreamDescriptorList()
    {
        return Collections.unmodifiableList(streamDescriptorList);
    }

    public List<OwnedTransferHolder> getTransferHolderList()
    {
        return Collections.unmodifiableList(transferHolderList);
    }

    @Override
    public boolean hasKeyInvalidationRequest(String deviceId)
    {
        synchronized (invalidationRequestList) {
            for (KeyInvalidationRequest request : invalidationRequestList) {
                if (request.deviceId.equals(deviceId))
                    return true;
            }
        }

        return false;
    }

    @Override
    public TransferItem loadTransferItem(String deviceId, long transferId, long id, TransferItem.Type type)
            throws PersistenceException
    {
        synchronized (transferHolderList) {
            for (OwnedTransferHolder holder : transferHolderList) {
                if (holder.item.transferId == transferId && holder.item.id == id && holder.item.type.equals(type)
                        && holder.deviceId.equals(deviceId))
                    return holder.item;
            }
        }

        throw new PersistenceException("There is no transfer data matching the given parameters.");
    }

    @Override
    public InputStream openInputStream(StreamDescriptor descriptor) throws IOException
    {
        if (descriptor instanceof MemoryStreamDescriptor)
            return new ByteArrayInputStream(((MemoryStreamDescriptor) descriptor).data.toByteArray());

        throw new IOException("Unknown descriptor type");
    }

    @Override
    public OutputStream openOutputStream(StreamDescriptor descriptor) throws IOException
    {
        if (descriptor instanceof MemoryStreamDescriptor)
            return ((MemoryStreamDescriptor) descriptor).data;

        throw new IOException("Unknown descriptor type");
    }

    @Override
    public void revokeNetworkPin()
    {
        networkPin = 0;
    }

    @Override
    public void save(Device device)
    {
        synchronized (deviceList) {
            deviceList.remove(device);
            deviceList.add(device);
        }
    }

    @Override
    public void save(DeviceAddress deviceAddress)
    {
        synchronized (deviceAddressList) {
            deviceAddressList.remove(deviceAddress);
            deviceAddressList.add(deviceAddress);
        }
    }

    @Override
    public void save(String deviceId, TransferItem item)
    {
        synchronized (transferHolderList) {
            for (OwnedTransferHolder holder : transferHolderList) {
                if (holder.item.equals(item)) {
                    holder.item = item;
                    holder.deviceId = deviceId;
                    return;
                }
            }

            transferHolderList.add(new OwnedTransferHolder(item, deviceId));
        }
    }

    @Override
    public void save(String deviceId, List<? extends TransferItem> itemList)
    {
        for (TransferItem item : itemList) {
            save(deviceId, item);
        }
    }

    @Override
    public void saveAvatar(String deviceId, byte[] bitmap)
    {
        synchronized (avatarList) {
            avatarList.add(new Avatar(deviceId, bitmap));
        }
    }

    @Override
    public void saveKeyInvalidationRequest(String deviceId, int receiverKey, int senderKey)
    {
        if (hasKeyInvalidationRequest(deviceId))
            return;

        synchronized (invalidationRequestList) {
            invalidationRequestList.add(new KeyInvalidationRequest(deviceId, receiverKey, senderKey));
        }
    }

    @Override
    public void setState(String deviceId, TransferItem item, int state, Exception e)
    {
        synchronized (transferHolderList) {
            for (OwnedTransferHolder holder : transferHolderList) {
                if (deviceId.equals(holder.deviceId) && item.equals(holder.item))
                    holder.state = state;
            }
        }
    }

    @Override
    public void sync(Device device) throws PersistenceException
    {
        synchronized (deviceList) {
            for (Device persistentDevice : deviceList) {
                if (device.equals(persistentDevice)) {
                    device.from(persistentDevice);
                    return;
                }
            }
        }

        throw new PersistenceException("The requested device did not exist and failed to sync.");
    }
}
