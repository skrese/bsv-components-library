package com.nchain.jcl.store.foundationDB.common

import com.nchain.jcl.store.blockStore.BlockStore
import com.nchain.jcl.store.common.IteratorSpecBase
import com.nchain.jcl.store.foundationDB.FDBTestUtils
import com.nchain.jcl.store.foundationDB.blockStore.BlockStoreFDB
import com.nchain.jcl.store.foundationDB.StoreFactory

import java.util.function.Function

/**
 * Testing iterator for the FBDIterator, which is a very basic class and plays a big role in the FoundationDB
 * implementation of the JCL-Store
 */

class FDBIteratorSpec extends IteratorSpecBase {

    // Start & Stop FoundationDB in Docker Container (check DockerTestUtils for details)...
    def setupSpec()     { FDBTestUtils.checkFDBBefore()}
    def cleanupSpec()   { FDBTestUtils.checkFDBAfter()}

    @Override
    BlockStore getInstance(String netId, boolean triggerBlockEvents, boolean triggerTxEvents) {
        return StoreFactory.getInstance(netId, triggerBlockEvents, triggerTxEvents)
    }

    @Override
    Iterator<byte[]> createIteratorForTxs(BlockStore db, String preffix, String suffix) {

        // We define a Function that returns the relative Key, that is the last Key to the right, after trimming all the
        // "directories" from the left:

        Function<Map.Entry<byte[], byte[]>, String> itemBuilder = { key ->


            // Each Key returned by this Iterator represents a Tx Key, which is made of:
            // [txDirKey] + [TX_KEY]
            // [TX_KEY] example: tx:1716126a699c8a76fb6ae591661a22622ea0909ff57eb143fe2f479694b75792

            // We need to return ONLY the [TX_KEY], so we remove the rest:

            int numBytesToRemove = ((BlockStoreFDB) db).fullKeyForTxs().length
            int byteTxKeyPos = numBytesToRemove
            int txKeyLength = key.key.length - numBytesToRemove;

            byte[] result = new byte[key.key.length - numBytesToRemove]
            System.arraycopy(key.key, byteTxKeyPos, result, 0, txKeyLength)
            return result;
        }

        BlockStoreFDB blockStoreFDB = (BlockStoreFDB) db;
        byte[] keyPreffix = blockStoreFDB.fullKey(blockStoreFDB.fullKeyForTxs(), preffix)
        byte[] keySuffix = (suffix != null) ? suffix.getBytes() : null;

        FDBSafeIterator.FDBSafeIteratorBuilder<String> itBuilder = FDBSafeIterator.<String>safeBuilder()
            .database(blockStoreFDB.db)
            .startingWithPreffix(keyPreffix)
            .endingWithSuffix(keySuffix)
            .buildItemBy(itemBuilder)
        return itBuilder.build()
    }

}
