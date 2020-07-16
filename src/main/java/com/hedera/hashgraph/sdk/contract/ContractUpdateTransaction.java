package com.hedera.hashgraph.sdk.contract;

import com.hedera.hashgraph.proto.ContractUpdateTransactionBody;
import com.hedera.hashgraph.proto.SmartContractServiceGrpc;
import com.hedera.hashgraph.proto.Transaction;
import com.hedera.hashgraph.proto.TransactionResponse;
import com.hedera.hashgraph.sdk.DurationHelper;
import com.hedera.hashgraph.sdk.TimestampHelper;
import com.hedera.hashgraph.sdk.SingleTransactionBuilder;
import com.hedera.hashgraph.sdk.account.AccountId;
import com.hedera.hashgraph.sdk.crypto.PublicKey;
import com.hedera.hashgraph.sdk.file.FileId;

import java.time.Duration;
import java.time.Instant;

import io.grpc.MethodDescriptor;

public class ContractUpdateTransaction extends SingleTransactionBuilder<ContractUpdateTransaction> {
    private final ContractUpdateTransactionBody.Builder builder = bodyBuilder.getContractUpdateInstanceBuilder();

    public ContractUpdateTransaction() {
        super();
    }

    public ContractUpdateTransaction setContractId(ContractId contract) {
        builder.setContractID(contract.toProto());
        return this;
    }

    public ContractUpdateTransaction setExpirationTime(Instant expiration) {
        builder.setExpirationTime(TimestampHelper.timestampFrom(expiration));
        return this;
    }

    // fixme: update to the new Key interface
    public ContractUpdateTransaction setAdminKey(PublicKey key) {
        builder.setAdminKey(key.toKeyProto());
        return this;
    }

    public ContractUpdateTransaction setProxyAccount(AccountId account) {
        builder.setProxyAccountID(account.toProto());
        return this;
    }

    public ContractUpdateTransaction setAutoRenewPeriod(Duration duration) {
        builder.setAutoRenewPeriod(DurationHelper.durationFrom(duration));
        return this;
    }

    public ContractUpdateTransaction setBytecodeFileId(FileId file) {
        builder.setFileID(file.toProto());
        return this;
    }

    /**
     * Set a memo for the contract itself, as opposed to for this transaction
     * (via {@link #setTransactionMemo(String)}).
     */
    public ContractUpdateTransaction setContractMemo(String memo) {
        builder.setMemo(memo);
        return this;
    }

    @Override
    protected MethodDescriptor<Transaction, TransactionResponse> getMethod() {
        return SmartContractServiceGrpc.getUpdateContractMethod();
    }

    @Override
    protected void doValidate() {
        require(builder.hasContractID(), ".setContractId() required");
    }
}
