package io.bitcoinsv.bsvcl.common.unit.bigObjects.receivers


import spock.lang.Ignore
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.Collectors
import java.util.stream.IntStream

@Ignore
class BigCollectionReceiverCMapTest extends Specification {

    // Size of Each Test Item:
    private static final int ITEM_BYTES_SIZE  = 100_000;       // 100 KB
    // Aprox size of the Keys. We are simulating collections related to blocks, so we use a block Hash as an example
    private static final int KEY_BYTES_SIZE   = "00000000000000000c18143a90c306161909362a00f7cdd441c5630aa0498d34".getBytes(StandardCharsets.UTF_8).length
    // MAx total number of CHUNKS (Entries)
    private static final int MAX_NUM_ENTRIES = 1000;
    // Max Size of each ChronicleMap. Once one is full, a new one is created.
    private static final int CMAP_MAX_ENTRIES  = 10;   // 10 MB

    /**
     * Definition of the Items we'll use of this test.
     */
    class ItemTest {
        private byte[] content;
        ItemTest(byte[] content) {
            this.content = content;
        }
    }

    /**
     * Item Serializer
     */
    class ItemTestSerializer implements io.bitcoinsv.bsvcl.common.bigObjects.stores.ObjectSerializer<ItemTest> {
        @Override
        void serialize(ItemTest item, io.bitcoinsv.bsvcl.common.bytes.ByteArrayWriter writer) {
            writer.write(item.content)
        }
        @Override
        ItemTest deserialize(io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader reader) {
            return new ItemTest(reader.read(ITEM_BYTES_SIZE))
        }
    }


    private io.bitcoinsv.bsvcl.common.bigObjects.receivers.BigCollectionReceiverCMap<ItemTest> getReceiver(io.bitcoinsv.bsvcl.common.config.RuntimeConfig runtimeConfig, String receiverId) {
        return new io.bitcoinsv.bsvcl.common.bigObjects.receivers.BigCollectionReceiverCMap<ItemTest>(runtimeConfig,
                receiverId, new ItemTestSerializer(), KEY_BYTES_SIZE, MAX_NUM_ENTRIES, ITEM_BYTES_SIZE, CMAP_MAX_ENTRIES);
    }

    private io.bitcoinsv.bsvcl.common.bigObjects.BigCollectionChunk<ItemTest> createChunk(int numItems, int ordinal) {
        List<ItemTest> items = IntStream.range(0, numItems)
                .mapToObj({i -> new ItemTest(new byte[ITEM_BYTES_SIZE])})
                .collect(Collectors.toList())
        return new io.bitcoinsv.bsvcl.common.bigObjects.BigCollectionChunkImpl<>(items, ordinal)
    }

