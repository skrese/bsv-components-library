# JCL-Store

*JCL-Store* is the Storage Module in *JCL*. It provides capabilities to Store information and retrieve it at any time.
The term "information" used here is generic: different applications or modules might have different needs, so 
there is no one-solution-fits-all in this case. Instead, *JCL-Store* provides several *Interfaces/Components*, each one providing 
a different set of functionalities.

*JCL-Store* only provides the definition of the storage interfaces. For each interface, several 
implementations might be provided, but the behaviour of all of them must comply with the specification defined in this document.

**Use *JCL-Store* if you need to:**

 * Store information about *Transactions* and *Blocks* in a permanent storage, so it can be recovered later on.
 * store information about the multiple *chains* that might be present in a specific network
 * Be notified about *Fork* or *Prune* Events.

 
The Interfaces defined in this module are the following:


 * **BlockStore:**
 This interface provides operations to save/retrieve/remove Blocks and Transactions. Blocks and Transactions are stored
 separately, but their relationship is also stored, so it's also possible to retrieve those Transactions belonging to a
 Block, for example. This interface is simple by design, so implementations can provide high performance. 
 
 * **BlockChainStore:**
 This interface is an extension of *BlockStore*, that is, it already provides all the functionalities of *BlockStore*,
 and it also adds operations to retrieve information about the *Chain* of Blocks. As Blocks are added to the Store, 
 this interface will keep track of the *Chain* of blocks, and also of any possible *Fork* that might happen. So in this
 case the Blocks are not separate entities, but now it's also possible to *traverse* the chain starting from any 
 specific Block, going back and forth through the chain, detect *Forks*, *prune* branches, etc.

> **About creating Instances:**
> 
> In *JCL-Store*, only guidelines about how to use these interfaces is provided. Before using an interface an instance
> must be obtained, but that process is implementation-specific. So for instructions fo how to crete instances of 
the interfaces above, go check out the documentation of all the implementations provided:
>
> [JCL-Store-LevelDB](../../store-levelDB/doc/README.md): *JCL-Store* implementation with LevelDB 
> 
> [JCL-Store-FoundationDB](../../store-foundationDB/doc/README.md): *JCL-Store* implementation with FoundationDB 
  

Examples of use of these 2 interfaces are detailed in the chapters below

## BlockStore interface

This interface provides basic operations to manipulate Blocks and Transactions, and also allows for being notified about 
different Events

### Starting and stopping the Service:

The Interface provides 2 methods that must be invoked *before* and *after* its usage. The objective of these methods are implementation-specific, but they are meant for initialization and cleaning processes.

```
BlockStore db = ...                     // we get an instance of BlockStore
try {
    db.start();
    ... // do something...
} catch (Exception e) {
	e.printStacktrace();
} finally {
    db.stop();
}

```


### Saving and retrieving Blocks and Txs

The operations are quite straightforward. Once you have the Block or Transactions you want to store, you just call 
the methods:

```
// We store Blocks:
BlockHeader blockHeader = ...           // we get a Block Header from somewhere
List<BlockHeader> blockHeaders = ...    // We got a List of Blocks from somewhere
db.saveBlock(blockHeader);
db.saveBlocks(blockHeaders);

// We store Txs serparately:
Transaction tx = ...                    // we got a Tx from somewhere
List<Transaction> txs = ...             // we got a List of Txs from somewhere
db.saveTx(Tx);
db.saveTxs(txs)

```

In the example above, both Blocks and Transactions are stored separately, without relation to 
each other. In most situations, its important to also store the *link* between them. That relationship
can be stored at different points in time. For example, in the following example, the relation is 
stored at the very moment when they are saved:

```
BlockHeader blockHeader = ...           // we get a Block Header
List<Transaction> txs = ...             // we got a List of Tx belong to that Block

db.saveBlockTxs(blockHeader, txs);

```

It's also possible to store the Block and Transaction separately and set that relation later on:

