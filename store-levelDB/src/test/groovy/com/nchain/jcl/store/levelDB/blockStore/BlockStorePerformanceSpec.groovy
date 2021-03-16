package com.nchain.jcl.store.levelDB.blockStore

import com.nchain.jcl.store.blockStore.BlockStore
import com.nchain.jcl.store.levelDB.StoreFactory

/**
 * Performance Testing class for the BlockStore
 */
class BlockStorePerformanceSpec extends com.nchain.jcl.store.blockStore.BlockStorePerformanceSpec {
    @Override BlockStore getInstance(String netId, boolean triggerBlockEvents, boolean triggerTxEvents) {
        return StoreFactory.getInstance(netId, triggerBlockEvents, triggerTxEvents)
    }
}
