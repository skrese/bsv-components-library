package com.nchain.jcl.store.foundationDB.blockStore;

import com.nchain.jcl.store.keyValue.blockStore.BlockStoreKeyValueConfig;
import com.nchain.jcl.tools.config.RuntimeConfig;
import com.nchain.jcl.tools.config.provided.RuntimeConfigDefault;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Configuration class for the FoundationDB Implementation of the BlockStore interface
 */
@Getter
public class BlockStoreFDBConfig implements BlockStoreKeyValueConfig {

    /**
     * Number of Items to process on each Transaction. An "item" might be a Block, a Tx, etc.
     * FoundationDB has a limitation on the number of Bytes affected within a Transaction and also on the time it takes
     * for each Transaction tom complete, that means that when running operations on a list of items (Saving Blocks or
     * Tx, removing, etc), we need to make sure that the number of items is not too big. So we use these property to
     * break down the list into smaller sublist, and on Tx is created for each sublist, to handle the items in that
     * sublist.
     *
     * This technique only takes into consideration the number of Items, not their size, so its not very efficient. A
     * more accurate implementation will take into consideration the SIZE of each ITem and will break down the list
     * depending on those sizes. That is doable when insert elements (since the items themselves are in the list so we
     * can inspect them and check out their size), but its more problematic when removing. So this technique is a
     * middle-ground solution.
     */
    public static final int TRANSACTION_BATCH_SIZE = 5000;

    /** Java API Version. This might change if the maven dependency is updated, so be careful */
    private static final int API_VERSION = 510;

    /** Runtime Config */
    private final RuntimeConfig runtimeConfig;

    /** FoundationDb cluster file. If not specified, the default location is used */
    private String clusterFile;

    /** JAva Api version to use */
    private int apiVersion;

    /**
     * The network Id to use as a Base Directory. We use a String here to keep dependencies simple,
     * but in real scenarios this value will be obtained from a ProtocolConfiguration form the JCL-Net
     * module
     */
    private String networkId;

    /**
     * Maximun Number of Items to process in a Transaction
     */
    private int transactionBatchSize;

    @Builder
    public BlockStoreFDBConfig(RuntimeConfig runtimeConfig,
                               String clusterFile,
                               Integer apiVersion,
                               @NonNull String networkId,
                               Integer transactionBatchSize) {
        this.runtimeConfig = (runtimeConfig != null) ? runtimeConfig: new RuntimeConfigDefault();
        this.clusterFile = clusterFile;
        this.apiVersion = (apiVersion != null) ? apiVersion : API_VERSION;
        this.networkId = networkId;
        this.transactionBatchSize = (transactionBatchSize != null) ? transactionBatchSize : TRANSACTION_BATCH_SIZE;
    }
}