```
BlockHeader blockHeader = ...           // we get a Block Header
List<Transaction> txs = ...             // we got a List of Tx belong to that Block

db.saveBlock(block);
db.saveTxs(txs);
txs.forEach(tx -> db.linkTxToBlock(tx, blockHeader));

```

The *linkTxToBlock* method above stores the relationship between the Block and the Txs, but the interface also provides 
other methods that allows for modifying this relationship, like:

 * ``linkTxToBlock*(...)``: Links a Tx to one Block.
 * ``unlinkTxFromBlock(...)``: Unlinks a Tx from a Block.
 * ``unlinkTx(..)``: Unlinks a Transaction from any whatever Block this Txs its linked to
 * ``unlinkBlock(...)``: Unlinks a Block form any Tx it might be linked to


> Using the methods above create a relation between a Transaction and a Block. This relation might be in one of the following scenarios:
> 
>  * A Block might be *empty*. or contain several Transactions, with no upper limit in that number of *Transactions*.
>  * A Transaction might NOT belong to any block at all. This is the situation when the Block i being mined and this Tx is not included in a block just yet.
>  * A Transaction might belong to ONLY one Block. This is the "normal" scenario where a Transaction is contained in a Block that has been mined and its part of the BlockChain.
>  * A Transaction might belong to MORE than 1 Block. This is the scenario of a FORK, where 2 or more different Blocks are competing to be the longest Chain, and they all contain the same Transaction.
>
>
>All the scenarios above are valid and can be modelled. The method ``getBlockHashLinkedToTx`` returns a *List* of those Blocks the Tx given belongs to. The returned List might an Empty list or a list with 1 or more elements, according to the scenarios described above.
> 


We can retrieve the Txs belonging to a Block. The result comes as an *Iterable* of the Transactions *Hashes* contained in that block:

```
Iterable<Sha256Wrapper> getBlockTxs(Sha256Wrapper blockHash);
```

An example of looping over the results of the prvious method:


```
// We get a Block from somwhere:
BlockHeader block = ...

// We print first the Total number of Txs:
System.out.println("Number of Txs:" + db.getBlockNumTxs(block.getHash()));

// No we print every Txs contained in this block:
Iterator<Sha256Wrapper> txsIt = db.getBlockTxs(block.getHash()).iterator();
while (txsIt.hasNext()) {
System.out.println("Tx Hash: " + txsIt.next());
}
```

### Streaming of Events

The *BlockStore* interface provides an *endpoint* that can be used to *listen* to *Events* that are triggered when some 
operations are performed:

 * when blocks are stored
 * when blocks are removed
 * when Txs are stored
 * when Txs are removed


The endpoint is the *EVENTS()* method, and we can *listen* to an Event by providing a *callback* by using the *forEach* 
method, in a similar way as with the *Java Streams*:

```
db.EVENTS().BLOCKS_SAVED.forEach(System.out::println)
db.EVENTS().BLOCKS_REMOVED.forEach(System.out::println)
db.EVENTS().TXS_SAVED.forEach(System.out::println)
db.EVENTS().TXS_REMOVED.forEach(System.out::println)

```

The examples above are using a very simple *lambda* expression that just prints out the Event received, but we can 
develop a custom method that does the same thing, and also gives us access to the *Event* object:

```
db.EVENTS().BLOCKS_SAVED.forEach(this::processBlocksSaved)
db.EVENTS().BLOCKS_REMOVED.forEach(this::processBlocksRemoved)
db.EVENTS().TXS_SAVED.forEach(this::rocessTxsSaved)
db.EVENTS().TXS_REMOVED.forEach(this::processTxsRemoved)
...
public void processBlocksSaved(BlocksSavedEvent event) {
    System.out.println(event);
}
public void processBlocksRemoved(BlocksRemovedEvent event) {
    System.out.println(event);
}
public void processTxsSaved(TxsSavedEvent event) {
    System.out.println(event);
}
public void processTxsRemoved(TxsRemovedEvent event) {
    System.out.println(event);
}

```



