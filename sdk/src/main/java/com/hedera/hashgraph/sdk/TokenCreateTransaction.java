package com.hedera.hashgraph.sdk;

import com.hedera.hashgraph.sdk.proto.TokenCreateTransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionBody;
import com.hedera.hashgraph.sdk.proto.TransactionResponse;
import com.hedera.hashgraph.sdk.proto.TokenServiceGrpc;
import io.grpc.MethodDescriptor;

public class TokenCreateTransaction extends Transaction<TokenCreateTransaction> {
    private final TokenCreateTransactionBody.Builder builder;

    public TokenCreateTransaction() {
        builder = TokenCreateTransactionBody.newBuilder();

        setAutoRenewPeriod(DEFAULT_AUTO_RENEW_PERIOD.toMillis() / 1000);
    }

    TokenCreateTransaction(TransactionBody body) {
        super(body);

        builder = body.getTokenCreation().toBuilder();
    }

    public String getName() {
        return builder.getName();
    }

    public TokenCreateTransaction setName(String name) {
        requireNotFrozen();
        builder.setName(name);
        return this;
    }

    public String getSymbol() {
        return builder.getSymbol();
    }

    public TokenCreateTransaction setSymbol(String symbol) {
        requireNotFrozen();
        builder.setSymbol(symbol);
        return this;
    }

    public int getDecimals() {
        return builder.getDecimals();
    }

    public TokenCreateTransaction setDecimals(int decimals) {
        requireNotFrozen();
        builder.setDecimals(decimals);
        return this;
    }

    public long getInitialSupply() {
        return builder.getInitialSupply();
    }

    public TokenCreateTransaction setInitialSupply(long initialSupply) {
        requireNotFrozen();
        builder.setInitialSupply(initialSupply);
        return this;
    }

    public AccountId getTreasury() {
        return AccountId.fromProtobuf(builder.getTreasury());
    }

    public TokenCreateTransaction setTreasury(AccountId accountId) {
        requireNotFrozen();
        builder.setTreasury(accountId.toProtobuf());
        return this;
    }

    public Key getAdminKey() {
        return Key.fromProtobuf(builder.getAdminKey());
    }

    public TokenCreateTransaction setAdminKey(Key key) {
        requireNotFrozen();
        builder.setAdminKey(key.toKeyProtobuf());
        return this;
    }

    public Key getKycKey() {
        return Key.fromProtobuf(builder.getKycKey());
    }

    public TokenCreateTransaction setKycKey(Key key) {
        requireNotFrozen();
        builder.setKycKey(key.toKeyProtobuf());
        return this;
    }

    public Key getFreezeKey() {
        return Key.fromProtobuf(builder.getFreezeKey());
    }

    public TokenCreateTransaction setFreezeKey(Key key) {
        requireNotFrozen();
        builder.setFreezeKey(key.toKeyProtobuf());
        return this;
    }

    public Key getWipeKey() {
        return Key.fromProtobuf(builder.getWipeKey());
    }

    public TokenCreateTransaction setWipeKey(Key key) {
        requireNotFrozen();
        builder.setWipeKey(key.toKeyProtobuf());
        return this;
    }

    public Key getSupplyKey() {
        return Key.fromProtobuf(builder.getSupplyKey());
    }

    public TokenCreateTransaction setSupplyKey(Key key) {
        requireNotFrozen();
        builder.setSupplyKey(key.toKeyProtobuf());
        return this;
    }

    public boolean getFreezeDefault() {
        return builder.getFreezeDefault();
    }

    public TokenCreateTransaction setFreezeDefault(boolean freezeDefault) {
        requireNotFrozen();
        builder.setFreezeDefault(freezeDefault);
        return this;
    }

    public long getExpirationTime() {
        return builder.getExpiry();
    }

    public TokenCreateTransaction setExpirationTime(long expirationTime) {
        requireNotFrozen();
        builder.setExpiry(expirationTime);
        return this;
    }

    public AccountId getAutoRenewAccount() {
        return AccountId.fromProtobuf(builder.getAutoRenewAccount());
    }

    public TokenCreateTransaction setAutoRenewAccount(AccountId accountId) {
        requireNotFrozen();
        builder.setAutoRenewAccount(accountId.toProtobuf());
        return this;
    }

    public long getAutoRenewPeriod() {
        return builder.getAutoRenewPeriod();
    }

    public TokenCreateTransaction setAutoRenewPeriod(long period) {
        requireNotFrozen();
        builder.setAutoRenewPeriod(period);
        return this;
    }

    @Override
    MethodDescriptor<com.hedera.hashgraph.sdk.proto.Transaction, TransactionResponse> getMethodDescriptor() {
        return TokenServiceGrpc.getCreateTokenMethod();
    }

    @Override
    boolean onFreeze(TransactionBody.Builder bodyBuilder) {
        bodyBuilder.setTokenCreation(builder);
        return true;
    }
}
