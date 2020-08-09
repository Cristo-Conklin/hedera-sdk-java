package com.hedera.hashgraph.sdk;

import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.errorprone.annotations.Var;
import com.google.gson.Gson;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Managed client for use on the Hedera Hashgraph network.
 */
public final class Client implements AutoCloseable {
    private static final String HEDERA_TESTNET_MIRROR_NODE = "hcs.testnet.mirrornode.hedera.com:5600";
    private static final Hbar DEFAULT_MAX_QUERY_PAYMENT = new Hbar(1);
    private static final Hbar DEFAULT_MAX_TRANSACTION_FEE = new Hbar(1);

    Hbar maxTransactionFee = DEFAULT_MAX_QUERY_PAYMENT;
    Hbar maxQueryPayment = DEFAULT_MAX_TRANSACTION_FEE;

    private final Map<AccountId, String> network;
    private Iterator<AccountId> nodes;

    private Map<AccountId, ManagedChannel> nodeChannels;

    private Map<String, ManagedChannel> mirrorChannels;
    private Iterator<String> mirrors;

    final ExecutorService executor;

    @Nullable
    private Operator operator;

    Duration requestTimeout = Duration.ofMinutes(2);

    Client(Map<AccountId, String> network) {
        var threadFactory = new ThreadFactoryBuilder()
            .setNameFormat("hedera-sdk-%d")
            .setDaemon(true)
            .build();

        this.executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            threadFactory);

        this.network = network;
        this.nodeChannels = new HashMap<>(network.size());

        this.mirrorChannels = new HashMap<>(1);
        this.mirrors = this.mirrorChannels.keySet().iterator();

