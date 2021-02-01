package com.nchain.jcl.store.foundationDB.blockStore

import com.nchain.jcl.store.blockStore.BlockStore
import com.nchain.jcl.store.blockStore.BlockStoreBlocksSpecBase
import com.nchain.jcl.store.foundationDB.DockerTestUtils
import com.nchain.jcl.store.foundationDB.StoreFactory

/**
 * Testing class for basic Scenarios for Blocks.
 * @see BlockStoreBlocksSpecBase
 */
class BlockStoreBlocksSpec extends BlockStoreBlocksSpecBase {

    // Start & Stop FoundationDB in Docker Container (check DockerTestUtils for details)...
    def setupSpec()     { DockerTestUtils.startDocker()}
    def cleanupSpec()   { DockerTestUtils.stopDocker()}

    @Override
    BlockStore getInstance(String netId, boolean triggerBlockEvents, boolean triggerTxEvents) {
        return StoreFactory.getInstance(netId, triggerBlockEvents, triggerTxEvents)
    }
}