> **NOTE:**
> 
> Triggering Events might affect the performance. The *BlockStore* interface describes *how* to 
access them, but it can NOT guarantee whether they are enable or not. Enabling or disabling the Events 
is implementation-specific, so **go check the implementation documentation** for the guidelines about how
to enable/configure the trigering of Events.
>
> [JCL-Store-LevelDB](../../store-levelDB/doc/README.md): *JCL-Store* implementation with LevelDB
> 
> [JCL-Store-FoundationDB](../../store-foundationDB/doc/README.md): *JCL-Store* implementation with FoundationDB 



### Filtering of Events

All the Events streamed by the *BlockStore* components can also be filtering out by using a *filter()* method, which 
accepts a *Predicate* as an argument and can be used in a similar way as the *Java Streams*:

```
...
ShaWrapper blockHashToSearch = ShaWrapper.wrap("000000000000000001f013dffd431ef7b833197d78bb22f9f81cfec4659db7ba")
db.EVENTS().BLOCKS_SAVED
    .filter(e -> e.getBlockHashes().contains(BlockHashToSearch))
    .forEach(e -> System.out.println("Block " + blockHashToSearch + " inserted.")
...
```

The previous example prints out a Message only when a specific Block has been Saved


### Comparing Blocks

In some scenarios, it's useful to compare 2 Block, to check what Transactions they have in common, or what Transaction on of them has but not the other one. For this scenario you can use the following method:

```
Optional<BlocksCompareResult> compareBlocks(Sha256Wrapper blockHashA, Sha256Wrapper blockHashB);
```

That Methods return the result *inmediately*. That result is composed of several *iterables*, each one of them showing different information: one will iterate over the Transactions both blocks have in common, another will do the same but over those Transactions that only the first Block has but not the second, etc. The complexity here is not coming from getting the result (which is inmediate), but from iterating those results (whch might take more or less time depending on the number of Txs, but the complexity to loop over the whole set of Transaction is *O(n)*.

### Getting Transactions Dependent

A Transaction *A* depends on a Transactions *B* and *C* if *A* is using some of the *outputs* from *B* and *C*. So the Transaction *A* will ony be validated after the Transactions *B* and *C* have been validated as well.

There is a specific method to retrieve the *Transactions* that one Transaction relies on:

```
List<Sha256Wrapper> getTxsNeeded(Sha256Wrapper txHash);
```

In the example above, this method will return a list containing the *Hashes* of the Transactions *B* and *C*.

> Note that in this example, the Transaction *B* and *C* might NOT be stored yet in the DB, since the Transactions might come in different order when you receive them from the network.


### Reference

#### *Events* streamed by the *BlockStore* interface:

##### BlocksSavedEvent

An Event triggered when a one or more Blocks have been saved in the Store.
 This Event is accesible by: ``[BlockStore].EVENTS().BLOCKS_SAVED``
 
 The Event class passed as parameter to the *forEach* method is an instance of
``com.nchain.jcl.store.blockStore.events.BlocksSavedEvent``

##### BlocksRemovedEvent

An Event triggered when a one or more Blocks have been removed from the Store.
 This Event is accesible by: ``[BlockStore].EVENTS().BLOCKS_REMOVED``
 
 The Event class passed as parameter to the *forEach* method is an instance of
``com.nchain.jcl.store.blockStore.events.BlocksRemovedEvent``


##### TxsSavedEvent

An Event triggered when a one or more Txs have been saved in the Store.
 This Event is accesible by: ``[BlockStore].EVENTS().TXS_SAVED``
 
 The Event class passed as parameter to the *forEach* method is an instance of
``com.nchain.jcl.store.blockStore.events.TxsSavedEvent``


##### TxsRemovedEvent

An Event triggered when a one or more Txs have been removed in the Store.
 This Event is accesible by: ``[BlockStore].EVENTS().TXS_REMOVED``
 
 The Event class passed as parameter to the *forEach* method is an instance of
``com.nchain.jcl.store.blockStore.events.TxsRemovedEvent``


## BlockChainStore interface

The *BlockChainStore* component is an *extension* of the *BlockStore* Module. It provides *ALL* the capabilities that
*BlockSore* does, and it also adds more. The new functionalities here are all about providing information of the *Chain* 
of Blocks.

In the *BlockStore* component, both *Blocks* and *Transactions* are separate entities. We can link a *Transaction* to a
Block so we can get all the *Transactions* belonging to a Block, and we can also *travel* from a Block to its *parent*,
 since that information is contained within the *BlockHeader* itself, but that's all we can do.
 
*Blocks* are part of a bigger structure called *Chain*, which is also known as *BlockChain*. This
 *Chain* is a sequence of *Blocks* in an specific order. The *BlockChainStore* components offers new methods to provide
 useful information about this *Chain*, like:
 
  * get relative information about the *Chain*, like its *height* or proof of Work. This information is "relative" because
    its specific for a particular *Block*. Another *Block* in the same chain will have a different *height* and a 
    different *proof of Work*.
  * Useful method to *traverse* the chain, not ony from a *Block* to it's *parent* in the chaib, but also from a *Block*
    to its "children", so you can *traverse* back and forth.
  * It keeps track of the *Tip* of the *Chain* (the most recent *Block* stored)
  * In case there is a *Fork* (more than 2 chains are being built on top of a Block), it also keeps track of all those
    individual "Chains".
  * It provides methods to "prune" a Chain. This process can also be performed automatically.     





### Getting Chain Information

The *getBlockChainInfo()* accetps a *Block Hash* as a parameter and returns an instance of *ChainInfo*, which stores
information about the *Chain* that Block belongs to. This information is relative to this *Block*. If the *Block* is 
not Store, or it's stored but its *NOT* connected to a *Chain* (because for instance its parent has not been stored yet,
since *Blocks* might be stored in a different order), then it returns an *Empty* Optional.

