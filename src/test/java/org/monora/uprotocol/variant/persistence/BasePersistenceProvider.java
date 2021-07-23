package org.monora.uprotocol.variant.persistence;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.monora.uprotocol.core.io.StreamDescriptor;
import org.monora.uprotocol.core.persistence.PersistenceException;
import org.monora.uprotocol.core.persistence.PersistenceProvider;
import org.monora.uprotocol.core.protocol.Client;
import org.monora.uprotocol.core.protocol.ClientAddress;
import org.monora.uprotocol.core.protocol.ClientType;
import org.monora.uprotocol.core.transfer.TransferItem;
import org.monora.uprotocol.variant.DefaultClient;
import org.monora.uprotocol.variant.DefaultClientAddress;
import org.monora.uprotocol.variant.DefaultTransferItem;
import org.monora.uprotocol.variant.holder.MemoryStreamDescriptor;
import org.monora.uprotocol.variant.holder.TransferHolder;
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
import java.util.*;

/**
 * This class provides some level of "persistence" on the level for testing purposes.
 * <p>
 * This will be slow and is not meant to be used in production.
 */
public abstract class BasePersistenceProvider implements PersistenceProvider
{
    private final Set<Client> clientList = new HashSet<>();
    private final Set<ClientAddress> clientAddressList = new HashSet<>();
    private final List<TransferHolder> transferHolderList = new ArrayList<>();
    private final List<MemoryStreamDescriptor> streamDescriptorList = new ArrayList<>();
    private final List<String> invalidationRequestList = new ArrayList<>();
    private final BouncyCastleProvider bouncyCastleProvider = new BouncyCastleProvider();
    private final @NotNull KeyFactory keyFactory;

    private @NotNull KeyPair keyPair;
    private @NotNull X509Certificate certificate;

    private @Nullable KeyPair keyPairBackup;
    private @Nullable X509Certificate certificateBackup;

    private int networkPin;

    public BasePersistenceProvider()
    {
        keyPair = generateKeyPair();
        certificate = generateCertificate();

        try {
            keyFactory = KeyFactory.getInstance("RSA");
        } catch (Exception e) {
            throw new RuntimeException("Could not create the key factory instance for RSA encoding.", e);
        }
    }

