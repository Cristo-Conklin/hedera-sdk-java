package com.hedera.hashgraph.sdk;

import com.hedera.hashgraph.sdk.proto.TransactionBody;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Base class for all transactions that may be built and submitted to Hedera.
 *
 * @param <T> The type of the transaction. Used to enable chaining.
 */
public abstract class TransactionBuilder<RespT, O extends Executable<RespT>, T extends TransactionBuilder<RespT, O, T>>
    extends Executable<RespT> {

    // Default auto renew duration for accounts, contracts, topics, and files (entities)
    static final Duration DEFAULT_AUTO_RENEW_PERIOD = Duration.ofDays(90);

    // Default transaction duration
    private static final Duration DEFAULT_TRANSACTION_VALID_DURATION = Duration.ofSeconds(120);

    protected final TransactionBody.Builder bodyBuilder;

    protected final com.hedera.hashgraph.sdk.proto.Transaction.Builder inner;

    TransactionBuilder() {
        inner = com.hedera.hashgraph.sdk.proto.Transaction.newBuilder();
        bodyBuilder = TransactionBody.newBuilder();

        setTransactionValidDuration(DEFAULT_TRANSACTION_VALID_DURATION);
    }

    /**
     * Set the ID for this transaction.
     * <p>
     * The transaction ID includes the operator's account ( the account paying the transaction
     * fee). If two transactions have the same transaction ID, they won't both have an effect. One
     * will complete normally and the other will fail with a duplicate transaction status.
     * <p>
     * Normally, you should not use this method. Just before a transaction is executed, a
     * transaction ID will be generated from the operator on the client.
     *
     * @return {@code this}
     * @param transactionId The TransactionId to be set
     * @see TransactionId
     */
    public final T setTransactionId(TransactionId transactionId) {
        bodyBuilder.setTransactionID(transactionId.toProtobuf());

        // noinspection unchecked
        return (T) this;
    }

    /**
     * Set the account ID of the node that this transaction will be submitted to.
     * <p>
     * Providing an explicit node account ID interferes with client-side load balancing of the
     * network. By default, the SDK will pre-generate a transaction for 1/3 of the nodes on the
     * network. If a node is down, busy, or otherwise reports a fatal error, the SDK will try again
     * with a different node.
     *
     * @return {@code this}
     * @param nodeAccountId The node AccountId to be set
     */
    public final T setNodeAccountId(AccountId nodeAccountId) {
        bodyBuilder.setNodeAccountID(nodeAccountId.toProtobuf());

        // noinspection unchecked
        return (T) this;
    }

    /**
     * Sets the duration that this transaction is valid for.
     * <p>
     * This is defaulted by the SDK to 120 seconds (or two minutes).
     *
     * @return {@code this}
     * @param validDuration The duration to be set
     */
    public final T setTransactionValidDuration(Duration validDuration) {
        bodyBuilder.setTransactionValidDuration(DurationConverter.toProtobuf(validDuration));

        // noinspection unchecked
        return (T) this;
    }

    /**
     * Set the maximum transaction fee the operator (paying account) is willing to pay.
     *
     * @param maxTransactionFee the maximum transaction fee, in tinybars.
     * @return {@code this}
     */
    public final T setMaxTransactionFee(Hbar maxTransactionFee) {
        bodyBuilder.setTransactionFee(maxTransactionFee.toTinybars());

        // noinspection unchecked
        return (T) this;
    }

    /**
     * Set a note or description that should be recorded in the transaction record (maximum length
     * of 100 characters).
     *
     * @param memo any notes or descriptions for this transaction.
     * @return {@code this}
     */
    public final T setTransactionMemo(String memo) {
        bodyBuilder.setMemo(memo);

        // noinspection unchecked
        return (T) this;
    }

    /**
     * Build this transaction to prepare for for signing or serialization.
     * @param client The Client to build the transaction for
     * @return {@link com.hedera.hashgraph.sdk.Transaction}
     */
    public abstract O build(@Nullable Client client);

    /**
     * Called in {@link #build} just before the transaction body is built. The intent is for the
     * derived class to assign their data variant to the transaction body.
     */
    abstract void onBuild(TransactionBody.Builder bodyBuilder);

    /**
     * Build and execute this transaction.
     * <p>
     * This transaction will be automatically signed by the configured operator, see {@link Client#setOperator}.
     */
    @Override
    public final CompletableFuture<RespT> executeAsync(Client client) {
        return build(client).executeAsync(client);
    }
}