```
Optional<ChainInfo> getBlockChainInfo(Sha256Wrapper blockHash);
```

In the following picture we can see 2 examples of the DB state at 2 points in time: In the **State A**, 5 blocks are stored: #1, #2, #4 and #5. Only the Blocks #1 and #2 are connecgted to the Chain, so we only have **ChainInfo** for them. In the **State B**, the Block #3 is inserted, and at this moment is automatically **connected** to the Chain, since it's parent (#2) is already stored. And this **connection** process is also triggered through all the Blocks built on top of #3, so in the end all the blocks are connected and we can potentially get **CHainInfo** from all of them.


![getChainInfo example](chainInfoExample.png)

### Traversing the Chain

Using the original *BlockStore* Component is already possible to travel from a *Block* to its parent, and by repeating
this process you could go up to the beginning of the *Chain*. The *BlockChainStore* makes this "traversing" more 
explicit by adding methods to go "back" to its parent and "forth" to the *Children* of a *Block* given:

```
Optional<Sha256Wrapper> getPrevBlock(Sha256Wrapper blockHash);
List<Sha256Wrapper> getNextBlocks(Sha256Wrapper blockHash);
```

In a regular situation when we only have one *Chain*, the ``getNextBlocks`` will return either a List of one *Block*, or 
an empty list. But in the rare case there is a *Fork* beginning at this *Block*, we might have *more* than one *children*, 
so that's why the methods returns a *List*.


### Tips of the Chains

The *BlockChainStore* components provides methods to get direct information about the *Tips* of the *Chains* stored in
the DB:

```
List<Sha256Wrapper> getTipsChains();
Optional<ChainInfo> getLongestChain();
```

An example of the the preovous method used on the following DB State:

![getChainInfo example](tipsChainExample.png)


### Forks and prunning

Due to the nature itself of the *BlockCHain*, a *Fork* might appear from time to time. A *Fork* is a situation where 
*more* than 1 *Blocks* are being built on top of an existing *Block*, so we end up with 2 different *Chains*. 

> The *BlockChainStore* component, as we'll see in another chapter further down, can send *notifications* (Events) when 
a *Fork* is detected.

