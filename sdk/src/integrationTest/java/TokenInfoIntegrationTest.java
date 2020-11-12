import com.hedera.hashgraph.sdk.*;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenInfoIntegrationTest {
    private static final AccountId OPERATOR_ID = AccountId.fromString(Objects.requireNonNull(System.getProperty("OPERATOR_ID")));
    private static final PrivateKey OPERATOR_KEY = PrivateKey.fromString(Objects.requireNonNull(System.getProperty("OPERATOR_KEY")));

    @Test
    void test() {
        assertDoesNotThrow(() -> {
            var client = IntegrationTestClientManager.getClient();

            TransactionResponse response = new TokenCreateTransaction()
                .setTokenName("ffff")
                .setTokenSymbol("F")
                .setDecimals(3)
                .setInitialSupply(1000000)
                .setTreasuryAccountId(OPERATOR_ID)
                .setAdminKey(OPERATOR_KEY.getPublicKey())
                .setFreezeKey(OPERATOR_KEY.getPublicKey())
                .setWipeKey(OPERATOR_KEY.getPublicKey())
                .setKycKey(OPERATOR_KEY.getPublicKey())
                .setSupplyKey(OPERATOR_KEY.getPublicKey())
                .setFreezeDefault(false)
                .execute(client);

            TokenId tokenId = response.getReceipt(client).tokenId;

                TokenInfo info = new TokenInfoQuery()
                    .setNodeAccountIds(Collections.singletonList(response.nodeId))
                    .setQueryPayment(new Hbar(2))
                    .setTokenId(tokenId)
                    .execute(client);

            assertTrue(info.tokenId.equals(tokenId));
            assertTrue(info.name.equals("ffff"));
            assertTrue(info.symbol.equals("F"));
            assertTrue(info.decimals == 3);
            assertTrue(info.treasury.equals(OPERATOR_ID));
            assertTrue(info.adminKey.toString().equals(OPERATOR_KEY.getPublicKey().toString()));
            assertTrue(info.kycKey.toString().equals(OPERATOR_KEY.getPublicKey().toString()));
            assertTrue(info.freezeKey.toString().equals(OPERATOR_KEY.getPublicKey().toString()));
            assertTrue(info.wipeKey.toString().equals(OPERATOR_KEY.getPublicKey().toString()));
            assertTrue(info.supplyKey.toString().equals(OPERATOR_KEY.getPublicKey().toString()));
            assertFalse(info.defaultFreezeStatus);
            assertFalse(info.defaultKycStatus);

                new TokenDeleteTransaction()
                    .setNodeAccountIds(Collections.singletonList(response.nodeId))
                    .setTokenId(tokenId)
                    .execute(client)
                    .getReceipt(client);
        });
    }
}
