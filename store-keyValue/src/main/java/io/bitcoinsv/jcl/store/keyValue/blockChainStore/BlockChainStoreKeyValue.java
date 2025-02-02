package io.bitcoinsv.jcl.store.keyValue.blockChainStore;


import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.HashProvider;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.base.HeaderReadOnly;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.extended.ChainInfo;
import io.bitcoinsv.bitcoinjsv.bitcoin.api.extended.ChainInfoReadOnly;
import io.bitcoinsv.bitcoinjsv.bitcoin.bean.extended.ChainInfoBean;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.jcl.store.blockChainStore.BlockChainStore;
import io.bitcoinsv.jcl.store.blockChainStore.BlockChainStoreState;
import io.bitcoinsv.jcl.store.blockChainStore.events.ChainForkEvent;
import io.bitcoinsv.jcl.store.blockChainStore.events.ChainPruneEvent;
import io.bitcoinsv.jcl.store.blockChainStore.events.ChainPruneAlertEvent;
import io.bitcoinsv.jcl.store.blockChainStore.events.ChainStateEvent;
import io.bitcoinsv.jcl.store.blockChainStore.validation.exception.BlockChainRuleFailureException;
import io.bitcoinsv.jcl.store.keyValue.blockStore.BlockStoreKeyValue;
import io.bitcoinsv.jcl.store.keyValue.common.HashesList;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Thsi interface extends teh "BlockStore" and adds capabilities to store and traverse the Chain of Blocks, and
 * also detected Forks (and prune them).
 *
 * In additiona to all the infor already sotred by the "BlockStore" interface, this one adds some more.
 * A Block has now other Entries, all under the "BLOCKS" directory:
 *
 *  "b:[blockHash]:next:    Stores a LIST of those Blcoks built on top of this one. Its usually a one-element list, but
 *                          in case of a FORK there might be more than one.
 *
 *  "b:chain:[blockHash]":  an instance of b.lockChainInfo. If this Block can be CONNECTED to the Chain (meaning that
 *                          its parent is also stored and connected to the Chain), then this isntance stores the
 *                          relative chain info for this Block.*
 *
 *  Also, there is a new Key (in the "BLOCKCHAIN" directory) that stores a List with the TIPS of the Chain
 *  (Block Hashes). Its usually a one-element List, but in case of a FORK it might contain more than one element.
 *
 * "chain_tips": a single property that stores the list of the current TIPS of all the chains stored
 *
 * This class also intrudices the concept of PATH:
 * a PAth is a series of blocks that make up a LINE of Blocks, from the parent (the FIRST), to the last. A Path is
 * always a Stright ine: no Forks are allowed.
 * Example:
 *      - Considering this series of Blocks: [A] - [B] - [C]
 *      - All the blocks in this example have the same Path. Every PAth has a PathId, in this case lets say is TWO (2).
 *        So we can print the same example adding the Path id to each block:
 *        [A:2] - [B:2] - [C:2]
 *      - If we insert a Block [X], which is also a "children" of [B], ten we are creating a FORK. At this momento, the
 *        PAth is split into 2, resultng into this:
 *        [A:2] - [B:2] - [C:3]
 *                     |- [X:4]
 *        So now we have 3 Paths, each one of them is a straight line of blocks.
 *
 *        For each Path that is created this way, we also store the relationship between this PAth and its "parentPath",
 *        in this case:
 *          - Path #2 has nor parent in this example
 *          - Path #3 has a Parent Path #2
 *          - Path #4 has a Parent PAth #2
 *
 *  "chain_path:[pathId]":  This propery stored the info for one specific Path.
 *  "chain_paths:next:"     This sores the Id of the Last Path used Since the PAths are created in Real-time whenever a Fork
 *                          is detected, this property is used to pick up the next Path Id.
 *
 **
 * @param <E>   Type of each ENTRY in the DB. Each Key-Value DB implementation usually provides Iterators that returns
 *              Entries from the DB (KeyValue in FoundationDB, a Map.Entry in LevelDb, etc).
 * @param <T>   Type of the TRANSACTION used by the DB-specific implementation. If Transactions are NOT supported, the
 *              "Object" Type can be used.
 */
public interface BlockChainStoreKeyValue<E, T> extends BlockStoreKeyValue<E, T>, BlockChainStore {

    /** Configuration: */
    BlockChainStoreKeyValueConfig getConfig();

    // Keys used to store block info and it's relative position within the Chain:
    String KEY_SUFFIX_BLOCK_NEXT     = "next";         // Block built on top of this one
    String KEY_PREFFIX_BLOCK_CHAIN   = "b_chain";      // Chain info for this block based on Hash
    String KEY_PREFFIX_BLOCK_HEIGHT  = "b_height";     // Chain info for this block based on Height
    String KEY_CHAIN_TIPS            = "chain_tips";   // List of all the Tip Chains

    String KEY_PREFFIX_PATHS         = "chain_paths";
    String KEY_SUFFIX_PATHS_LAST     = "last";
    String KEY_PREFFIX_PATH          = "chain_path";
    /* Functions to generate Simple Keys in String format: */

    default String keyForBlockNext(String blockHash)        { return KEY_PREFFIX_BLOCK_PROP + blockHash + KEY_SEPARATOR + KEY_SUFFIX_BLOCK_NEXT + KEY_SEPARATOR; }
    default String keyForBlockChainInfo(String blockHash)   { return KEY_PREFFIX_BLOCK_CHAIN + KEY_SEPARATOR + blockHash + KEY_SEPARATOR; }
    default String keyForBlocksByHeight(int height)         { return KEY_PREFFIX_BLOCK_HEIGHT + KEY_SEPARATOR + height + KEY_SEPARATOR;}
    default String keyForChainTips()                        { return KEY_CHAIN_TIPS + KEY_SEPARATOR; }
    default String keyForChainPathsLast()                   { return KEY_PREFFIX_PATHS + KEY_SEPARATOR + KEY_SUFFIX_PATHS_LAST + KEY_SEPARATOR;}
    default String keyForChainPath(int branchId)            { return KEY_PREFFIX_PATH + KEY_SEPARATOR + branchId + KEY_SEPARATOR;}


    /* Functions to generate WHOLE Keys, from the root up to the item. to be implemented by specific DB provider */

    byte[] fullKeyForBlockNext(String blockHash);
    byte[] fullKeyForBlockChainInfo(String blockHash);
    byte[] fullKeyForBlockHashesByHeight(int height);
    byte[] fullKeyForChainTips();
    byte[] fullKeyForChainPathsLast();
    byte[] fullKeyForChainPath(int branchId);


    /* Functions to serialize Objects:  */

    default byte[] bytes(BlockChainInfo blockChainInfo)     { return BlockChainInfoSerializer.getInstance().serialize(blockChainInfo); }
    default byte[] bytes(ChainPathInfo chainBranchInfo)     { return ChainPathInfoSerializer.getInstance().serialize(chainBranchInfo);}

    /* Functions to deserialize Objects: */