    private @NotNull X509Certificate generateCertificate()
    {
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
            return new JcaX509CertificateConverter().setProvider(bouncyCastleProvider)
                    .getCertificate(certificateBuilder.build(contentSigner));
        } catch (Exception e) {
            throw new RuntimeException("Could not generate the certificate for this client.", e);
        }
    }

    public @NotNull KeyPair generateKeyPair()
    {
        try {
            return KeyPairGenerator.getInstance("RSA").genKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not generate PKI key pair.");
        }
    }

    public void regenerateSecrets()
    {
        if (keyPairBackup == null)
            keyPairBackup = keyPair;

        if (certificateBackup == null)
            certificateBackup = certificate;

        keyPair = generateKeyPair();
        certificate = generateCertificate();
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
    public boolean approveInvalidationOfCredentials(@NotNull Client client)
    {
        synchronized (invalidationRequestList) {
            if (!invalidationRequestList.remove(client.getClientUid()))
                return false;
        }

        client.setClientCertificate(null);
        persist(client, true);
        return true;
    }

    @Override
    public boolean containsTransfer(long groupId)
    {
        synchronized (transferHolderList) {
            for (TransferHolder holder : transferHolderList) {
                if (holder.item.getItemGroupId() == groupId) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public @NotNull ClientAddress createClientAddressFor(@NotNull InetAddress address, @NotNull String clientUid)
    {
        return new DefaultClientAddress(address, clientUid, System.currentTimeMillis());
    }

    @Override
    public @NotNull Client createClientFor(@NotNull String uid, @NotNull String nickname, @NotNull String manufacturer,
                                           @NotNull String product, @NotNull ClientType type,
                                           @NotNull String versionName, int versionCode, int protocolVersion,
                                           int protocolVersionMin)
    {
        return new DefaultClient(uid, nickname, manufacturer, product, type, versionName, versionCode,
                protocolVersion, protocolVersionMin);
    }

    @Override
    public @NotNull TransferItem createTransferItemFor(long groupId, long id, @NotNull String name,
                                                       @NotNull String mimeType, long size, @Nullable String directory,
                                                       @NotNull TransferItem.Type type)
    {
        return new DefaultTransferItem(groupId, id, name, mimeType, size, directory, type);
    }

    @Override
    public @Nullable Client getClientFor(@NotNull String clientUid)
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
    public @NotNull X509Certificate getCertificate()
    {
        return certificate;
    }

    @Override
    public @NotNull StreamDescriptor getDescriptorFor(@NotNull TransferItem transferItem)
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
    public @Nullable TransferItem getFirstReceivableItem(long groupId)
    {
        synchronized (transferHolderList) {
            for (TransferHolder holder : transferHolderList) {
                if (TransferItem.Type.Incoming.equals(holder.item.getItemType())
                        && holder.item.getItemGroupId() == groupId
                        && TransferItem.State.Pending.equals(holder.state)) {
                    return holder.item;
                }
            }
        }
        return null;
    }

    @Override
    public int getNetworkPin()
    {
        if (networkPin == 0) {
            networkPin = (int) (Integer.MAX_VALUE * Math.random());
        }

        return networkPin;
    }

    @Override
    public @NotNull PrivateKey getPrivateKey()
    {
        try {
            return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyPair.getPrivate().getEncoded()));
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("Could not generate the encoded private key.", e);
        }
    }

    @Override
    public @NotNull PublicKey getPublicKey()
    {
        try {
            return keyFactory.generatePublic(new X509EncodedKeySpec(keyPair.getPublic().getEncoded()));
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("Could not generate the encoded public key.", e);
        }
    }

    public @NotNull List<MemoryStreamDescriptor> getStreamDescriptorList()
    {
        return Collections.unmodifiableList(streamDescriptorList);
    }

    public @NotNull List<TransferHolder> getTransferHolderList()
    {
        return Collections.unmodifiableList(transferHolderList);
    }

    @Override
    public boolean hasRequestForInvalidationOfCredentials(@NotNull String clientUid)
    {
        synchronized (invalidationRequestList) {
            return invalidationRequestList.contains(clientUid);
        }
    }

    @Override
    public @NotNull TransferItem loadTransferItem(@NotNull String clientUid, long groupId, long id,
                                                  @NotNull TransferItem.Type type) throws PersistenceException
    {
        synchronized (transferHolderList) {
            for (TransferHolder holder : transferHolderList) {
                if (holder.item.getItemGroupId() == groupId && holder.item.getItemId() == id
                        && holder.item.getItemType().equals(type) && holder.clientUid.equals(clientUid))
                    return holder.item;
            }
        }

        throw new PersistenceException("There is no transfer data matching the given parameters.");
    }

    @Override
    public @NotNull InputStream openInputStream(@NotNull StreamDescriptor descriptor) throws IOException
    {
        if (descriptor instanceof MemoryStreamDescriptor) {
            return new ByteArrayInputStream(((MemoryStreamDescriptor) descriptor).data.toByteArray());
        }

        throw new IOException("Unknown descriptor type");
    }

    @Override
    public @NotNull OutputStream openOutputStream(@NotNull StreamDescriptor descriptor) throws IOException
    {
        if (descriptor instanceof MemoryStreamDescriptor) {
            return ((MemoryStreamDescriptor) descriptor).data;
        }

        throw new IOException("Unknown descriptor type");
    }

    @Override
    public void persist(@NotNull Client client, boolean updating)
    {
        synchronized (clientList) {
            clientList.add(client);
        }
    }

    @Override
    public void persist(@NotNull ClientAddress clientAddress)
    {
        synchronized (clientAddressList) {
            clientAddressList.add(clientAddress);
        }
    }

    @Override
    public void persist(@NotNull String clientUid, @NotNull TransferItem item)
    {
        synchronized (transferHolderList) {
            for (TransferHolder holder : transferHolderList) {
                if (holder.item.equals(item)) {
                    holder.item = item;
                    holder.clientUid = clientUid;
                    return;
                }
            }

            transferHolderList.add(new TransferHolder(item, clientUid));
        }
    }

    @Override
    public void persist(@NotNull String clientUid, @NotNull List<? extends @NotNull TransferItem> itemList)
    {
        for (TransferItem item : itemList) {
            // This doesn't reflect the correct use case. In production, this should only insert
            // while single item counterpart above is updating.
            persist(clientUid, item);
        }
    }

    @Override
    public void persistClientPicture(@NotNull Client client, byte @Nullable [] data, int checksum)
    {
        if (client instanceof DefaultClient) {
            DefaultClient defaultClient = (DefaultClient) client;
            defaultClient.pictureData = data == null ? new byte[0] : data;
            defaultClient.pictureChecksum = checksum;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public boolean removeTransfer(@NotNull Client client, long groupId)
    {
        synchronized (transferHolderList) {
            final List<TransferHolder> copyItems = new ArrayList<>(transferHolderList);
            boolean removedAny = false;

            for (TransferHolder transferHolder : copyItems) {
                if (transferHolder.clientUid.equals(client.getClientUid())
                        && transferHolder.item.getItemGroupId() == groupId
                        && transferHolderList.remove(transferHolder)) {
                    removedAny = true;
                }
            }

            return removedAny;
        }
    }

    @Override
    public void revokeNetworkPin()
    {
        networkPin = 0;
    }

    @Override
    public void saveRequestForInvalidationOfCredentials(@NotNull String clientUid)
    {
        if (hasRequestForInvalidationOfCredentials(clientUid)) {
            return;
        }

        synchronized (invalidationRequestList) {
            invalidationRequestList.add(clientUid);
        }
    }

    @Override
    public void setState(@NotNull String clientUid, @NotNull TransferItem item, @NotNull TransferItem.State state,
                         @Nullable Exception e)
    {
        synchronized (transferHolderList) {
            for (TransferHolder holder : transferHolderList) {
                if (clientUid.equals(holder.clientUid) && item.equals(holder.item)) {
                    holder.state = state;
                }
            }
        }
    }
}
