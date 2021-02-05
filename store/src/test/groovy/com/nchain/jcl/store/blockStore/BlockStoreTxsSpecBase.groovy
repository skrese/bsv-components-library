package com.nchain.jcl.store.blockStore



import com.nchain.jcl.store.common.TestingUtils
import io.bitcoinj.bitcoin.api.base.Tx
import io.bitcoinj.core.Sha256Hash

import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors

/**
 * Testing class for Basic Scenarios with Txs (inserting, removing, etc).
 *
 * SO this class can NOT be tested itself, it needs to be extended. An extending class must implement the "getInstance"
 * method, which returns a concrete implementation of the BlockStore interface (like a LevelDB or FoundationDB
 * Implementation).
 *
 * Once that method is implemented, the extending class can be tested without any other additions, since running the
 * extending class will automatically trigger the tests defined in THIS class.
 */
abstract class BlockStoreTxsSpecBase extends BlockStoreSpecBase {

    /**
     * We test that TXs are properly saved and removed into the DB and the related Events are properly triggered.
     */
    def "testing saving/removing Txs"() {
        given:
            println(" - Connecting to the DB...")
            BlockStore db = getInstance("BSV-Main", false, true)
            // We keep track of the Events triggered:
            AtomicInteger numTxsSavedEvents = new AtomicInteger()
            AtomicInteger numTxsRemovedEvents = new AtomicInteger();
            db.EVENTS().TXS_SAVED.forEach({e -> numTxsSavedEvents.incrementAndGet()})
            db.EVENTS().TXS_REMOVED.forEach({e -> numTxsRemovedEvents.incrementAndGet()})

        when:
            db.start()
            //TestingUtils.clearDB(blockStore.db)

            // We define 3 Txs:
            Tx tx1 = TestingUtils.buildTx()
            Tx tx2 = TestingUtils.buildTx()
            Tx tx3 = TestingUtils.buildTx()

            // We save 1 individual Txs:
            long numTxsBeforeAll = db.getNumTxs()
            boolean isTx1FoundBeforeInserting = db.containsTx(tx1.getHash())
            println(" - Saving Tx " + tx1.getHash().toString() + "...")
            db.saveTx(tx1)
            long numTxsAfter1Tx = db.getNumTxs()
            boolean isTx1FoundAfterInserting = db.containsTx(tx1.getHash())

            // We save the remaining 2 Txs in a single batch:
            println(" - Saving a Batch of 2 Txs:")
            println(" - tx " + tx2.getHash().toString())
            println(" - tx " + tx3.getHash().toString())

            db.saveTxs(Arrays.asList(tx2, tx3))
            long numTxsAfter3Txs = db.getNumTxs()

            // We check the DB Content in the console...
            db.printKeys()

            // We remove one Tx individually:
            println(" - Removing Tx " + tx2.getHash().toString() + "...")
            db.removeTx(tx2.getHash())
            long numTxsAfterRemove1Tx = db.getNumTxs()

            // The 2 remaining Txs are removed in a single Batch:
            println(" - Removing a Batch of 2 Txs:")
            println(" - tx " + tx2.getHash().toString())
            println(" - tx " + tx3.getHash().toString())
            db.removeTxs(Arrays.asList(tx1.getHash(), tx3.getHash()))
            long numTxsAfterRemove3Tx = db.getNumTxs()

            // We check the DB Content in the console...
            db.printKeys()

        then:
            numTxsBeforeAll == 0
            numTxsAfter1Tx == 1
            !isTx1FoundBeforeInserting
            isTx1FoundAfterInserting
            numTxsAfter3Txs == 3
            numTxsSavedEvents.get() == 2
            numTxsSavedEvents.get() == 2
            numTxsAfterRemove1Tx == 2
            numTxsAfterRemove3Tx == 0
        cleanup:
            println(" - Cleanup...")
            db.removeTxs(Arrays.asList(tx1.getHash(), tx2.getHash(), tx3.getHash()))
            db.printKeys()
            db.stop()
            println(" - Test Done.")
    }

    /**
     * We test that we save several Txs, some of them spending the outputs of others, and then we check that we can
     * recover this information.
     */
    def "testing Txs Needed"() {
        final int NUM_TXS = 3
        given:
            println(" - Connecting to the DB...")
            BlockStore db = getInstance("BSV-Main", false, true)
        when:
            db.start()
            //TestingUtils.clearDB(blockStore.db)
            // We save several Txs: after the First, each one is using an output generated by the previous Txs...
            List<Tx> txs = new ArrayList<>()
            for (int i = 0; i < NUM_TXS; i++) {
                String parentTxHash = (i == 0) ? null : txs.get(i - 1).getHash().toString();
                txs.add(TestingUtils.buildTx(parentTxHash))
            }

            // We save all the Txs
            println(" - Saving " + NUM_TXS + "...")
            txs.forEach({ tx -> println(" - tx " + tx.getHash().toString() + " saved.")})
            db.saveTxs(txs)

            // We check the DB Content in the console...
            db.printKeys()

            // Now we recover each of them, checking that the info about the Txs Needed is correct for each one...
            Boolean OK = true
            List<Sha256Hash> txHashes = txs.stream().map({ tx -> tx.getHash()}).collect(Collectors.toList())
            for (int i = 0; i < NUM_TXS; i++) {
                Tx tx = db.getTx(txHashes.get(i)).get()
                List<Sha256Hash> txsNeeded = db.getPreviousTxs(tx.getHash())
                if (i > 0) {
                    OK &= txsNeeded.size() == 1 &&  txsNeeded.get(0).equals(txHashes.get(i - 1))
                }
            }
        then:
            OK
        cleanup:
            println(" - Cleanup...")
            db.removeTxs(txs.stream().map({tx -> tx.getHash()}).collect(Collectors.toList()))
            db.printKeys()
            db.stop()
            println(" - Test Done.")
    }
}