    default BlockChainInfo  toBlockChainInfo(byte[] bytes)  { return (isBytesOk(bytes)) ? BlockChainInfoSerializer.getInstance().deserialize(bytes) : null;}
    default ChainPathInfo   toChainPathInfo(byte[] bytes)   { return (isBytesOk(bytes)) ? ChainPathInfoSerializer.getInstance().deserialize(bytes) : null;}

    /* function definitions */

    default void validateBlockChainInfo(ChainInfo block) throws BlockChainRuleFailureException {};

    /*
     BlockChain Store DB Operations:
     These methods execute the business logic. Most of the time, each one of the methods below map a method of the
     BlockStore interface, but with some peculiarities:
     - They do NOT trigger Events
     - They do NOT crete new DB Transaction, instead they need to reuse one passed as a parameter.

     The Events and Transactions are created at a higher-level (byt he public methods that implemen the BlockStore
     interface).
     */

    private BlockChainInfo _getBlockChainInfo(T tr, String blockHash) {
        byte[] value = read(tr, fullKeyForBlockChainInfo(blockHash));
        return toBlockChainInfo(value);
    }

    private void _saveBlockChainInfo(T tr, BlockChainInfo blockChainInfo) {
        byte[] key = fullKeyForBlockChainInfo(blockChainInfo.getBlockHash());
        byte[] value = bytes(blockChainInfo);
        save(tr, key, value);
        getLogger().trace("BlockChainInfo Saved/Updated [block: {}, path: {}, height: {}]", blockChainInfo.getBlockHash(), blockChainInfo.getChainPathId(), blockChainInfo.getHeight());
    }

    private void _removeBlockChainInfo(T tr, String blockHash) {
        remove(tr, fullKeyForBlockChainInfo(blockHash));
    }

    private BlockChainInfo _getBlockChainInfo(HeaderReadOnly block, BlockChainInfo parentBlockChainInfo, int chainPathId){
        // We calculate the Height of the Chain:
        int resultHeight = (parentBlockChainInfo != null)
                ? parentBlockChainInfo.getHeight() + 1
                : 0;
        // We calculate the Size Inn Bytes of the Chain:
        // TODO: Possible overflow here????
        long resultChainSize = (parentBlockChainInfo != null)
                ? block.getMessageSize() + parentBlockChainInfo.getTotalChainSize()
                : block.getMessageSize();

        // We set the value of the ChainWork:
        BigInteger chainWork = (parentBlockChainInfo != null)
                ? parentBlockChainInfo.getChainWork().add(block.getWork())
                : getConfig().getGenesisBlock().getWork();

        // We build the object and save it:
        BlockChainInfo blockChainInfo = BlockChainInfo.builder()
                .blockHash(block.getHash().toString())
                .chainWork(chainWork)
                .height(resultHeight)
                .totalChainSize(resultChainSize)
                .chainPathId(chainPathId)
                .build();

        return blockChainInfo;

    }


    private HashesList _getBlockHashesByHeight(T tr, int height) {
        byte[] value = read(tr, fullKeyForBlockHashesByHeight(height));
        return toHashes(value);
    }

    private void _saveBlockHashByHeight(T tr, String blockHash, int height) {
        byte[] key = fullKeyForBlockHashesByHeight(height);
        HashesList currentHashes = toHashes(read(tr, key));
        if (currentHashes == null) {
            currentHashes = new HashesList.HashesListBuilder().hash(blockHash).build();
        }
        currentHashes.addHash(blockHash);
        byte[] value = bytes(currentHashes);
        save(tr, key, value);
    }

    private void _removeBlockHashesByHeight(T tr, int height) {
        byte[] key = fullKeyForBlockHashesByHeight(height);
        remove(tr, key);
    }

    private void _removeBlockHashFromHeight(T tr, String blockHash, int height) {
        byte[] key = fullKeyForBlockHashesByHeight(height);
        HashesList hashesList = toHashes(read(tr, key));
        if (hashesList != null) {
            hashesList.removeHash(blockHash);
            if (hashesList.getHashes().isEmpty()) {
                remove(tr, key);
            } else {
                save(tr, key, bytes(hashesList));
            }
        }
    }

    private List<String> _getNextBlocks(T tr, String blockHash) {
        List<String> result = new ArrayList<>();
        HashesList nextBlocks = toHashes(read(tr, fullKeyForBlockNext(blockHash)));
        if (nextBlocks != null) result.addAll(nextBlocks.getHashes());
        return result;
    }

    private List<BlockChainInfo> _getNextConnectedBlocks(T tr, String blockHash) {
        List<BlockChainInfo> result = new ArrayList<>();
        List<String> nextBlocks = _getNextBlocks(tr, blockHash);
        for (String childBlockHash : nextBlocks) {
            BlockChainInfo childChainInfo = _getBlockChainInfo(tr, childBlockHash);
            if (childChainInfo != null) result.add(childChainInfo);
        }
        return result;
    }

    private void _addChildToBlock(T tr, String parentBlockHash, String childBlockHash) {
        List<String> childs = _getNextBlocks(tr, parentBlockHash);
        if (!(childs.contains(childBlockHash)))
            childs.add(childBlockHash);
        HashesList childListToStore = HashesList.builder().hashes(childs).build();
        save(tr, fullKeyForBlockNext(parentBlockHash), bytes(childListToStore));
        getLogger().trace("Block {} saved as a CHILD of {}", childBlockHash, parentBlockHash);
    }

    private void _removeChildFromBlock(T tr, String parentBlockHash, String childBlockHash) {
        List<String> childs = _getNextBlocks(tr, parentBlockHash);
        childs.remove(childBlockHash);
        if (childs.size() > 0) {
            HashesList childsToStore = HashesList.builder().hashes(childs).build();
            save(tr, fullKeyForBlockNext(parentBlockHash), bytes(childsToStore));
        }  else    remove(tr, fullKeyForBlockNext(parentBlockHash));
        getLogger().trace("Block {} removed as CHILD from parent {}", childBlockHash, parentBlockHash);
    }

    default List<String> _getChainTips(T tr) {
        List<String> result = new ArrayList<>();
        HashesList tips = toHashes(read(tr, fullKeyForChainTips()));
        if (tips != null) result.addAll(tips.getHashes());
        return result;
    }

    private void _saveChainTips(T tr, HashesList chainstips) {
        save(tr, fullKeyForChainTips(), bytes(chainstips));
    }

    private void _updateTipsChain(T tr, String blockHashToAdd, String blockHashToRemove) {
        List<String> tipsChain = _getChainTips(tr);
        if ((blockHashToAdd != null)  && (!tipsChain.contains(blockHashToAdd))) {
            tipsChain.add(blockHashToAdd);
            getLogger().trace("Block {} added to the Tips", blockHashToAdd);
        }
        if (blockHashToRemove != null && tipsChain.contains(blockHashToRemove))  {
            tipsChain.remove(blockHashToRemove);
            getLogger().trace("Block {} removed from the Tips", blockHashToRemove);
        }
        HashesList tipsToSave = HashesList.builder().hashes(tipsChain).build();
        _saveChainTips(tr, tipsToSave);
    }

