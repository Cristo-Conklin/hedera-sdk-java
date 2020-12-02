package com.hedera.hashgraph.sdk;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hedera.hashgraph.sdk.proto.SignatureMap;
import com.hedera.hashgraph.sdk.proto.SignedTransaction;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionList;
import java8.util.concurrent.CompletableFuture;
import java8.util.function.Function;
import org.bouncycastle.crypto.digests.SHA384Digest;
import org.threeten.bp.Duration;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Base class for all transactions that may be built and submitted to Hedera.
 *
 * @param <T> The type of the transaction. Used to enable chaining.
 */
public abstract class Transaction<T extends Transaction<T>>
    extends Executable<com.hedera.hashgraph.sdk.proto.Transaction, com.hedera.hashgraph.sdk.proto.TransactionResponse, TransactionResponse> {

    // Default auto renew duration for accounts, contracts, topics, and files (entities)
    static final Duration DEFAULT_AUTO_RENEW_PERIOD = Duration.ofDays(90);

    // Default transaction duration
    private static final Duration DEFAULT_TRANSACTION_VALID_DURATION = Duration.ofSeconds(120);

    protected TransactionBody.Builder bodyBuilder;

    // A SDK [Transaction] is composed of multiple, raw protobuf transactions. These should be
    // functionally identical, with the exception of pointing to different nodes. When retrying a
    // transaction after a network error or retry-able status response, we try a
    // different transaction and thus a different node.
    protected List<com.hedera.hashgraph.sdk.proto.Transaction> transactions = Collections.emptyList();
    protected List<com.hedera.hashgraph.sdk.proto.SignedTransaction.Builder> signedTransactions = Collections.emptyList();
    protected List<SignatureMap.Builder> signatures = Collections.emptyList();
    protected List<AccountId> nodeIds = Collections.emptyList();

    // The index of the _next_ transaction to be built and executed.
    // Each time `buildNext` is invoked, this should be incremented by 1 and wrapped around with the
    // size of the transaction array.
    int nextTransactionIndex = 0;

    Transaction() {
        bodyBuilder = TransactionBody.newBuilder();

        // Cannot call `Transaction#setTranscationValidDuration()` because it calls `isFrozen()` and
        // causes a `NullPointerException` in `TopicMessageSubmitTransaction#isFrozen()`. I assume the private
        // fields are not being set before the `super()` call which is why that is happening.
        bodyBuilder.setTransactionValidDuration(DurationConverter.toProtobuf(DEFAULT_TRANSACTION_VALID_DURATION));
    }

    Transaction(HashMap<AccountId, com.hedera.hashgraph.sdk.proto.Transaction> txs) throws InvalidProtocolBufferException {
        this.nodeIds = new ArrayList<>(txs.keySet());
        this.signatures = new ArrayList<>(this.nodeIds.size());
        this.transactions = new ArrayList<>(this.nodeIds.size());
        this.signedTransactions = new ArrayList<>(this.nodeIds.size());

        for (var value : txs.values()) {
            var transaction = SignedTransaction.parseFrom(value.getSignedTransactionBytes());
            this.signatures.add(transaction.getSigMap().toBuilder());
            this.signedTransactions.add(transaction.toBuilder());
            this.transactions.add(value);
        }

        try {
            bodyBuilder = TransactionBody.parseFrom(this.signedTransactions.get(0).getBodyBytes()).toBuilder();
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalArgumentException(e);
        }
    }

    Transaction(TransactionBody.Builder bodyBuilder) {
        this.bodyBuilder = bodyBuilder;
    }

    public static Transaction<?> fromBytes(byte[] bytes) throws InvalidProtocolBufferException {
        var txs = new HashMap<TransactionId, HashMap<AccountId, com.hedera.hashgraph.sdk.proto.Transaction>>();
        TransactionBody.DataCase dataCase = TransactionBody.DataCase.DATA_NOT_SET;

        var list = TransactionList.parseFrom(bytes);

        for (var transaction : list.getTransactionListList()) {
            var signedTransaction = SignedTransaction.parseFrom(transaction.getSignedTransactionBytes());
            var txBody = TransactionBody.parseFrom(signedTransaction.getBodyBytes());

            if (dataCase.getNumber() == TransactionBody.DataCase.DATA_NOT_SET.getNumber()) {
                dataCase = txBody.getDataCase();
            }

            var account = AccountId.fromProtobuf(txBody.getNodeAccountID());
            var transactionId = TransactionId.fromProtobuf(txBody.getTransactionID());

            txs.computeIfAbsent(transactionId, k -> new HashMap<>()).put(account, transaction);
        }

        Transaction<?> instance;

        switch (dataCase) {
            case CONTRACTCALL:
                instance = new ContractExecuteTransaction(txs);
                break;

            case CONTRACTCREATEINSTANCE:
                instance = new ContractCreateTransaction(txs);
                break;

            case CONTRACTUPDATEINSTANCE:
                instance = new ContractUpdateTransaction(txs);
                break;

            case CONTRACTDELETEINSTANCE:
                instance = new ContractDeleteTransaction(txs);
                break;

            case CRYPTOADDLIVEHASH:
                instance = new LiveHashAddTransaction(txs);
                break;

            case CRYPTOCREATEACCOUNT:
                instance = new AccountCreateTransaction(txs);
                break;

            case CRYPTODELETE:
                instance = new AccountDeleteTransaction(txs);
                break;

            case CRYPTODELETELIVEHASH:
                instance = new LiveHashDeleteTransaction(txs);
                break;

            case CRYPTOTRANSFER:
                instance = new TransferTransaction(txs);
                break;

            case CRYPTOUPDATEACCOUNT:
                instance = new AccountUpdateTransaction(txs);
                break;

            case FILEAPPEND:
                instance = new FileAppendTransaction(txs);
                break;

            case FILECREATE:
                instance = new FileCreateTransaction(txs);
                break;

            case FILEDELETE:
                instance = new FileDeleteTransaction(txs);
                break;

            case FILEUPDATE:
                instance = new FileUpdateTransaction(txs);
                break;

            case SYSTEMDELETE:
                instance = new SystemDeleteTransaction(txs);
                break;

            case SYSTEMUNDELETE:
                instance = new SystemUndeleteTransaction(txs);
                break;

            case FREEZE:
                instance = new FreezeTransaction(txs);
                break;

            case CONSENSUSCREATETOPIC:
                instance = new TopicCreateTransaction(txs);
                break;

            case CONSENSUSUPDATETOPIC:
                instance = new TopicUpdateTransaction(txs);
                break;

            case CONSENSUSDELETETOPIC:
                instance = new TopicDeleteTransaction(txs);
                break;

            case CONSENSUSSUBMITMESSAGE:
                // a chunked transaction does not need the same handling
                return new TopicMessageSubmitTransaction(txs);

            default:
                throw new IllegalArgumentException("parsed transaction body has no data");
        }

        return instance;
    }

    private static byte[] hash(byte[] bytes) {
        var digest = new SHA384Digest();
        var hash = new byte[digest.getDigestSize()];

        digest.update(bytes, 0, bytes.length);
        digest.doFinal(hash, 0);

        return hash;
    }

    @Nullable
    public final AccountId getNodeAccountId() {
        if (!nodeIds.isEmpty()) {
            return nodeIds.get(nextTransactionIndex);
        }

        if (bodyBuilder.hasNodeAccountID()) {
            return AccountId.fromProtobuf(bodyBuilder.getNodeAccountID());
        }

        return null;
    }

    @Nullable
    public final List<AccountId> getNodeAccountIds() {
        if (!nodeIds.isEmpty()) {
            return nodeIds;
        }

        return null;
    }

    /**
     * Set the account IDs of the nodes that this transaction will be submitted to.
     * <p>
     * Providing an explicit node account ID interferes with client-side load balancing of the
     * network. By default, the SDK will pre-generate a transaction for 1/3 of the nodes on the
     * network. If a node is down, busy, or otherwise reports a fatal error, the SDK will try again
     * with a different node.
     *
     * @param nodeAccountIds The list of node AccountIds to be set
     * @return {@code this}
     */
    public final T setNodeAccountIds(List<AccountId> nodeAccountIds) {
        requireNotFrozen();
        nodeIds = nodeAccountIds;
        bodyBuilder.setNodeAccountID(nodeIds.get(nextTransactionIndex).toProtobuf());

        // noinspection unchecked
        return (T) this;
    }

    @Nullable
    public final Duration getTransactionValidDuration() {
        return bodyBuilder.hasTransactionValidDuration() ? DurationConverter.fromProtobuf(bodyBuilder.getTransactionValidDuration()) : null;
    }

    /**
     * Sets the duration that this transaction is valid for.
     * <p>
     * This is defaulted by the SDK to 120 seconds (or two minutes).
     *
     * @param validDuration The duration to be set
     * @return {@code this}
     */
    public final T setTransactionValidDuration(Duration validDuration) {
        requireNotFrozen();
        bodyBuilder.setTransactionValidDuration(DurationConverter.toProtobuf(validDuration));

        // noinspection unchecked
        return (T) this;
    }

    @Nullable
    public final Hbar getMaxTransactionFee() {
        var transactionFee = bodyBuilder.getTransactionFee();

        if (transactionFee == 0) {
            // a zero max fee is assumed to be _no_
            // max fee has been set
            return null;
        }

        return Hbar.fromTinybars(transactionFee);
    }

    /**
     * Set the maximum transaction fee the operator (paying account) is willing to pay.
     *
     * @param maxTransactionFee the maximum transaction fee, in tinybars.
     * @return {@code this}
     */
    public final T setMaxTransactionFee(Hbar maxTransactionFee) {
        requireNotFrozen();
        bodyBuilder.setTransactionFee(maxTransactionFee.toTinybars());

        // noinspection unchecked
        return (T) this;
    }

    public final String getTransactionMemo() {
        return bodyBuilder.getMemo();
    }

    /**
     * Set a note or description that should be recorded in the transaction record (maximum length
     * of 100 characters).
     *
     * @param memo any notes or descriptions for this transaction.
     * @return {@code this}
     */
    public final T setTransactionMemo(String memo) {
        requireNotFrozen();
        bodyBuilder.setMemo(memo);

        // noinspection unchecked
        return (T) this;
    }

    public byte[] toBytes() {
        if (!this.isFrozen()) {
            throw new IllegalStateException("transaction must have been frozen before calculating the hash will be stable, try calling `freeze`");
        }

        if (transactions.size() != signedTransactions.size()) {
            for (var i = nextTransactionIndex; i < signedTransactions.size(); ++i) {
                transactions.add(com.hedera.hashgraph.sdk.proto.Transaction.newBuilder()
                    .setSignedTransactionBytes(
                        signedTransactions.get(i)
                            .setSigMap(signatures.get(i))
                            .buildPartial()
                            .toByteString()
                    ).buildPartial());
            }
        }

        var list = TransactionList.newBuilder();

        for (var transaction : transactions) {
            list.addTransactionList(transaction);
        }

        return list.build().toByteArray();
    }

    public byte[] getTransactionHash() {
        if (!this.isFrozen()) {
            throw new IllegalStateException("transaction must have been frozen before calculating the hash will be stable, try calling `freeze`");
        }

        return hash(signedTransactions.get(0).setSigMap(signatures.get(0)).buildPartial().toByteArray());
    }

    public Map<AccountId, byte[]> getTransactionHashPerNode() {
        if (!this.isFrozen()) {
            throw new IllegalStateException("transaction must have been frozen before calculating the hash will be stable, try calling `freeze`");
        }

        var hashes = new HashMap<AccountId, byte[]>();

        for (var i = 0; i < transactions.size(); i++) {
            hashes.put(nodeIds.get(i), hash(transactions.get(i).getSignedTransactionBytes().toByteArray()));
        }

        return hashes;
    }

    @Override
    public final TransactionId getTransactionId() {
        if (!bodyBuilder.hasTransactionID() && !this.isFrozen()) {
            throw new IllegalStateException("transaction must have been frozen before getting the transaction ID, try calling `freeze`");
        }

        return TransactionId.fromProtobuf(bodyBuilder.getTransactionID());
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
     * @param transactionId The TransactionId to be set
     * @return {@code this}
     * @see TransactionId
     */
    public final T setTransactionId(TransactionId transactionId) {
        requireNotFrozen();
        bodyBuilder.setTransactionID(transactionId.toProtobuf());

        // noinspection unchecked
        return (T) this;
    }

    public final T sign(PrivateKey privateKey) {
        return signWith(privateKey.getPublicKey(), privateKey::sign);
    }

    public T signWith(PublicKey publicKey, Function<byte[], byte[]> transactionSigner) {
        if (!isFrozen()) {
            throw new IllegalStateException("Signing requires transaction to be frozen");
        }

        transactions.clear();

        for (var index = 0; index < signedTransactions.size(); ++index) {
            var bodyBytes = signedTransactions.get(index).getBodyBytes().toByteArray();

            // NOTE: Yes the transactionSigner is invoked N times
            //  However for a verified/pin signature system it is reasonable to allow it to sign multiple
            //  transactions with identical details apart from the node ID
            var signatureBytes = transactionSigner.apply(bodyBytes);

            signatures
                .get(index)
                .addSigPair(publicKey.toSignaturePairProtobuf(signatureBytes));
        }

        // noinspection unchecked
        return (T) this;
    }

    public T signWithOperator(Client client) {
        var operator = client.getOperator();

        if (operator == null) {
            throw new IllegalStateException(
                "`client` must have an `operator` to sign with the operator");
        }

        if (!isFrozen()) {
            freezeWith(client);
        }

        if (keyAlreadySigned(operator.publicKey)) {
            // noinspection unchecked
            return (T) this;
        }

        return signWith(operator.publicKey, operator.transactionSigner);
    }

    protected boolean keyAlreadySigned(PublicKey key) {
        if (!signatures.isEmpty()) {
            for (var sigPair : signatures.get(0).getSigPairList()) {
                if (ByteString.copyFrom(key.toBytes()).startsWith(sigPair.getPubKeyPrefix())) {
                    // transaction already signed with the operator
                    return true;
                }
            }
        }

        return false;
    }

    public T addSignature(PublicKey publicKey, byte[] signature) {
        requireExactNode();

        if (!isFrozen()) {
            freeze();
        }

        if (keyAlreadySigned(publicKey)) {
            // noinspection unchecked
            return (T) this;
        }

        this.signatures.get(0).addSigPair(publicKey.toSignaturePairProtobuf(signature));

        // noinspection unchecked
        return (T) this;
    }

    public Map<AccountId, Map<PublicKey, byte[]>> getSignatures() {
        var map = new HashMap<AccountId, Map<PublicKey, byte[]>>(nodeIds.size());

        if (signatures.size() == 0) {
            return map;
        }

        for (int i = 0; i < nodeIds.size(); i++) {
            var sigMap = signatures.get(i);
            var nodeAccountId = nodeIds.get(i);
            var keyMap = map.computeIfAbsent(nodeAccountId, k -> new HashMap<>(sigMap.getSigPairCount()));
            for (var sigPair : sigMap.getSigPairList()) {
                keyMap.put(
                    PublicKey.fromBytes(sigPair.getPubKeyPrefix().toByteArray()),
                    sigPair.getEd25519().toByteArray()
                );
            }
        }

        return map;
    }

    protected boolean isFrozen() {
        return !signedTransactions.isEmpty();
    }

    protected void requireNotFrozen() {
        if (isFrozen()) {
            throw new IllegalStateException("transaction is immutable; it has at least one signature or has been explicitly frozen");
        }
    }

    protected void requireExactNode() {
        if (signedTransactions.size() != 1) {
            throw new IllegalStateException("transaction did not have exactly one node ID set");
        }
    }

    /**
     * Freeze this transaction from further modification to prepare for
     * signing or serialization.
     *
     * @return {@code this}
     */
    public T freeze() {
        return freezeWith(null);
    }

    /**
     * Freeze this transaction from further modification to prepare for
     * signing or serialization.
     * <p>
     * Will use the `Client`, if available, to generate a default Transaction ID and select 1/3
     * nodes to prepare this transaction for.
     *
     * @return {@code this}
     */
    public T freezeWith(@Nullable Client client) {
        if (isFrozen()) {
            // noinspection unchecked
            return (T) this;
        }

        if (client != null && bodyBuilder.getTransactionFee() == 0) {
            bodyBuilder.setTransactionFee(client.maxTransactionFee.toTinybars());
        }

        if (!bodyBuilder.hasTransactionID() && client != null) {
            var operator = client.getOperator();

            if (operator != null) {
                // Set a default transaction ID, generated from the operator account ID
                setTransactionId(TransactionId.generate(operator.accountId));
            } else {
                // no client means there must be an explicitly set node ID and transaction ID
                throw new IllegalStateException(
                    "`client` must have an `operator` or `transactionId` must be set");
            }
        }

        if (!onFreeze(bodyBuilder)) {
            // noinspection unchecked
            return (T) this;
        }

        if (nodeIds.isEmpty()) {
            if (client == null) {
                throw new IllegalStateException(
                    "`client` must be provided or both `nodeId` and `transactionId` must be set");
            }

            nodeIds = client.network.getNodeAccountIdsForExecute();
        }

        transactions = new ArrayList<>(nodeIds.size());
        signatures = new ArrayList<>(nodeIds.size());
        signedTransactions = new ArrayList<>(nodeIds.size());

        for (AccountId nodeId : nodeIds) {
            signatures.add(SignatureMap.newBuilder());
            signedTransactions.add(com.hedera.hashgraph.sdk.proto.SignedTransaction.newBuilder()
                .setBodyBytes(bodyBuilder
                    .setNodeAccountID(nodeId.toProtobuf())
                    .build()
                    .toByteString()
                ));
        }

        // noinspection unchecked
        return (T) this;
    }

    /**
     * Called in {@link #freezeWith(Client)} just before the transaction
     * body is built. The intent is for the derived class to assign
     * their data variant to the transaction body.
     */
    abstract boolean onFreeze(TransactionBody.Builder bodyBuilder);

    @Override
    final com.hedera.hashgraph.sdk.proto.Transaction makeRequest() {
        if (transactions.size() <= nextTransactionIndex) {
            transactions.add(
                com.hedera.hashgraph.sdk.proto.Transaction.newBuilder()
                    .setSignedTransactionBytes(
                        signedTransactions.get(nextTransactionIndex)
                            .setSigMap(signatures.get(nextTransactionIndex))
                            .build().toByteString()
                    )
                    .build()
            );
        }

        return transactions.get(nextTransactionIndex);
    }

    @Override
    final TransactionResponse mapResponse(
        com.hedera.hashgraph.sdk.proto.TransactionResponse transactionResponse,
        AccountId nodeId,
        com.hedera.hashgraph.sdk.proto.Transaction request
    ) {
        freeze();
        var transactionId = Objects.requireNonNull(getTransactionId());
        return new TransactionResponse(nodeId, transactionId, hash(request.toByteArray()));
    }

    @Override
    final Status mapResponseStatus(com.hedera.hashgraph.sdk.proto.TransactionResponse transactionResponse) {
        return Status.valueOf(transactionResponse.getNodeTransactionPrecheckCode());
    }

    @Override
    void advanceRequest() {
        // each time buildNext is called we move our cursor to the next transaction
        // wrapping around to ensure we are cycling
        nextTransactionIndex = (nextTransactionIndex + 1) % transactions.size();
    }

    @Override
    CompletableFuture<Void> onExecuteAsync(Client client) {
        if (!isFrozen()) {
            freezeWith(client);
        }

        var operatorId = client.getOperatorAccountId();

        if (operatorId != null && operatorId.equals(getTransactionId().accountId)) {
            // on execute, sign each transaction with the operator, if present
            // and we are signing a transaction that used the default transaction ID
            signWithOperator(client);
        }

        return CompletableFuture.completedFuture(null);
    }

    @Override
    @SuppressWarnings("LiteProtoToString")
    public String toString() {
        // NOTE: regex is for removing the instance address from the default debug output
        return bodyBuilder.buildPartial().toString().replaceAll("@[A-Za-z0-9]+", "");
    }
}
