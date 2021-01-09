package org.monora.uprotocol.variant.persistence;

import org.monora.uprotocol.core.network.Client;
import org.monora.uprotocol.core.network.ClientAddress;
import org.monora.uprotocol.core.network.TransferItem;
import org.monora.uprotocol.core.persistence.PersistenceException;
import org.monora.uprotocol.core.persistence.PersistenceProvider;
import org.monora.uprotocol.core.persistence.StreamDescriptor;
import org.monora.uprotocol.variant.DefaultClient;
import org.monora.uprotocol.variant.DefaultClientAddress;
import org.monora.uprotocol.variant.DefaultTransferItem;
import org.monora.uprotocol.variant.holder.Avatar;
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
    private final List<Client> clientList = new ArrayList<>();
    private final List<ClientAddress> clientAddressList = new ArrayList<>();
    private final List<OwnedTransferHolder> transferHolderList = new ArrayList<>();
    private final List<Avatar> avatarList = new ArrayList<>();
    private final List<MemoryStreamDescriptor> streamDescriptorList = new ArrayList<>();
    private final List<String> invalidationRequestList = new ArrayList<>();
    private final BouncyCastleProvider bouncyCastleProvider = new BouncyCastleProvider();
    private final SecureRandom secureRandom = new SecureRandom();
    private final KeyFactory keyFactory;

    private KeyPair keyPair;
    private X509Certificate certificate;

    private KeyPair keyPairBackup;
    private X509Certificate certificateBackup;


    private int networkPin;

    public BasePersistenceProvider()
    {
        regenerateSecrets();

        try {
            keyFactory = KeyFactory.getInstance("RSA");
        } catch (Exception e) {
            throw new RuntimeException("Could not create the key factory instance for RSA encoding.", e);
        }
    }

    public void regenerateSecrets()
    {
        if (keyPairBackup == null)
            keyPairBackup = keyPair;

        if (certificateBackup == null)
            certificateBackup = certificate;

        try {
            keyPair = KeyPairGenerator.getInstance("RSA").genKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not generate PKI key pair.");
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
    }

    public void restoreSecrets()
    {
        if (keyPairBackup != null) {
            keyPair = keyPairBackup;
            keyPairBackup = null;
        }

        if (certificateBackup != null) {
            certificate = certificateBackup;
            certificateBackup = null;
        }
    }

    @Override
    public boolean approveInvalidationOfCredentials(Client client)
    {
        synchronized (invalidationRequestList) {
            if (!invalidationRequestList.remove(client.uid))
                return false;
        }

        client.certificate = null;
        save(client);
        return true;
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
    public ClientAddress createDeviceAddressFor(InetAddress address)
    {
        return new DefaultClientAddress(address);
    }

    @Override
    public DefaultClient createDevice()
    {
        return new DefaultClient();
    }

    @Override
    public DefaultClient createDeviceFor(String uid)
    {
        return new DefaultClient(uid);
    }

    @Override
    public TransferItem createTransferItemFor(long transferId, long id, String name, String mimeType, long size,
                                              String directory, TransferItem.Type type)
    {
        return new DefaultTransferItem(transferId, id, name, mimeType, size, directory, type);
    }

    @Override
    public byte[] getAvatar()
    {
        return new byte[0];
    }

    @Override
    public byte[] getAvatarFor(Client client)
    {
        synchronized (avatarList) {
            for (Avatar avatar : avatarList) {
                if (avatar.deviceUid.equals(client.uid))
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
            networkPin = (int) (Integer.MAX_VALUE * Math.random());

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
    public SSLContext getSSLContextFor(Client client)
    {
        try {
            // Get device private key
            PrivateKey privateKey = getPrivateKey();
            char[] password = new char[0];

            // Setup keystore
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setKeyEntry("key", privateKey, password, new Certificate[]{certificate});

            if (client.certificate != null)
                keyStore.setCertificateEntry(client.uid, client.certificate);

            // Setup key manager factory
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, password);

            TrustManager[] trustManagers;

            if (client.certificate == null) {
                // Set up custom trust manager if we don't have the certificate for the peer.
                X509TrustManager trustManager = new X509TrustManager()
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
                };

                trustManagers = new TrustManager[]{trustManager};
            } else {
                // Set up the default trust manager if we already have the certificate for the peer.
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                        TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(keyStore);

                trustManagers = trustManagerFactory.getTrustManagers();
            }

            // Newer TLS versions are only supported on API 16+
            SSLContext tlsContext = SSLContext.getInstance("TLSv1");
            tlsContext.init(keyManagerFactory.getKeyManagers(), trustManagers, secureRandom);

            return tlsContext;
        } catch (Exception e) {
            // TODO: 1/7/21 Should this throw custom exceptions?
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
    public boolean hasRequestForInvalidationOfCredentials(String deviceUid)
    {
        synchronized (invalidationRequestList) {
            return invalidationRequestList.contains(deviceUid);
        }
    }

    @Override
    public TransferItem loadTransferItem(String deviceUid, long transferId, long id, TransferItem.Type type)
            throws PersistenceException
    {
        synchronized (transferHolderList) {
            for (OwnedTransferHolder holder : transferHolderList) {
                if (holder.item.transferId == transferId && holder.item.id == id && holder.item.type.equals(type)
                        && holder.deviceUid.equals(deviceUid))
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

    public void remove(Client client)
    {
        synchronized (clientList) {
            clientList.remove(client);
        }
    }

    @Override
    public void revokeNetworkPin()
    {
        networkPin = 0;
    }

    @Override
    public void save(Client client)
    {
        synchronized (clientList) {
            clientList.remove(client);
            clientList.add(client);
        }
    }

    @Override
    public void save(ClientAddress clientAddress)
    {
        synchronized (clientAddressList) {
            clientAddressList.remove(clientAddress);
            clientAddressList.add(clientAddress);
        }
    }

    @Override
    public void save(String deviceUid, TransferItem item)
    {
        synchronized (transferHolderList) {
            for (OwnedTransferHolder holder : transferHolderList) {
                if (holder.item.equals(item)) {
                    holder.item = item;
                    holder.deviceUid = deviceUid;
                    return;
                }
            }

            transferHolderList.add(new OwnedTransferHolder(item, deviceUid));
        }
    }

    @Override
    public void save(String deviceUid, List<? extends TransferItem> itemList)
    {
        for (TransferItem item : itemList) {
            save(deviceUid, item);
        }
    }

    @Override
    public void saveAvatar(String deviceUid, byte[] bitmap)
    {
        synchronized (avatarList) {
            avatarList.add(new Avatar(deviceUid, bitmap));
        }
    }

    @Override
    public void saveRequestForInvalidationOfCredentials(String deviceUid)
    {
        if (hasRequestForInvalidationOfCredentials(deviceUid))
            return;

        synchronized (invalidationRequestList) {
            invalidationRequestList.add(deviceUid);
        }
    }

    @Override
    public void setState(String deviceUid, TransferItem item, int state, Exception e)
    {
        synchronized (transferHolderList) {
            for (OwnedTransferHolder holder : transferHolderList) {
                if (deviceUid.equals(holder.deviceUid) && item.equals(holder.item))
                    holder.state = state;
            }
        }
    }

    @Override
    public void sync(Client client) throws PersistenceException
    {
        synchronized (clientList) {
            for (Client persistentClient : clientList) {
                if (client.equals(persistentClient)) {
                    client.from(persistentClient);
                    return;
                }
            }
        }

        throw new PersistenceException("The requested device did not exist and failed to sync.");
    }
}