    default void connectBlock(Sha256Hash blockHash, Consumer<Sha256Hash> onBlockConnected) throws BlockChainRuleFailureException {
        try {
            getLock().writeLock().lock();
            T tr = createTransaction();

            var header = _getBlock(tr, blockHash.toString());

            _connectBlock(tr, header, null, onBlockConnected);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    default List<Sha256Hash> connectBlock(Sha256Hash blockHash) throws BlockChainRuleFailureException {
        try {
            getLock().writeLock().lock();
            T tr = createTransaction();

            var header = _getBlock(tr, blockHash.toString());

            return _connectBlock(tr, header, null, null).stream()
                    .map(HashProvider::getHash)
                    .collect(Collectors.toList());
        } finally {
            getLock().writeLock().unlock();
        }
    }

    private List<HeaderReadOnly> _connectBlock(T tr, HeaderReadOnly blockHeader, BlockChainInfo parentBlockChainInfo, Consumer<Sha256Hash> onBlockConnected) throws BlockChainRuleFailureException {
        List<HeaderReadOnly> blocksConnected = new ArrayList<>();

        Deque<HeaderReadOnly> blocksToConnect = new ArrayDeque<>();
        blocksToConnect.push(blockHeader);

        boolean firstBlockToConnect = true;

        while (!blocksToConnect.isEmpty()) {

            var blockToConnect = blocksToConnect.pop();
            var blockToConnectHash = blockToConnect.getHash();
            var blockToConnectHashString = blockToConnectHash.toString();

            getLogger().trace("Connecting Block {} ...", blockToConnect.getHash());
            // Block chain Info that will be inserted for this Block:

            if (!firstBlockToConnect) {
                parentBlockChainInfo = _getBlockChainInfo(tr, blockToConnect.getPrevBlockHash().toString());
            }

            BlockChainInfo blockChainInfo = _getBlockChainInfo(tr, blockToConnect, parentBlockChainInfo);

            //save the block
            _saveBlockChainInfo(tr, blockChainInfo);

            // We store a Key to link the HEIGHT with its Hash, so we can retrieve it later by its Height in O(1) time...
            _saveBlockHashByHeight(tr, blockChainInfo.getBlockHash(), blockChainInfo.getHeight());

            try {
                //we validate block afterwards as the validation will look up state from the blockstore.
                _validateBlock(blockToConnect, blockChainInfo);
            } catch (BlockChainRuleFailureException ex) {
                //remove all traces from the block if it's invalid
                _removeBlock(tr, blockToConnect.getHash().toString());
                // nothing else to process
                throw ex;
            }

            // Now we update the tips of the Chain:
            List<String> tipsChain = _getChainTips(tr);

            // If the Parent is part of the TIPS of the Chains, then it must be removed from it:
            if (parentBlockChainInfo != null && tipsChain.contains(parentBlockChainInfo.getBlockHash())) {
                _updateTipsChain(tr, null, parentBlockChainInfo.getBlockHash()); // we don't add, just remove
            }

            //We want to return this block as it's been connected
            blocksConnected.add(blockToConnect);

            //Add this block to the chain tips
            _updateTipsChain(tr, blockToConnectHashString, blockToConnect.getPrevBlockHash().toString()); // We add this block to the Tips

            // Now we look into the CHILDREN (Blocks built on top of this Block), and we connect them as well...
            // If the Block has NOT Children, then this is the Last Block that can be connected, so we add it to the Tips
            _getNextBlocks(tr, blockToConnectHashString).forEach(childHashHex ->
                    ofNullable(_getBlock(tr, childHashHex)).ifPresent(header -> {
                        blocksToConnect.push(header);
                        BlockStoreKeyValue.super._removeOrphanBlockHash(tr, childHashHex);
                    })
            );

            ofNullable(onBlockConnected).ifPresent(c -> c.accept(blockToConnectHash));

            firstBlockToConnect = false;
        }

        return blocksConnected;
    }

    private BlockChainInfo _getBlockChainInfo(T tr, HeaderReadOnly blockHeader, BlockChainInfo parentBlockChainInfo) {
        // Special case for the Genesis Block:
        if (parentBlockChainInfo == null) {
            return _getBlockChainInfo(blockHeader, parentBlockChainInfo, 1);
        }

        // Regular scenario, when connecting a Block to an existing Parent:
        //  - If the parent has NO children we just connect the Block and REUSE the parent's PathId
        //  - If the Parent has ONE Child, then this is the First FORk starting from that Parent, so we create
        //    2 new Paths: 1 is assigned to the old child, and the new one to the block we are connecting now.
        //  - If the parent has already MOR than 1 child, that means that there is already a FORK starting from this
        //    parent, so its children are already using different Paths. In this case we only need to create one additional
        //    path and assign it to the Block we are connecting.

        int pathIdForNewBlock = parentBlockChainInfo.getChainPathId();
        List<BlockChainInfo> parentConnectedChildren = _getNextConnectedBlocks(tr, parentBlockChainInfo.getBlockHash());

        if (!parentConnectedChildren.isEmpty()) {
            // if there is only ONE Child, we update its PathId with a new one...
            // If there are MORE than one children, then they must have already different Paths Id, so nothing to do...

            if (parentConnectedChildren.size() == 1) {
                int newChainPathToPropagate = _createNewChainPath(tr, parentBlockChainInfo.getChainPathId(), parentConnectedChildren.get(0).getBlockHash()).getId();
                _propagateChainPathUnderBlock(tr, parentConnectedChildren.get(0), parentConnectedChildren.get(0).getChainPathId(), newChainPathToPropagate);
            }
            // We create a new Path Id for the block we are connecting...
            pathIdForNewBlock = _createNewChainPath(tr, parentBlockChainInfo.getChainPathId(), blockHeader.getHash().toString()).getId();
        }
        // connect this block
        return _getBlockChainInfo(blockHeader, parentBlockChainInfo, pathIdForNewBlock);
    }

    default void disconnectBlock(Sha256Hash blockHash) {
        disconnectBlock(blockHash, null);
    }

    default void disconnectBlock(Sha256Hash blockHash, Consumer<Sha256Hash> onDisconnected) {
        try {
            getLock().writeLock().lock();
            T tr = createTransaction();

            _disconnectBlock(tr, blockHash.toString(), onDisconnected);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    private void _disconnectBlock(T tr, String blockHash) {
        _disconnectBlock(tr, blockHash, null);
    }

    private void _disconnectBlock(T tr, String blockHash, Consumer<Sha256Hash> onDisconnected) {
        // If this block is already connected we remove the Chain Info:
        BlockChainInfo blockChainInfo = _getBlockChainInfo(tr, blockHash);

        if (blockChainInfo == null) {
            return;
        }

        getLogger().trace("Disconnecting Block {} (height: {}) (path: {})...", blockChainInfo.getBlockHash(), blockChainInfo.getHeight(), blockChainInfo.getChainPathId());

        Deque<String> blockStack = new ArrayDeque<>();

        // We push first block that we are disconnecting
        blockStack.push(blockHash);

        // First fetch of next blocks
        final List<String> nextBlocks = new ArrayList<>(_getNextBlocks(tr, blockHash));

        while (!nextBlocks.isEmpty()) {

            // We add all next blocks to stack for disconnect
            nextBlocks.forEach(blockStack::push);

            // We put aside all the block we need to check if children exist
            List<String> checkBlocks = new ArrayList<>(nextBlocks);
            nextBlocks.clear();

            // We fetch all next blocks that are related to the blocks we are checking for
            checkBlocks.forEach(block -> nextBlocks.addAll(_getNextBlocks(tr, block)));
        }

        while (!blockStack.isEmpty()) {
            // We take out block hash that we are going to disconnect
            var disBlockHash = blockStack.pop();

            // We remove the ChainInfo for this Node (this will disconnect this Block from the Chain):
            _removeBlockChainInfo(tr, disBlockHash);

            // We remove the link of this Height with this block Hash...
            _removeBlockHashFromHeight(tr, disBlockHash, blockChainInfo.getHeight());

            // We update the tip of the chain (this block is not the tip anymore, if its already)
            var block = _getBlock(tr, disBlockHash);
            // We get previous block

            String blockHashToAdd = null;

            // We check if previous block has children, if not it is new tip
            // If this is the GENESIS Block, we do nothing
            if (blockStack.isEmpty() && !block.getPrevBlockHash().equals(Sha256Hash.ZERO_HASH)) {
                var prevBlock = _getBlock(tr, block.getPrevBlockHash().toString());

                if(prevBlock != null) {
                    var siblings = _getNextBlocks(tr, prevBlock.getHash().toString());
                    siblings.remove(disBlockHash);
                    if (siblings.isEmpty()) {
                        blockHashToAdd = prevBlock.getHash().toString();
                    }
                }
            }

            _updateTipsChain(tr, blockHashToAdd, disBlockHash);

            // notify of successful disconnection of the block
            ofNullable(onDisconnected).ifPresent(c -> c.accept(Sha256Hash.wrap(disBlockHash)));
        }
    }

    private List<String> _getBlocks(T tr, String tip, String blockHash) {
        List<String> blocks = new ArrayList<>();

        var targetHeight =  ofNullable(_getBlockChainInfo(tr, blockHash)).map(BlockChainInfo::getHeight).orElse(-1);

        if(targetHeight == -1) {
            return blocks;
        }

        var currentBlockHash = _getBlock(tr, tip).getHash().toString();
        var info = _getBlockChainInfo(tr, currentBlockHash);

        while (info.getHeight() >= targetHeight) {
            blocks.add(currentBlockHash);

            currentBlockHash = _getBlock(tr, currentBlockHash).getPrevBlockHash().toString();

            info = _getBlockChainInfo(tr, currentBlockHash);

            if(info == null) {
                break;
            }
        }

        if(!blocks.get(blocks.size() - 1).equals(blockHash)) {
            return new ArrayList<>();
        }

        return blocks;
    }

    default void _initGenesisBlock(T tr, HeaderReadOnly genesisBlock) {
        // We init the Info stored about the Paths in the Chain:
        _updateLastPathId(tr, 0);
        _createNewChainPath(tr, -1, genesisBlock.getHash().toString());

        // Now we insert (and connect) the Genesis Block:
        _saveBlock(tr, genesisBlock);
        try {
            _connectBlock(tr, genesisBlock, null, null); // No parent for this block
        } catch (BlockChainRuleFailureException e) {
            e.printStackTrace();
        }
    }

    default void _publishState() {
        try {
            getLock().readLock().lock();
            ChainStateEvent event = new ChainStateEvent(getState());
            getEventBus().publish(event);
        } catch (Exception e) {
            getLogger().error("ERROR at publishing State", e);
        } finally {
            getLock().readLock().unlock();
        }
    }


    @Override
    default List<HeaderReadOnly> _saveBlock(T tr, HeaderReadOnly blockHeader) {
        String parentHashHex = blockHeader.getPrevBlockHash().toString();

        //We want to return a list of blocks that are saved AND connected in the BlockChainStore
        List<HeaderReadOnly> blocksSaved = new ArrayList<>();

        // we save the Block...:
        BlockStoreKeyValue.super._saveBlock(tr, blockHeader);

        // and its relation with its parent (ONLY If this is NOT the GENESIS Block)
        if (!blockHeader.getHash().equals(getConfig().getGenesisBlock().getHash())) {
            _addChildToBlock(tr, parentHashHex, blockHeader.getHash().toString());
        }

        // We search for the ChainInfo of this block, to check if its already connected to the Chain:
        if (_getBlockChainInfo(tr, blockHeader.getHash().toString()) == null) {
            // If the Parent exists and it's also Connected, we connect this one too:
            BlockChainInfo parentChainInfo =  _getBlockChainInfo(tr, parentHashHex);
            if (parentChainInfo != null) {
                try {
                    //Add
                    blocksSaved.addAll(_connectBlock(tr, blockHeader, parentChainInfo, null));
                } catch (BlockChainRuleFailureException ignored) {
                    //The block we saved above cannot be connected yet. It will be returned later when the parent is connected
                    blocksSaved.remove(blockHeader);
                }

                // If this is a fork, we trigger a Fork Event:
                List<String> parentChilds = _getNextBlocks(tr, blockHeader.getPrevBlockHash().toString());
                if (parentChilds.size() > 1) {
                    ChainForkEvent event = new ChainForkEvent(blockHeader.getPrevBlockHash(), blockHeader.getHash());
                    getEventBus().publish(event);
                }
            } else {
                //Only the genesis block should have ZERO hash, genesis is not an orphan
                if(!blockHeader.getHash().equals(getConfig().getGenesisBlock().getHash())) {
                    _saveOrphanBlockHash(tr, blockHeader.getHash().toString());
                }
            }
        } else {
            blocksSaved.add(blockHeader);
        }

        return blocksSaved;
    }

    @Override
    default void _removeBlock(T tr, String blockHash) {
        // Basic check if the block exists:
        HeaderReadOnly block = _getBlock(tr, blockHash);
        if (block == null) return;

        // We remove the relationship between this block and its parent:
        _removeChildFromBlock(tr, block.getPrevBlockHash().toString(), blockHash);
        _disconnectBlock(tr, blockHash);

        // we remove the Block the usual way:
        BlockStoreKeyValue.super._removeBlock(tr, blockHash);
    }

    private void _updateLastPathId(T tr, int pathId) {
        byte[] key = fullKeyForChainPathsLast();
        save(tr, key, bytes(pathId));
    }

    private int _getLastPathId(T tr) {
        byte[] key = fullKeyForChainPathsLast();
        byte[] value = read(tr, key);
        return (value != null) ? toInt(read(tr, key)) : 0;
    }

    private ChainPathInfo _createNewChainPath(T tr, int parentPathId, String blockHash) {
        // We get the latest Path ID (if there is any) and we update it:
        int lastPathId = _getLastPathId(tr);
        _updateLastPathId(tr, ++lastPathId);

        // No we create a new record to store info about this Path:
        ChainPathInfo result = _saveChainPath(tr, lastPathId, parentPathId, blockHash);
        return result;
    }

    private ChainPathInfo _saveChainPath(T tr, int pathId, int parentId, String blockHash) {
        ChainPathInfo result = ChainPathInfo.builder()
                .id(pathId)
                .parent_id(parentId)
                .blockHash(blockHash)
                .build();
        byte[] key = fullKeyForChainPath(pathId);
        byte[] value = bytes(result);
        save(tr, key, value);
        getLogger().trace("PathInfo Saved [path id: {}, parent path: {}]", pathId, parentId);
        return result;
    }

    private void _removeChainPath(T tr, int pathId) {
        byte[] key = fullKeyForChainPath(pathId);
        remove(tr, key);
        getLogger().trace("PathInfo Removed [path id: {}]", pathId);
    }

    private ChainPathInfo _getChainPathInfo(T tr, int pathId) {
        byte[] key = fullKeyForChainPath(pathId);
        byte[] value = read(tr, key);
        ChainPathInfo result = toChainPathInfo(value);
        return result;
    }


    private void _propagateChainPathUnderBlock(T tr, BlockChainInfo blockChainInfo, int pathIdToReplace, int newPathId) {

        // We Update the BlockInfoChain info for this block, reflecting the new Path Id its linked to, and also
        // does the same for all its children until it reaches a Block with more than 1 children....

        List<BlockChainInfo> blocksToProcess = new ArrayList<>(){{add(blockChainInfo);}};

        while(!blocksToProcess.isEmpty()) {
            List<BlockChainInfo> blocksToProcessNextIteration = new ArrayList<>();
            for (BlockChainInfo block : blocksToProcess) {
                ChainPathInfo pathInfo = _getChainPathInfo(tr, block.getChainPathId());

                // If this Block is assigned to a DIFFERENT PathID AND the PARENT of that Path is DIFFERENT from the
                // one we try to replace, then this Block is OK:
                boolean isThisBlockOK = (block.getChainPathId() != pathIdToReplace) && (pathInfo.getParent_id() != pathIdToReplace);

                if (!isThisBlockOK) {
                    if (block.getChainPathId() == pathIdToReplace) {
                        // We assign the new PathId to this Block:
                        getLogger().trace("Update Path for Block {} (height: {}) [ {} -> {}]", blockChainInfo.getBlockHash(), blockChainInfo.getHeight(), pathIdToReplace, newPathId);
                        block = block.toBuilder().chainPathId(newPathId).build();
                        _saveBlockChainInfo(tr, block);
                    }
                    if (pathInfo.getParent_id() == pathIdToReplace) {
                        // We Change the parent of this Path:
                        _saveChainPath(tr, pathInfo.getId(), newPathId, block.getBlockHash());
                    }
                    // We process its children:
                    List<BlockChainInfo> children = _getNextBlocks(tr, block.getBlockHash()).stream()
                            .map(b -> _getBlockChainInfo(tr, b))
                            .collect(Collectors.toList());
                    blocksToProcessNextIteration.addAll(children);
                } // if !isThisBlockOK...
            } // for...
            blocksToProcess = blocksToProcessNextIteration;
        } // while...

    }

    /*
     * High level Functions
     */

    @Override
    default BlockChainStoreState getState() {
        try {
            getLock().readLock().lock();
            List<ChainInfo> tipsChainInfo = getTipsChains().stream()
                    .map(h -> getBlockChainInfo(h).get())
                    .collect(Collectors.toList());
            return BlockChainStoreState.builder()
                    .tipsChains(tipsChainInfo)
                    .numBlocks(getNumBlocks())
                    .numTxs(getNumTxs())
                    .build();
        } finally {
            getLock().readLock().unlock();
        }

    }

    @Override
    default List<Sha256Hash> getTipsChains() {
        try {
            getLock().readLock().lock();
            List<Sha256Hash> result = new ArrayList<>();
            T tr = createTransaction();
            executeInTransaction(tr , () -> {
                List<String> tipsChain = _getChainTips(tr);
                result.addAll(tipsChain.stream().map(Sha256Hash::wrap).collect(Collectors.toList()));
            });
            return result;
        } finally {
            getLock().readLock().unlock();
        }
    }

    @Override
    default List<Sha256Hash> getTipsChains(Sha256Hash blockHash) {
        try {
            getLock().readLock().lock();
            List<Sha256Hash> result = new ArrayList<>();
            T tr = createTransaction();
            executeInTransaction(tr, () -> {
                // We only continue if the Block given is connected:
                BlockChainInfo blockChainInfo = _getBlockChainInfo(tr, blockHash.toString());
                if (blockChainInfo == null) return;

                // We loop over the current Tips of the Chains. For each one, we check if the Path they belong to, and
                // the Parent Paths of them. If the PAth of the block specified is one of those PAths, then we include
                // that tip in the result
                List<String> tipsChain = _getChainTips(tr);
                for (String tipHash : tipsChain) {
                    // we get the PathId of this Tip:
                    int chainPathId = _getBlockChainInfo(tr, tipHash).getChainPathId();
                    boolean blockHashIsPartOfPath = false;
                    do {
                        // If the Path of this Tip is the msae as the Path of our Block, then we are done.
                        blockHashIsPartOfPath |= chainPathId == blockChainInfo.getChainPathId();
                        // If not, then we keep searching but this time on the Parent of this Path, if any:
                        ChainPathInfo pathInfo = _getChainPathInfo(tr, chainPathId);
                        chainPathId = pathInfo.getParent_id();

                    } while (chainPathId != -1 && !blockHashIsPartOfPath);
                    if (blockHashIsPartOfPath) result.add(Sha256Hash.wrap(tipHash));
                }
            });
            return result;
        } finally {
            getLock().readLock().unlock();
        }
    }

    /**
     * Efficiently( O(total_paths) ) Checks whether either hash is within the same chain
     * @Return True if both hashes share a common path, false if not
     */
    default boolean isInChain(Sha256Hash blockHash1, Sha256Hash blockHash2) {
        try {
            getLock().readLock().lock();
            AtomicReference<Boolean> result = new AtomicReference<>(false);

            T tr = createTransaction();
            executeInTransaction(tr, () -> {
                BlockChainInfo block1 = _getBlockChainInfo(tr, blockHash1.toString());
                BlockChainInfo block2 = _getBlockChainInfo(tr, blockHash2.toString());

                if(block1 == null || block2 == null){
                    return;
                }

                //get the chain path from the higest block
                ChainPathInfo highestChainPathInfo;
                ChainPathInfo targetChainPathInfo;
                if(block1.getHeight() > block2.getHeight()) {
                    highestChainPathInfo =  _getChainPathInfo(tr, block1.getChainPathId());
                    targetChainPathInfo = _getChainPathInfo(tr, block2.getChainPathId());
                } else {
                    highestChainPathInfo =  _getChainPathInfo(tr, block2.getChainPathId());
                    targetChainPathInfo = _getChainPathInfo(tr, block1.getChainPathId());
                }

                //traverse the chain path back to genesis to see if the target path is on the same path
                while(highestChainPathInfo != null){
                    if(highestChainPathInfo.getId() == targetChainPathInfo.getId()){
                        result.set(true);
                        return;
                    }

                    highestChainPathInfo = _getChainPathInfo(tr, highestChainPathInfo.getParent_id());
                }
            });

            return result.get();

        } finally {
            getLock().readLock().unlock();
        }
    }

    @Override
    default Optional<ChainInfo> getAncestorByHeight(Sha256Hash blockHash, int ancestorHeight) {

        try {
            getLock().readLock().lock();
            AtomicReference<ChainInfo> result = new AtomicReference<>();

            T tr = createTransaction();
            executeInTransaction(tr, () -> {
                BlockChainInfo currentBlock = _getBlockChainInfo(tr, blockHash.toString());
                List<BlockChainInfo> ancestorBlocks = _getBlockHashesByHeight(tr, ancestorHeight).getHashes().stream().map(h -> _getBlockChainInfo(tr, h)).collect(Collectors.toList());

                if(currentBlock == null || ancestorBlocks.isEmpty()){
                    return;
                }

                if(currentBlock.getHeight() < ancestorHeight) {
                    return;
                }

                //starting path
                ChainPathInfo chainPathInfo = _getChainPathInfo(tr, currentBlock.getChainPathId());

                //recursively loop up the given blocks chain paths, if one of the ancestor blocks is on the same path, then we know it's a direct ancestor
                BlockChainInfo ancestorBlock = null;
                while(chainPathInfo != null){
                    for(BlockChainInfo blockChainInfo : ancestorBlocks){
                        if(blockChainInfo.getChainPathId() == chainPathInfo.getId()) {
                            ancestorBlock = blockChainInfo;
                            break;
                        }
                    }
                    chainPathInfo = _getChainPathInfo(tr, chainPathInfo.getParent_id());
                }

                HeaderReadOnly ancestorBlockHeader = _getBlock(tr, ancestorBlock.getBlockHash());

                ChainInfoBean chainInfoResult = new ChainInfoBean(ancestorBlockHeader);
                chainInfoResult.setChainWork(ancestorBlock.getChainWork());
                chainInfoResult.setHeight(ancestorBlock.getHeight());
                chainInfoResult.makeImmutable();

                result.set(chainInfoResult);
            });

            return ofNullable(result.get());

        } finally {
            getLock().readLock().unlock();
        }
    }

    @Override
    default Optional<ChainInfo> getLowestCommonAncestor(List<Sha256Hash> blockHashes) {

        try {
            getLock().readLock().lock();
            AtomicReference<ChainInfo> result = new AtomicReference<>();

            T tr = createTransaction();
            executeInTransaction(tr, () -> {
                Set<BlockChainInfo> leafNodeSet = new TreeSet<>(Comparator.comparing(BlockChainInfo::getHeight).reversed());

                // we loop through each of the chain path histories, taking the common root ancestors, then the ancestor with the largest height will be the highest common ancestor
                for(Sha256Hash hash : blockHashes){
                    BlockChainInfo chainInfo = _getBlockChainInfo(tr, hash.toString());

                    if(chainInfo == null)
                        return;

                    //we will store all ancestors up to genesis
                    Set<BlockChainInfo> ancestorRootNodes = new TreeSet<>(Comparator.comparing(BlockChainInfo::getHeight));

                    //starting path
                    ChainPathInfo chainPathInfo = _getChainPathInfo(tr, chainInfo.getChainPathId());

                    //recursively loop up the chain paths, and get the first nodes parent in that branch. The parent is the common ancestor. Then take the intersection of all three lists to
                    //find the common ancestor(s). Then we take the ancestor with the highest height from the new intersected list.
                    while(chainPathInfo != null){
                        HeaderReadOnly firstNodeBlock = _getBlock(tr, chainPathInfo.getBlockHash());
                        BlockChainInfo rootNodeBlockChainInfo;

                        //secial handling for genesis branch, as genesis blocks parent is itself
                        if(firstNodeBlock.getHash().equals(getConfig().getGenesisBlock().getHash())){
                            rootNodeBlockChainInfo = _getBlockChainInfo(tr, firstNodeBlock.getHash().toString());
                        } else {
                            rootNodeBlockChainInfo = _getBlockChainInfo(tr, firstNodeBlock.getPrevBlockHash().toString());
                        }

                        //Add to list of common ancestors
                        ancestorRootNodes.add(rootNodeBlockChainInfo);

                        //recursively loop up the paths
                        chainPathInfo = _getChainPathInfo(tr, chainPathInfo.getParent_id());
                    }

                    //we want to keep the intersection between all the sets, if this is the first loop then the intersection will be an empty set, so just copy it over.
                    if(leafNodeSet.isEmpty()){
                        leafNodeSet.addAll(ancestorRootNodes);
                    } else {
                        //if not, then the intersection is the common ancestors between both lists
                        leafNodeSet.retainAll(ancestorRootNodes);
                    }
                }

                //As this list is sorted by height decending, the first in the list will be the common ancestor with the highest height
                BlockChainInfo lowestCommonAncestorChainInfo = leafNodeSet.iterator().next();
                HeaderReadOnly lowestCommonAncestorHeader = _getBlock(tr, lowestCommonAncestorChainInfo.getBlockHash());

                ChainInfoBean chainInfoResult = new ChainInfoBean(lowestCommonAncestorHeader);
                chainInfoResult.setChainWork(lowestCommonAncestorChainInfo.getChainWork());
                chainInfoResult.setHeight(lowestCommonAncestorChainInfo.getHeight());
                chainInfoResult.makeImmutable();

                //the first element in the set, is now the higest common ancestor
                result.set(chainInfoResult);
            });

            return ofNullable(result.get());

        } finally {
            getLock().readLock().unlock();
        }

    }

    @Override
    default Optional<ChainInfo> getFirstBlockInHistory(Sha256Hash blockHash) {
        try {
            getLock().readLock().lock();
            AtomicReference<ChainInfo> result = new AtomicReference<>();
            T tr = createTransaction();
            executeInTransaction(tr, () -> {

                // If the Block is not connect we just break and return:
                BlockChainInfo blockChainInfo = _getBlockChainInfo(tr, blockHash.toString());
                if (blockChainInfo == null) return;

                // Now we get loop over the Paths that make the history of this block: The block it belongs to, the
                // parent of that block, and so on. For each PAth, we get the Block that Path begins with, and check
                // if there is a Fork at that point or not. Of there is a fork, then that Block is the FIRST Block in
                // the Path of this Block. If there is no fork (because it's been already pruned), then we continue
                // doing the same with the parent Path....

                int pathId = blockChainInfo.getChainPathId();
                ChainPathInfo pathInfo = _getChainPathInfo(tr, pathId);
                while (true) {
                    // We get the Block this Path begins with:
                    HeaderReadOnly blockBeginPath = _getBlock(tr, pathInfo.getBlockHash());
                    // if its the genesis, we are done:
                    if (blockBeginPath.getHash().equals(getConfig().getGenesisBlock().getHash())) break;

                    // Its any other Block. No we check if at this point there is still a fork. There is a Fork if
                    // the Block parent of the Beginning of the PAth still has MORE than one children:

                    BlockChainInfo parentPathBlockInfo  = _getBlockChainInfo(tr, blockBeginPath.getPrevBlockHash().toString());
                    List<String> parentPathBlockChildren = _getNextBlocks(tr, parentPathBlockInfo.getBlockHash());
                    boolean isAFork = (parentPathBlockChildren.size() > 1);

                    if (isAFork) break;
                    if (pathInfo.getParent_id() == -1) break;
                    pathInfo = _getChainPathInfo(tr, pathInfo.getParent_id());
                } // while...

                // At this point, the info we are looking for is in the pathInfo after the loop
                BlockChainInfo blockResultInfo = _getBlockChainInfo(tr, pathInfo.getBlockHash());
                HeaderReadOnly blockResultHeader = _getBlock(tr, pathInfo.getBlockHash());

                ChainInfoBean chainInfoResult = new ChainInfoBean(blockResultHeader);
                chainInfoResult.setChainWork(blockResultInfo.getChainWork());
                chainInfoResult.setHeight(blockResultInfo.getHeight());
                chainInfoResult.makeImmutable();
                result.set(chainInfoResult);
            });
            return ofNullable(result.get());

        } finally {
            getLock().readLock().unlock();
        }
    }

    @Override
    default void removeTipsChains() {
        try {
            getLock().writeLock().lock();
            T tr = createTransaction();
            executeInTransaction(tr, () -> {
                _saveChainTips(tr, HashesList.builder().build());
            });
        } finally {
            getLock().writeLock().unlock();
        }
    }

    @Override
    default List<ChainInfo> getBlock(int height) {
        AtomicReference<List<ChainInfo>> result = new AtomicReference<>(new ArrayList<>());
        T tr = createTransaction();
        executeInTransaction(tr, () -> {
            HashesList blockHashes = _getBlockHashesByHeight(tr, height);
            if (blockHashes == null) return;
            for (String blockHash : blockHashes.getHashes()) {
                HeaderReadOnly blockResultHeader = _getBlock(tr, blockHash);
                if (blockResultHeader != null) {
                    BlockChainInfo blockResultInfo = _getBlockChainInfo(tr, blockHash);
                    if (blockResultInfo != null) {
                        ChainInfoBean chainInfoResult = new ChainInfoBean(blockResultHeader);
                        chainInfoResult.setChainWork(blockResultInfo.getChainWork());
                        chainInfoResult.setHeight(blockResultInfo.getHeight());
                        chainInfoResult.makeImmutable();
                        result.get().add(chainInfoResult);
                    }
                }
            }
        });
        return result.get();
    }

    @Override
    default Optional<ChainInfo> getBlockChainInfo(Sha256Hash blockHash) {
        try {
            getLock().readLock().lock();
            AtomicReference<ChainInfo> result = new AtomicReference<>();
            T tr = createTransaction();
            executeInTransaction(tr, () -> {
                HeaderReadOnly block = _getBlock(tr, blockHash.toString());
                if (block == null) return;

                byte[] value = read(tr, fullKeyForBlockChainInfo(blockHash.toString()));
                BlockChainInfo blockChainInfo = toBlockChainInfo(value);
                if (blockChainInfo != null) {
                    ChainInfoBean chainInfoResult = new ChainInfoBean(block);
                    chainInfoResult.setChainWork(blockChainInfo.getChainWork());
                    chainInfoResult.setHeight(blockChainInfo.getHeight());
                    chainInfoResult.makeImmutable();
                    result.set(chainInfoResult);
                }
            });
            return ofNullable(result.get());
        } finally {
            getLock().readLock().unlock();
        }
    }

    @Override
    default boolean isConnected(Sha256Hash blockHash) {
        AtomicBoolean result = new AtomicBoolean();
        try {
            getLock().readLock().lock();
            byte[] key = fullKeyForBlockChainInfo(blockHash.toString());
            T tr = createTransaction();
            executeInTransaction(tr, () -> {
                        byte[] value = read(tr, key);
                        result.set(value != null);
                    }
            );
        } finally {
            getLock().readLock().unlock();
        }
        return result.get();
    }

    default boolean isOnLongestChain(Sha256Hash blockHash) {
        try {
            getLock().readLock().lock();

            // We get the longest chain
            var longestChain = getLongestChain();

            // Just to be safe
            if (longestChain.isEmpty()) {
                return false;
            }

            // We get block info if requested block
            var blockInfo = getBlockChainInfo(blockHash);

            // Just to be safe
            if (blockInfo.isEmpty()) {
                return false;
            }

            // We get block on the same height in the longest chain
            var ancestor = getAncestorByHeight(
                    longestChain.get().getHeader().getHash(),
                    blockInfo.get().getHeight()
            );

            // We check if block in the longest chain and the block we are asking for are the same block
            return ancestor
                    .map(ancestorInfo -> ancestorInfo.getHeader().getHash().equals(blockHash))
                    .orElse(false);
        } finally {
            getLock().readLock().unlock();
        }
    }

    @Override
    default Optional<ChainInfo> getLongestChain() {
        try {
            getLock().readLock().lock();

            return getState().getTipsChains().stream()
                    .max(Comparator.comparingInt(ChainInfoReadOnly::getHeight));
        } finally {
            getLock().readLock().unlock();
        }
    }

    @Override
    default Optional<Sha256Hash> getPrevBlock(Sha256Hash blockHash) {
        try {
            getLock().readLock().lock();
            return getBlock(blockHash).map(HeaderReadOnly::getPrevBlockHash);
        } finally {
            getLock().readLock().unlock();
        }
    }

    @Override
    default List<Sha256Hash> getNextBlocks(Sha256Hash blockHash) {
        try {
            getLock().readLock().lock();
            List<Sha256Hash> result = new ArrayList<>();
            T tr = createTransaction();
            executeInTransaction(tr, () -> {
                List<String> children = _getNextBlocks(tr, blockHash.toString());
                result.addAll(children.stream().map(Sha256Hash::wrap).collect(Collectors.toList()));
            });
            return result;
        } finally {
            getLock().readLock().unlock();
        }
    }

    @Override
    default Iterable<Sha256Hash> getOrphanBlocks() {

        // We configure the parameters for creating an Iterator that loops over the ORPHAN Blocks:

        // The iterator will loop over that Keys that belong to the "blocks" folder and start with the preffix
        // used for storing orphans:
        byte[] startingWithKey = fullKey(fullKeyForBlocks(), KEY_PREFFIX_ORPHAN_HASH);


        // The keyVerifier Function will check that each Key we loop over is a Valid Key: A Valid Key is a key that
        // references a Block that has NO parent block stored in the DB:

        BiPredicate<T, byte[]> keyVerifier = (tr, key) -> true;

        // The "buildItemBy" is the function used to take a Key and return each Item of the Iterator. The iterator
        // will returns a series of BlockHeader, so this function will build a BlockHeader out of a Key:

        Function<E, Sha256Hash> buildItemBy = (E item) -> {
            byte[] key = keyFromItem(item);
            String blockHash = extractBlockHashFromKey(key).get();
            return Sha256Hash.wrap(blockHash);
        };

        // With everything set up, we create our Iterator and return it wrapped up in an Iterable:
        Iterator<Sha256Hash> iterator = getIterator(startingWithKey, null, keyVerifier, buildItemBy);

        Iterable<Sha256Hash> result = () -> iterator;
        return result;
    }

    @Override
    default List<Sha256Hash> prune(ChainPruneAlertEvent pruneReq) {
        return prune(pruneReq.getTipForkHash(), true, getConfig().isForkPruningIncludeTxs());
    }

    @Override
    default List<Sha256Hash> prune(Sha256Hash tipChainHash, boolean removeBlocks, boolean removeTxs) {
        getLogger().debug("Prunning chain tip #{}...",tipChainHash);
        try {
            getLock().writeLock().lock();
            List<Sha256Hash> tipsChains = getTipsChains();

            // First we check if this Hash really is a TIP of a chain:
            if (!tipsChains.contains(tipChainHash))
                throw new RuntimeException("The Hash specified for Prunning is NOT the Tip of any Chain.");

            // Now we move from the tip backwards until we find a Block that has MORE than one child (one being the Block
            // we are removing)

            Sha256Hash hashBlockToRemove = tipChainHash;
            List<Sha256Hash> hashesBlocksToRemove = new ArrayList<>();
            Optional<Sha256Hash> parentHashOpt = getPrevBlock(hashBlockToRemove);
            long numBlocksRemoved = 0;
            while (true) {
                // we do NOT prune the GENESIS Block:
                if (hashBlockToRemove.equals(getConfig().getGenesisBlock().getHash())) break;

                // We only remove the Blocks FOR REAL depending on the parameter:
                if (removeBlocks) {
                    // We prune the block:
                    getLogger().debug("prunning block {}...", hashBlockToRemove);

                    // We use the internal method "_removeBlock()" instead of the public one, because there might be a
                    // Locking problem is this class is extended and that method overriden. We might have to make this class
                    // FINAL to prevent that from happening...
                    T tr = createTransaction();
                    _removeBlock(tr, hashBlockToRemove.toString());
                    commitTransaction(tr);

                    if (removeTxs) removeBlockTxs(hashBlockToRemove);
                    numBlocksRemoved++;
                }
                hashesBlocksToRemove.add(hashBlockToRemove);

                // If it does not have parent or the parent has more than one Child, we stop right here:
                if (parentHashOpt.isEmpty()) break;
                if (getNextBlocks(parentHashOpt.get()).size() > 0) break;

                // In the next iteration we try to prune its parent...
                hashBlockToRemove = parentHashOpt.get();
                parentHashOpt = getPrevBlock(hashBlockToRemove);
            } // while...

            getLogger().debug("chain tip #{} Pruned. {} blocks removed.",tipChainHash, numBlocksRemoved);

            // We trigger a Prune Event:
            if (removeBlocks) {
                ChainPruneEvent event = new ChainPruneEvent(tipChainHash, parentHashOpt.get(), hashesBlocksToRemove);
                getEventBus().publish(event);
            }
            return hashesBlocksToRemove;
        } finally {
            getLock().writeLock().unlock();
        }
    }

    // It performs an Automatic Prunning: It search for Fork Chains, and if they meet the criteria to be pruned, it
    // prunes them. Criteria to prune a Chain:
    // - it is NOT the longest Chain
    // - its Height is >= than "prunningHeightDifference"
    // - the difference of age between the block of the tip and the on in the tip of the longest chain is
    //   longer than "prunningAgeDifference"

    default void _automaticForkPrunning() {
        try {
            getLock().writeLock().lock();
            getLogger().debug("Automatic Fork Pruning initiating...");

            // We only prune if there is more than one chain:
            List<Sha256Hash> tipsChain = getTipsChains();
            if (tipsChain != null && (tipsChain.size() > 1)) {
                ChainInfo longestChain = getLongestChain().get();
                List<Sha256Hash> tipsToPrune = getState().getTipsChains().stream()
                        .filter(c -> (!c.equals(longestChain))
                                && ((longestChain.getHeight() - c.getHeight()) >= getConfig().getForkPruningHeightDifference()))
                        .map(c -> c.getHeader().getHash())
                        .collect(Collectors.toList());

                // Based on Config, we perform REAL Prunning (removing Blocks), or trigger an ALERT, or both:
                boolean prunning      = getConfig().isForkPruningAutomaticEnabled();
                boolean prunningAlert = getConfig().isForkPruningAlertEnabled();
                boolean removeTxs     = getConfig().isForkPruningIncludeTxs();

                if (prunning || prunningAlert) {
                    tipsToPrune.forEach(c -> {
                        List<Sha256Hash> blocksMightBeRemoved = prune(c, prunning, removeTxs);
                        if (prunningAlert) {
                            getEventBus().publish(new ChainPruneAlertEvent(c, blocksMightBeRemoved));
                        }
                    });
                }

            }
            getLogger().debug("Automatic Fork Pruning finished.");
        } finally {
            getLock().writeLock().unlock();
        }
    }

    default void _automaticOrphanPrunning() {

        getLogger().debug("Automatic Orphan Pruning initiating...");
        int numBlocksRemoved = 0;
        // we get the list of Orphans, and we remove them if they are old" enough:
        Iterator<Sha256Hash> orphansIt = getOrphanBlocks().iterator();
        while (orphansIt.hasNext()) {
            Sha256Hash blockHash = orphansIt.next();
            Optional<HeaderReadOnly> blockHeaderOpt = getBlock(blockHash);
            if (blockHeaderOpt.isPresent()) {
                Instant blockTime = Instant.ofEpochSecond(blockHeaderOpt.get().getTime());
                if (Duration.between(blockTime, Instant.now()).compareTo(getConfig().getOrphanPruningBlockAge()) > 0) {
                    removeBlock(blockHash);
                    numBlocksRemoved++;
                    getLogger().debug("Automatic Orphan Pruning:: Block {} pruned.", blockHash.toString());
                }
            }
        } // while...
        getLogger().debug("Automatic Orphan Prunning finished. {} orphan Blocks Removed", numBlocksRemoved);
    }

    private void _validateBlock(HeaderReadOnly candidateBlockHeader, BlockChainInfo candidateChainInfo) throws BlockChainRuleFailureException {

        ChainInfoBean chainInfoBean = new ChainInfoBean(candidateBlockHeader);
        chainInfoBean.setChainWork(candidateChainInfo.getChainWork());
        chainInfoBean.setHeight(candidateChainInfo.getHeight());
        chainInfoBean.makeImmutable();

        validateBlockChainInfo(chainInfoBean);
    }

}