These 2 chains keep growing, but over time the *P2P* network chooses automatically the best/longest one, and the other is 
abandoned so no further Blocks are built on top of it. But even if one *Chain* is abandoned, the blocks that were 
originally built on top of that are still stored in the DB, so it's a good idea to remove them once the Chain as been discarded
by the Network. The *BlockChainStore* Component provides 2 ways to "remove/prune" a *Chain*:

 * by using the *prune()* method.
 * by letting the *BlockChainStore* component to do it automatically.
 

The *prune()* methods allows for removing a *Chain* by specifying the *Block* that makes the *Tip* of that Chain. All 
the Blocks belonging to that *Chain* will be removed, starting from its *Tip* and all the way back until the moment the 
*Fork* was created.

The following example shows the state of the Chain before and after prunning a Fork. notice that only the *Tip* of the Fork needs to be specified in the **prune** method:

![getChainInfo example](forkExample.png)

#### Automatic prunning

The problem with the *prune()* method is that is not easy to know when its "safe" to *prune* a Chain. A *Chain* can *ONLY* 
be safely removed when that Chain has been discarded by the Network and no *Blocks* are being built on top of it. So it's not 
enough to "know" when a *FORK* has happened, we also need to *wait* until we are sure that the *Chain* is not the longest 
one and it can be removed. For all these reasons, the *BlockChainSore* also includes, as part of its specification, an 
"automatic" prunning system:

> By enabling the *Automatic Prunning*, the *BlockChainStore* Component will take care of keeping track of all the *Chains*, and
it will also detect when the right time for removing/prunning one of them is. A *Chain* can safely be removed when the 
difference in height with the longest *Chain* is bigger than a *Threshold* specified. The *Automatic Prunning* Configuration
is implementation-specific, so **go check the documentation**:
>
> [JCL-Store-LevelDB](../store-levelDB/doc/README.md): *JCL-Store* implementation with LevelDB 



### Streaming of Events

In a similar fashion as the *BlockStore* Component, the *BlockChainStore* component also streams some Events, and those 
Events can be subscribed to, so we can get notified and run our business logic. 

 * You can subscribe to a *FORK* Event, which happens when a *Fork* is detected
 * You can subscribe to a *PRUNE* Event, which happens when a *Chain* has been pruned, either manually (by the ``prune()``
   method), or automatically
 * You can subscribe to a *STATE* Event, which will be explained in a separate chapter.   


#### State Streaming

Apart form the *Events* triggered when some operations are performed, the *BlockChainStore* interface also provides 
an *endpoint* to an Event that can be triggered on a scheduled basis. This is the case of the *STATE* event. The *STATE*
event is an Event that is triggered using a pre-defined frequency set up by the user, and it contains useful information
about the state of the DB (Number of Blocks, Txs, etc) 

```
db.EVENTS().STATE.forEach(System.out::println)
```

> Enabling the Streaming of *STATE* Events is implementation-specific, so **go check out** the documentation:
>
> [JCL-Store-LevelDB](../../store-levelDB/doc/README.md): *JCL-Store* implementation with LevelDB 

### Reference

#### *Events* streamed by the *BlockChainStore* interface:

##### ChainForkEvent

An Event triggered when a Fork is detected: when a *Block* has just been saved, and the *parent* of this *Block* has
ALREADY other Block built on top of it.
 This Event is accesible by: ``[BlockStore].EVENTS().FORKS``
 
 The Event class passed as parameter to the *forEach* method is an instance of
``com.nchain.jcl.store.blockChainStore.events.ChainForkEvent``

##### ChainPruneEvent

An Event triggered when a *Chain* has just been pruned. This might happen on-demand (by invoking the *prune()* method), or
automatically (by the *Automatic Prunning*)
 This Event is accessible by: ``[BlockStore].EVENTS().BLOCKS_REMOVED``
 
 The Event class passed as parameter to the *forEach* method is an instance of
``com.nchain.jcl.store.blockChainStore.events.ChainPruneEvent``


##### ChainStateEvent

An Event triggered periodically based on the configuration (implementation-specific) provided.
 This Event is accesible by: ``[BlockStore].EVENTS().STATE``
 
 The Event class passed as parameter to the *forEach* method is an instance of
``com.nchain.jcl.store.blockChainStore.events.ChainStateEvent``