    def "testing receiving complete Collection and callback"() {
        given:
            io.bitcoinsv.bsvcl.common.config.RuntimeConfig runtimeConfig = new io.bitcoinsv.bsvcl.common.config.provided.RuntimeConfigDefault()
            String receiverId = "testingReceiver1"
        when:
            io.bitcoinsv.bsvcl.common.bigObjects.receivers.BigCollectionReceiver<ItemTest> receiver = getReceiver(runtimeConfig, receiverId);
            receiver.start()
            // The callback will just set a FLAG to TRUE, and we are going to send/receive 2 collections, so
            // store them in a Map
            AtomicReference<Map> flags = new AtomicReference<>(new ConcurrentHashMap<String, Boolean>())
            receiver.onCollectionCompleted({collectionId, source ->
                println(collectionId + " received completely from " + source + "!")
                flags.get().put(collectionId, true)
            })

            // We send 2 collections, first with 1 item, the second with 2:
            io.bitcoinsv.bsvcl.common.bigObjects.BigCollectionChunk<ItemTest> col1Chunk1 = createChunk(1,0)
            io.bitcoinsv.bsvcl.common.bigObjects.BigCollectionChunk<ItemTest> col2Chunk1 = createChunk(2,0)
            io.bitcoinsv.bsvcl.common.bigObjects.BigCollectionChunk<ItemTest> col2Chunk2 = createChunk(1,1)

            // We also send a "partial" collection:
            io.bitcoinsv.bsvcl.common.bigObjects.BigCollectionChunk<ItemTest> col3Chunk1 = createChunk(1,1)

            // We register collection-1 and send the items:
            receiver.registerNumTotalItems("collection-1", 1, "test")
            receiver.registerIncomingItems("collection-1", col1Chunk1, "test");

            // For the second collection, we send the items first:
            receiver.registerIncomingItems("collection-2", col2Chunk1, "test");
            receiver.registerIncomingItems("collection-2", col2Chunk2, "test");
            receiver.registerNumTotalItems("collection-2", 3, "test")

            // For the third Collection, se do NOT send all the items, so the collection wil never complete:
            receiver.registerNumTotalItems("collection-3", 3, "test")
            receiver.registerIncomingItems("collection-3", col3Chunk1, "test");

            // We check the completion of both Collections:
            boolean col1CompletedFlag = receiver.isCompleted("collection-1")
            boolean col2CompletedFlag = receiver.isCompleted("collection-2")
            boolean col3CompletedFlag = receiver.isCompleted("collection-3")

            // We clear everything:
            receiver.destroy()

        then:
            flags.get().get("collection-1")
            flags.get().get("collection-2")
            !flags.get().get("collection-3")
            col1CompletedFlag
            col2CompletedFlag
            !col3CompletedFlag
    }

    def "testing changing Source and callback"() {
        given:
            io.bitcoinsv.bsvcl.common.config.RuntimeConfig runtimeConfig = new io.bitcoinsv.bsvcl.common.config.provided.RuntimeConfigDefault()
            String receiverId = "testingReceiver2"
            String COLLECTION_ID = "collection-1"
        when:
            io.bitcoinsv.bsvcl.common.bigObjects.receivers.BigCollectionReceiver<ItemTest> receiver = getReceiver(runtimeConfig, receiverId);
            receiver.start()

            // some callbacks to keep track of what's going on:
            AtomicReference<Boolean> sourceChangedFlag = new AtomicReference<>(false);
            AtomicReference<String> completedSource = new AtomicReference<>(null);

            receiver.onSourceChanged({collectionId, source ->
                println("Source changed for collection '" + collectionId + "': New source :'" + source + "'")
                sourceChangedFlag.set(true)
            })
            receiver.onItemsReceived({coleectionID, chunk, String source ->
                println("Chunk with " + chunk.items.size() + " items received from " + source);
            })
            receiver.onCollectionCompleted({collectionId, source ->
                println("Collection Completed from " + source);
                completedSource.set(source)
            })

            // We "register" the total number of Items for this Collection from 2 sources:
            receiver.registerNumTotalItems(COLLECTION_ID, 3, "source-1")
            receiver.registerNumTotalItems(COLLECTION_ID, 3, "source-2")

            // We send chunks from 2 sources: The second source completes the Collection
            io.bitcoinsv.bsvcl.common.bigObjects.BigCollectionChunk<ItemTest> col1Chunk1 = createChunk(2,0)
            receiver.registerIncomingItems("collection-1", col1Chunk1, "source-1")
            io.bitcoinsv.bsvcl.common.bigObjects.BigCollectionChunk<ItemTest> col1Chunk2 = createChunk(3,0)
            receiver.registerIncomingItems("collection-1", col1Chunk2, "source-2")


            // Now we get the iterator, to check the content is right:
            Iterator<io.bitcoinsv.bsvcl.common.bigObjects.BigCollectionChunk<ItemTest>> it = receiver.getCollectionChunks("collection-1")
            boolean contentOK = it.hasNext() && (it.next().items.size() == 3)


            // We clear everything:
            receiver.destroy()

        then:
            contentOK
            sourceChangedFlag.get()
            completedSource.get().equals("source-2")
    }
}