        setNetworkNodes(network);
    }

    private void setNetworkNodes(Map<AccountId, String> network) {
        // Take all given node account IDs, shuffle, and prepare an infinite iterator for use in [getNextNodeId]
        var allNodes = new ArrayList<>(network.keySet());
        Collections.shuffle(allNodes, ThreadLocalSecureRandom.current());
        this.nodes = Iterables.cycle(allNodes).iterator();
    }

    public void setMirrorNetwork(List<String> mirrorNetwork) {
        var mirrors = new ArrayList<>(mirrorNetwork);
        Collections.shuffle(mirrors, ThreadLocalSecureRandom.current());
        this.mirrors = Iterables.cycle(mirrors).iterator();
    }

    /**
     * Construct a client given a set of nodes.
     *
     * <p>It is the responsibility of the caller to ensure that all nodes in the map are part of the
     * same Hedera network. Failure to do so will result in undefined behavior.
     *
     * <p>The client will load balance all requests to Hedera using a simple round-robin scheme to
     * chose nodes to send transactions to. For one transaction, at most 1/3 of the nodes will be
     * tried.
     *
     * @param network the map of node IDs to node addresses that make up the network.
     * @return {@link com.hedera.hashgraph.sdk.Client}
     */
    public static Client forNetwork(Map<AccountId, String> network) {
        return new Client(network);
    }

    /**
     * Construct a Hedera client pre-configured for <a
     * href="https://docs.hedera.com/guides/mainnet/address-book#mainnet-address-book">Mainnet
     * access</a>.
     *
     * @return {@link com.hedera.hashgraph.sdk.Client}
     */
    public static Client forMainnet() {
        var network = new HashMap<AccountId, String>();
        network.put(new AccountId(3), "35.237.200.180:50211");
        network.put(new AccountId(4), "35.186.191.247:50211");
        network.put(new AccountId(5), "35.192.2.25:50211");
        network.put(new AccountId(6), "35.199.161.108:50211");
        network.put(new AccountId(7), "35.203.82.240:50211");
        network.put(new AccountId(8), "35.236.5.219:50211");
        network.put(new AccountId(9), "35.197.192.225:50211");
        network.put(new AccountId(10), "35.242.233.154:50211");
        network.put(new AccountId(11), "35.240.118.96:50211");
        network.put(new AccountId(12), "35.204.86.32:50211");

        return Client.forNetwork(network);
    }

    /**
     * Construct a Hedera client pre-configured for <a
     * href="https://docs.hedera.com/guides/testnet/nodes">Testnet access</a>.
     *
     * @return {@link com.hedera.hashgraph.sdk.Client}
     */
    public static Client forTestnet() {
        var network = new HashMap<AccountId, String>();
        network.put(new AccountId(3), "0.testnet.hedera.com:50211");
        network.put(new AccountId(4), "1.testnet.hedera.com:50211");
        network.put(new AccountId(5), "2.testnet.hedera.com:50211");
        network.put(new AccountId(6), "3.testnet.hedera.com:50211");

        var client = Client.forNetwork(network);
        client.setMirrorNetwork(List.of(HEDERA_TESTNET_MIRROR_NODE));
        return client;
    }

    /**
     * Configure a client based off the given JSON string.
     *
     * @param json The json string containing the client configuration
     * @return {@link com.hedera.hashgraph.sdk.Client}
     */
    public static Client fromJson(String json) {
        return fromJson(new StringReader(json));
    }

    /**
     * Configure a client based off the given JSON reader.
     *
     * @param json The Reader containing the client configuration
     * @return {@link com.hedera.hashgraph.sdk.Client}
     */
    public static Client fromJson(Reader json) {
        Config config = new Gson().fromJson(json, Config.class);

        Map<AccountId, String> nodes = new HashMap<>(config.network.size());

        for (Map.Entry<String, String> entry : config.network.entrySet()) {
            nodes.put(AccountId.fromString(entry.getValue()), entry.getKey());
        }

        Client client = new Client(nodes);

        if (config.operator != null) {
            AccountId operatorAccount = AccountId.fromString(config.operator.accountId);
            PrivateKey privateKey = PrivateKey.fromString(config.operator.privateKey);

            client.setOperator(operatorAccount, privateKey);
        }

        if (config.mirrorNetwork != null) {
            client.setMirrorNetwork(config.mirrorNetwork);
        }

        return client;
    }

    /**
     * Configure a client based on a JSON file at the given path.
     *
     * @param fileName The string containing the file path
     * @return {@link com.hedera.hashgraph.sdk.Client}
     * @throws IOException if IO operations fail
     */
    public static Client fromJsonFile(String fileName) throws IOException {
        return fromJsonFile(new File(fileName));
    }

    /**
     * Configure a client based on a JSON file.
     *
     * @param file The file containing the client configuration
     * @return {@link com.hedera.hashgraph.sdk.Client}
     * @throws IOException if IO operations fail
     */
    public static Client fromJsonFile(File file) throws IOException {
        return fromJson(Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8));
    }

    /**
     * Replace all nodes in this Client with a new set of nodes (e.g. for an Address Book update).
     * <p>
     *
     * @param nodes a map of node account ID to node URL.
     * @return {@code this} for fluent API usage.
     */
    public Client setNetwork(Map<AccountId, String> nodes) throws InterruptedException {
        setNetworkNodes(nodes);
        @Var ManagedChannel channel = null;

        for (Map.Entry<AccountId, String> node : this.network.entrySet()) {
            String newNodeUrl = nodes.get(node.getKey());

            // node hasn't changed
            if (node.getValue().equals(newNodeUrl)) {
                continue;
            }

            // Set new node address
            node.setValue(newNodeUrl);

            if (newNodeUrl == null) {
                // set null for removal, should be fixed here to just remove instead of setting null then removing
                nodeChannels.put(node.getKey(), null);
            } else if (nodeChannels.get(node.getKey()) != null && !nodeChannels.get(node.getKey()).authority().equals(newNodeUrl)) {
                // Shutdown channel before replacing address
                nodeChannels.get(node.getKey()).shutdown().awaitTermination(30, TimeUnit.SECONDS);
                channel = ManagedChannelBuilder.forTarget(newNodeUrl)
                    .usePlaintext()
                    .userAgent(getUserAgent())
                    .executor(executor)
                    .build();
                nodeChannels.put(node.getKey(), channel);
            }
        }

        // remove
        this.network.values().removeAll(Collections.singleton(null));
        this.nodeChannels.values().removeAll(Collections.singleton(null));

        // add new nodes
        for (Map.Entry<AccountId, String> node : nodes.entrySet()) {
            this.network.put(node.getKey(), node.getValue());
            // .getNetworkChannel() will add the node and channel from network
            this.getNetworkChannel(node.getKey());
        }

        return this;
    }

    /**
     * Set the account that will, by default, be paying for transactions and queries built with
     * this client.
     * <p>
     * The operator account ID is used to generate the default transaction ID for all transactions executed with
     * this client.
     * <p>
     * The operator private key is used to sign all transactions executed by this client.
     *
     * @param accountId  The AccountId of the operator
     * @param privateKey The PrivateKey of the operator
     * @return {@code this}
     */
    public Client setOperator(AccountId accountId, PrivateKey privateKey) {
        return setOperatorWith(accountId, privateKey.getPublicKey(), privateKey::sign);
    }

    /**
     * Sets the account that will, by default, by paying for transactions and queries built with
     * this client.
     * <p>
     * The operator account ID is used to generate a default transaction ID for all transactions
     * executed with this client.
     * <p>
     * The `transactionSigner` is invoked to sign all transactions executed by this client.
     *
     * @param accountId         The AccountId of the operator
     * @param publicKey         The PrivateKey of the operator
     * @param transactionSigner The signer for the operator
     * @return {@code this}
     */
    public Client setOperatorWith(AccountId accountId, PublicKey publicKey, Function<byte[], byte[]> transactionSigner) {
        this.operator = new Operator(accountId, publicKey, transactionSigner);
        return this;
    }

    /**
     * Get the ID of the operator. Useful when the client was constructed from file.
     *
     * @return {AccountId}
     */
    @Nullable
    public AccountId getOperatorId() {
        if (operator == null) {
            return null;
        }

        return operator.accountId;
    }

    /**
     * Get the key of the operator. Useful when the client was constructed from file.
     *
     * @return {PublicKey}
     */
    @Nullable
    public PublicKey getOperatorKey() {
        if (operator == null) {
            return null;
        }

        return operator.publicKey;
    }

    /**
     * Set the maximum fee to be paid for transactions executed by this client.
     * <p>
     * Because transaction fees are always maximums, this will simply add a call to
     * {@link TransactionBuilder#setMaxTransactionFee(Hbar)} on every new transaction. The actual
     * fee assessed for a given transaction may be less than this value, but never greater.
     *
     * @param maxTransactionFee The Hbar to be set
     * @return {@code this}
     */
    public Client setMaxTransactionFee(Hbar maxTransactionFee) {
        if (maxTransactionFee.toTinybars() < 0) {
            throw new IllegalArgumentException("maxTransactionFee must be non-negative");
        }

        this.maxTransactionFee = maxTransactionFee;
        return this;
    }

    /**
     * Set the maximum default payment allowable for queries.
     * <p>
     * When a query is executed without an explicit {@link QueryBuilder#setQueryPayment(Hbar)} call,
     * the client will first request the cost
     * of the given query from the node it will be submitted to and attach a payment for that amount
     * from the operator account on the client.
     * <p>
     * If the returned value is greater than this value, a
     * {@link MaxQueryPaymentExceededException} will be thrown from
     * {@link QueryBuilder#execute(Client)} or returned in the second callback of
     * {@link QueryBuilder#executeAsync(Client, Consumer, Consumer)}.
     * <p>
     * Set to 0 to disable automatic implicit payments.
     *
     * @param maxQueryPayment The Hbar to be set
     * @return {@code this}
     */
    public Client setMaxQueryPayment(Hbar maxQueryPayment) {
        if (maxQueryPayment.toTinybars() < 0) {
            throw new IllegalArgumentException("maxQueryPayment must be non-negative");
        }

        this.maxQueryPayment = maxQueryPayment;
        return this;
    }

    public Client setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
        return this;
    }

    @Nullable
    Operator getOperator() {
        return this.operator;
    }

    /**
     * Initiates an orderly shutdown of all channels (to the Hedera network) in which preexisting
     * transactions or queries continue but more would be immediately cancelled.
     *
     * <p>After this method returns, this client can be re-used. Channels will be re-established as
     * needed.
     */
    @Override
    public synchronized void close() {
        close(Duration.ofSeconds(30));
    }

    /**
     * Initiates an orderly shutdown of all channels (to the Hedera network) in which preexisting
     * transactions or queries continue but more would be immediately cancelled.
     *
     * <p>After this method returns, this client can be re-used. Channels will be re-established as
     * needed.
     *
     * @param timeout The Duration to be set
     */
    public void close(Duration timeout) {
        // initialize shutdown for all channels
        // this should not block
        for (var channel : Iterables.concat(mirrorChannels.values(), mirrorChannels.values())) {
            channel.shutdown();
        }

        // wait for all channels to shutdown
        for (var channel : Iterables.concat(mirrorChannels.values(), mirrorChannels.values())) {
            try {
                channel.awaitTermination(timeout.getSeconds(), TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        // preemptively clear memory for the channels map
        nodeChannels.clear();
        mirrorChannels.clear();

        executor.shutdown();

        try {
            executor.awaitTermination(timeout.getSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // Get the next node ID, following a round-robin distribution with a randomized start point
    synchronized AccountId getNextNodeId() {
        return nodes.next();
    }

    synchronized ManagedChannel getNextMirrorChannel() {
        return getMirrorChannel(mirrors.next());
    }

    int getNumberOfNodesForTransaction() {
        return (network.size() + 3 - 1) / 3;
    }

    // Return or establish a channel for a given node ID
    synchronized ManagedChannel getNetworkChannel(AccountId nodeId) {
        @Var var channel = nodeChannels.get(nodeId);

        if (channel != null) {
            return channel;
        }

        var address = network.get(nodeId);

        if (address != null) {
            // TODO: Determine if we should/should not have `keepAlive()` on this channel
            channel = ManagedChannelBuilder.forTarget(address)
                .usePlaintext()
                .userAgent(getUserAgent())
                .executor(executor)
                .build();

            nodeChannels.put(nodeId, channel);
        }

        if (channel == null) {
            throw new IllegalArgumentException("Node Id does not exist");
        }

        return channel;
    }

    // Return or establish a channel for a given node ID
    synchronized ManagedChannel getMirrorChannel(String address) {
        @Var var channel = mirrorChannels.get(address);

        if (channel != null) {
            return channel;
        }

        channel = ManagedChannelBuilder.forTarget(address)
            .keepAliveTime(2, TimeUnit.MINUTES)
            .usePlaintext()
            .userAgent(getUserAgent())
            .executor(executor)
            .build();

        mirrorChannels.put(address, channel);

        return channel;
    }

    private String getUserAgent() {
        var thePackage = getClass().getPackage();
        var implementationVersion = thePackage != null ? thePackage.getImplementationVersion() : null;
        return "hedera-sdk-java/" + ((implementationVersion != null) ? ("v" + implementationVersion) : "DEV");
    }

    static class Operator {
        final AccountId accountId;
        final PublicKey publicKey;
        final Function<byte[], byte[]> transactionSigner;

        Operator(AccountId accountId, PublicKey publicKey, Function<byte[], byte[]> transactionSigner) {
            this.accountId = accountId;
            this.publicKey = publicKey;
            this.transactionSigner = transactionSigner;
        }
    }

    private static class Config {
        private HashMap<String, String> network = new HashMap<>();

        @Nullable
        private ConfigOperator operator;

        @Nullable
        private List<String> mirrorNetwork = new ArrayList<>();

        private static class ConfigOperator {
            private String accountId = "";
            private String privateKey = "";
        }
    }
}
