package org.monora.uprotocol.variant.persistence;

import org.monora.uprotocol.core.io.StreamDescriptor;
import org.monora.uprotocol.core.persistence.PersistenceException;
import org.monora.uprotocol.core.persistence.PersistenceProvider;
import org.monora.uprotocol.core.protocol.Client;
import org.monora.uprotocol.core.protocol.ClientAddress;
import org.monora.uprotocol.core.protocol.ClientType;
import org.monora.uprotocol.core.protocol.Clients;
import org.monora.uprotocol.core.transfer.TransferItem;
import org.monora.uprotocol.variant.DefaultClient;
import org.monora.uprotocol.variant.DefaultClientAddress;
import org.monora.uprotocol.variant.DefaultTransferItem;
import org.monora.uprotocol.variant.holder.ClientPicture;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.*;
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
    private final List<ClientPicture> clientPictureList = new ArrayList<>();
    private final List<MemoryStreamDescriptor> streamDescriptorList = new ArrayList<>();
    private final List<String> invalidationRequestList = new ArrayList<>();
    private final BouncyCastleProvider bouncyCastleProvider = new BouncyCastleProvider();
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

            nameBuilder.addRDN(BCStyle.CN, getClientUid());
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
            if (!invalidationRequestList.remove(client.getClientUid()))
                return false;
        }

        client.setClientCertificate(null);
        save(client);
        return true;
    }

    @Override
    public void broadcast()
    {

    }

    @Override
    public boolean containsTransfer(long groupId)
    {
        synchronized (transferHolderList) {
            for (OwnedTransferHolder holder : transferHolderList) {
                if (holder.item.getItemGroupId() == groupId)
                    return true;
            }
        }

        return false;
    }

    @Override
    public ClientAddress createClientAddressFor(InetAddress address, String clientUid)
    {
        return new DefaultClientAddress(address, clientUid, System.currentTimeMillis());
    }
    
    @Override
    public Client createClientFor(String uid, String nickname, String manufacturer,
                                  String product, ClientType type, String versionName, int versionCode,
                                  int protocolVersion, int protocolVersionMin)
    {
        return new DefaultClient(uid, nickname, manufacturer, product, type, versionName, versionCode,
                protocolVersion, protocolVersionMin);
    }

    @Override
    public TransferItem createTransferItemFor(long groupId, long id, String name, String mimeType, long size,
                                              String directory, TransferItem.Type type)
    {
        return new DefaultTransferItem(groupId, id, name, mimeType, size, directory, type);
    }

    @Override
    public Client getClientFor(String clientUid)
    {
        synchronized (clientList) {
            for (Client persistentClient : clientList) {
                if (clientUid.equals(persistentClient.getClientUid())) {
                    return persistentClient;
                }
            }
        }

        return null;
    }

    @Override
    public byte[] getClientPicture()
    {
        return new byte[0];
    }

    @Override
    public byte[] getClientPictureFor(Client client)
    {
        synchronized (clientPictureList) {
            for (ClientPicture clientPicture : clientPictureList) {
                if (clientPicture.clientUid.equals(client.getClientUid()))
                    return clientPicture.data;
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
    public TransferItem getFirstReceivableItem(long groupId)
    {
        synchronized (transferHolderList) {
            for (OwnedTransferHolder holder : transferHolderList) {
                if (TransferItem.Type.Incoming.equals(holder.item.getItemType())
                        && holder.item.getItemGroupId() == groupId && holder.state == STATE_PENDING)
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

    public List<MemoryStreamDescriptor> getStreamDescriptorList()
    {
        return Collections.unmodifiableList(streamDescriptorList);
    }

    public List<OwnedTransferHolder> getTransferHolderList()
    {
        return Collections.unmodifiableList(transferHolderList);
    }

    @Override
    public boolean hasRequestForInvalidationOfCredentials(String clientUid)
    {
        synchronized (invalidationRequestList) {
            return invalidationRequestList.contains(clientUid);
        }
    }

    @Override
    public TransferItem loadTransferItem(String clientUid, long groupId, long id, TransferItem.Type type)
            throws PersistenceException
    {
        synchronized (transferHolderList) {
            for (OwnedTransferHolder holder : transferHolderList) {
                if (holder.item.getItemGroupId() == groupId && holder.item.getItemId() == id
                        && holder.item.getItemType().equals(type) && holder.clientUid.equals(clientUid))
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
    public void save(String clientUid, TransferItem item)
    {
        synchronized (transferHolderList) {
            for (OwnedTransferHolder holder : transferHolderList) {
                if (holder.item.equals(item)) {
                    holder.item = item;
                    holder.clientUid = clientUid;
                    return;
                }
            }

            transferHolderList.add(new OwnedTransferHolder(item, clientUid));
        }
    }

    @Override
    public void save(String clientUid, List<? extends TransferItem> itemList)
    {
        for (TransferItem item : itemList) {
            save(clientUid, item);
        }
    }

    @Override
    public void saveClientPicture(String clientUid, byte[] bitmap)
    {
        synchronized (clientPictureList) {
            clientPictureList.add(new ClientPicture(clientUid, bitmap));
        }
    }

    @Override
    public void saveRequestForInvalidationOfCredentials(String clientUid)
    {
        if (hasRequestForInvalidationOfCredentials(clientUid))
            return;

        synchronized (invalidationRequestList) {
            invalidationRequestList.add(clientUid);
        }
    }

    @Override
    public void setState(String clientUid, TransferItem item, int state, Exception e)
    {
        synchronized (transferHolderList) {
            for (OwnedTransferHolder holder : transferHolderList) {
                if (clientUid.equals(holder.clientUid) && item.equals(holder.item))
                    holder.state = state;
            }
        }
    }
}
