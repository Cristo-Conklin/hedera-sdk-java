package com.hedera.hashgraph.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.TransactionID;
import java8.util.concurrent.CompletableFuture;
import org.threeten.bp.Clock;
import org.threeten.bp.Instant;

import javax.annotation.Nullable;

import static java8.util.concurrent.CompletableFuture.completedFuture;
import static java8.util.concurrent.CompletableFuture.failedFuture;

/**
 * The client-generated ID for a transaction.
 *
 * <p>This is used for retrieving receipts and records for a transaction, for appending to a file
 * right after creating it, for instantiating a smart contract with bytecode in a file just created,
 * and internally by the network for detecting when duplicate transactions are submitted.
 */
public final class TransactionId implements WithGetReceipt, WithGetRecord {
    /**
     * The Account ID that paid for this transaction.
     */
    public final AccountId accountId;

    /**
     * The time from when this transaction is valid.
     *
     * <p>When a transaction is submitted there is additionally a validDuration (defaults to 120s)
     * and together they define a time window that a transaction may be processed in.
     */
    public final Instant validStart;

    public TransactionId(AccountId accountId, Instant validStart) {
        this.accountId = accountId;
        this.validStart = validStart;
    }

    /**
     * Generates a new transaction ID for the given account ID.
     *
     * <p>Note that transaction IDs are made of the valid start of the transaction and the account
     * that will be charged the transaction fees for the transaction.
     *
     * @param accountId the ID of the Hedera account that will be charge the transaction fees.
     * @return {@link com.hedera.hashgraph.sdk.TransactionId}
     */
    public static TransactionId generate(AccountId accountId) {
        Instant instant = Clock.systemUTC().instant().minusNanos((long) (Math.random() * 5000000000L + 8000000000L));
        return new TransactionId(accountId, instant);
    }

    static TransactionId fromProtobuf(TransactionID transactionID) {
        return new TransactionId(
            AccountId.fromProtobuf(transactionID.getAccountID()),
            InstantConverter.fromProtobuf(transactionID.getTransactionValidStart()));
    }

    public static TransactionId fromString(String s) {
        var parts = s.split("@", 2);

        if (parts.length != 2) {
            throw new IllegalArgumentException("expecting {account}@{seconds}.{nanos}");
        }

        var accountId = AccountId.fromString(parts[0]);

        var validStartParts = parts[1].split("\\.", 2);

        if (validStartParts.length != 2) {
            throw new IllegalArgumentException("expecting {account}@{seconds}.{nanos}");
        }

        var validStart = Instant.ofEpochSecond(
            Long.parseLong(validStartParts[0]),
            Long.parseLong(validStartParts[1]));

        return new TransactionId(accountId, validStart);
    }

    public static TransactionId fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        return fromProtobuf(TransactionID.parseFrom(bytes).toBuilder().build());
    }

    @Override
    @FunctionalExecutable(type = "TransactionReceipt", exceptionTypes = {"ReceiptStatusException"})
    public CompletableFuture<TransactionReceipt> getReceiptAsync(Client client) {
        return new TransactionReceiptQuery()
            .setTransactionId(this)
            .executeAsync(client)
            .thenCompose(receipt -> {
                if (receipt.status != Status.SUCCESS) {
                    return failedFuture(new ReceiptStatusException(this, receipt));
                }

                return completedFuture(receipt);
            });
    }

    @Override
    @FunctionalExecutable(type = "TransactionRecord", exceptionTypes = {"ReceiptStatusException"})
    public CompletableFuture<TransactionRecord> getRecordAsync(Client client) {
        // note: we get the receipt first to ensure consensus has been reached
        return getReceiptAsync(client).thenCompose(receipt -> new TransactionRecordQuery()
            .setTransactionId(this)
            .executeAsync(client));
    }

    TransactionID toProtobuf() {
        return TransactionID.newBuilder()
            .setAccountID(accountId.toProtobuf())
            .setTransactionValidStart(InstantConverter.toProtobuf(validStart))
            .build();
    }

    @Override
    public String toString() {
        return "" + accountId + "@" + validStart.getEpochSecond() + "." + validStart.getNano();
    }

    public byte[] toBytes() {
        return toProtobuf().toByteArray();
    }


    @Override
    public boolean equals(@Nullable Object object) {
        if (!(object instanceof TransactionId)) {
            return false;
        }

        return ((TransactionId) object).accountId.equals(accountId) &&
            ((TransactionId) object).validStart.equals(validStart);
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
