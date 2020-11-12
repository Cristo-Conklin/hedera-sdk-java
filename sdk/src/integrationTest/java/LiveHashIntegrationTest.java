import com.hedera.hashgraph.sdk.AccountCreateTransaction;
import com.hedera.hashgraph.sdk.AccountDeleteTransaction;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.HederaPreCheckStatusException;
import com.hedera.hashgraph.sdk.LiveHashAddTransaction;
import com.hedera.hashgraph.sdk.LiveHashDeleteTransaction;
import com.hedera.hashgraph.sdk.LiveHashQuery;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.Status;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;
import org.threeten.bp.Duration;

import java.util.Collections;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LiveHashIntegrationTest {
    private static final byte[] hash = Hex.decode("100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002");
    @Test
    void test() {
        assertDoesNotThrow(() -> {

            var client = IntegrationTestClientManager.getClient();
            var operatorId = client.getOperatorAccountId();

            var key = PrivateKey.generate();

            var receipt = new AccountCreateTransaction()
                .setKey(key)
                .setMaxTransactionFee(new Hbar(2))
                .setInitialBalance(new Hbar(1))
                .setNodeAccountIds(Collections.singletonList(new AccountId(5)))
                .execute(client)
                .transactionId
                .getReceipt(client);

            assertNotNull(receipt.accountId);
            assertTrue(Objects.requireNonNull(receipt.accountId).num > 0);

            var account = receipt.accountId;

            try {
                new LiveHashAddTransaction()
                    .setAccountId(account)
                    .setDuration(Duration.ofDays(30))
                    .setNodeAccountIds(Collections.singletonList(new AccountId(5)))
                    .setHash(hash)
                    .setKeys(key)
                    .execute(client);
            } catch (HederaPreCheckStatusException e) {
                assertEquals(e.status, Status.NOT_SUPPORTED);
            }

//            @Var AccountInfo info = new AccountInfoQuery()
//                .setAccountId(account)
//                .execute(client);
//
//            assertEquals(info.liveHashes.size(), 1);
//            assertEquals(info.liveHashes.get(0).accountId, account);
//            assertEquals(info.liveHashes.get(0).duration, Duration.ofDays(30));
//            assertEquals(info.liveHashes.get(0).hash.toByteArray(), hash);
//
//            PublicKey[] keys = (PublicKey[])info.liveHashes.get(0).keys.toArray();
//            assertEquals(keys.length, 1);
//            assertEquals(keys[0], key.getPublicKey());

            try {
                new LiveHashDeleteTransaction()
                    .setAccountId(account)
                    .setNodeAccountIds(Collections.singletonList(new AccountId(5)))
                    .setHash(hash)
                    .execute(client);
            } catch (HederaPreCheckStatusException e) {
                assertEquals(e.status, Status.NOT_SUPPORTED);
            }

            assertDoesNotThrow(() -> {
                new LiveHashQuery()
                    .setAccountId(account)
                    .setNodeAccountIds(Collections.singletonList(new AccountId(5)))
                    .setHash(hash)
                    .execute(client);
//                assertEquals(liveHash.accountId, account);
//                assertEquals(liveHash.duration, Duration.ofDays(30));
//                assertEquals(liveHash.hash.toByteArray(), hash);
//
//                PublicKey[] keys = (PublicKey[])liveHash.keys.toArray();
//                assertEquals(keys.length, 1);
//                assertEquals(keys[0], key.getPublicKey());
            });
//
//            info = new AccountInfoQuery()
//                .setAccountId(account)
//                .execute(client);
//
//            assertEquals(info.liveHashes.size(), 0);
//
            new AccountDeleteTransaction()
                .setAccountId(account)
                .setNodeAccountIds(Collections.singletonList(new AccountId(5)))
                .setTransferAccountId(operatorId)
                .execute(client);

            client.close();
        });
    }
}
