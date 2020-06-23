package com.hedera.hashgraph.sdk;

import com.hedera.hashgraph.sdk.account.AccountId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

class ClientTest {
    @Test
    @DisplayName("fromJson() functions correctly")
    void testFromJson() {
        // put it in a file for nicer formatting
        InputStream clientConfig = ClientTest.class.getClassLoader()
            .getResourceAsStream("client-config.json");

        Assertions.assertNotNull(clientConfig);

        Client.fromJson(new InputStreamReader(clientConfig));

        // put it in a file for nicer formatting
        InputStream clientConfigWithOperator = ClientTest.class.getClassLoader()
            .getResourceAsStream("client-config-with-operator.json");

        Assertions.assertNotNull(clientConfigWithOperator);

        Client.fromJson(new InputStreamReader(clientConfigWithOperator));
    }

    @Test
    @DisplayName("replaceNodes() functions correctly")
    void testReplaceNodes() {
        Map<AccountId, String> nodes = new HashMap<>();
        nodes.put(new AccountId(3), "0.testnet.hedera.com:50211");
        nodes.put(new AccountId(4), "1.testnet.hedera.com:50211");

        Client client = new Client(nodes);

        nodes = new HashMap<>();
        nodes.put(new AccountId(5), "2.testnet.hedera.com:50211");
        nodes.put(new AccountId(6), "3.testnet.hedera.com:50211");

        client.replaceNodes(nodes);

        Assertions.assertNotNull(client.getNodeForId(new AccountId(5)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> client.getNodeForId(new AccountId(3)));
    }
}
