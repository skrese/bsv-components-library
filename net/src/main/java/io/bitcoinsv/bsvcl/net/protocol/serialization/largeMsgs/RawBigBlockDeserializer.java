package io.bitcoinsv.bsvcl.net.protocol.serialization.largeMsgs;


import io.bitcoinsv.bsvcl.net.protocol.messages.*;
import io.bitcoinsv.bsvcl.net.protocol.serialization.BlockHeaderMsgSerializer;
import io.bitcoinsv.bsvcl.net.protocol.serialization.RawBlockMsgSerializer;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An implementation of Big Blocks Deserializer. Its based on the LargeMessageDeserializerImpl, so the general
 * behaviour consists of deserializing "small" parts of the Block and notify them using the convenience methods
 * "notify" provided by the parent Class. Those notifications will trigger callbacks that previously must have been
 * fed by the client of this class. All notifications will contain Raw Tx Data.
 */
public class RawBigBlockDeserializer extends LargeMessageDeserializerImpl {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(RawBigBlockDeserializer.class);

    // Once the Block Header is deserialzed, we keep a reference here, since we include it as well when we
    // deserialize each set of TXs:
    private BlockHeaderMsg blockHeader;

    /** Constructor */
    public RawBigBlockDeserializer(ExecutorService executor) {
        super(executor);
    }

    /** Constructor. Callbacks will be blocking */
    public RawBigBlockDeserializer() {
        super(null);
    }

    @Override
    public void deserializeBody(DeserializerContext context, HeaderMsg headerMsg, ByteArrayReader byteReader) {
        try {
            // We update the reader:
            adjustReaderSpeed(byteReader);

            // We first deserialize the Block Header:
            log.trace("Deserializing the Block Header...");
            blockHeader = BlockHeaderMsgSerializer.getInstance().deserialize(context, byteReader);
            PartialBlockHeaderMsg partialBlockHeader = PartialBlockHeaderMsg.builder()
                    .headerMsg(headerMsg)
                    .blockHeader(blockHeader)
                    .txsSizeInBytes(context.getMaxBytesToRead() - blockHeader.getLengthInBytes())
                    .blockTxsFormat(PartialBlockHeaderMsg.BlockTxsFormat.RAW)
                    .build();
            notifyDeserialization(partialBlockHeader);

            // Now we Deserialize the Txs, in batches...
            log.trace("Deserializing TXs...");

            // We use a RawBlockMsgSerializer, which already contains the logic for this:
            RawBlockMsgSerializer rawBlockMsgSerializer = RawBlockMsgSerializer.getInstance();

            long txsBytesSize = context.getMaxBytesToRead() - blockHeader.getLengthInBytes();
            long totalBytesRemaining = txsBytesSize;
            int totalSizeInBatch = 0;

            // Order of each batch of Txs within the Block
            long txsOrderNumber = 0;

            // Index of the FIRST Tx in this Chunk within the Block
            long txsIndexNumber = 0;

            //record each tx in this batch
            List<RawTxMsg> rawTxBatch = new ArrayList<>();

            while (totalBytesRemaining > 0) {

                RawTxMsg tx = rawBlockMsgSerializer.deserializeNextTx(context, byteReader);
                rawTxBatch.add(tx);
                long totalBytesInTx = tx.getLengthInBytes();
                totalSizeInBatch += totalBytesInTx;

                if(totalSizeInBatch > super.partialMsgSize){
                    // We do not Have enough space in this Batch for this Tx. push the batch we have so far down the pipeline
                    PartialBlockRawTxMsg partialBlockRawTXs = PartialBlockRawTxMsg.builder()
                            .blockHeader(blockHeader)
                            .txs(rawTxBatch)
                            .txsOrdersNumber(txsOrderNumber)
                            .txsIndexNumber(txsIndexNumber)
                            .build();
                    notifyDeserialization(partialBlockRawTXs);

                    //we're now moving onto the next batch
                    txsOrderNumber++;
                    txsIndexNumber += rawTxBatch.size();
                    totalSizeInBatch = 0;

                    // We mov on to next Chunk:
                    rawTxBatch = new ArrayList<>();
                }

                totalBytesRemaining -= totalBytesInTx;
            }

            // flush any remaining txs
            if(rawTxBatch.size() > 0){
                //push the batch down the pipeline
                PartialBlockRawTxMsg partialBlockRawTXs = PartialBlockRawTxMsg.builder()
                        .blockHeader(blockHeader)
                        .txs(rawTxBatch)
                        .txsOrdersNumber(txsOrderNumber)
                        .txsIndexNumber(txsIndexNumber)
                        .build();
                notifyDeserialization(partialBlockRawTXs);
            }

            // We reset the reader as it was before..
            resetReaderSpeed(byteReader);

        } catch (Exception e) {
            //e.printStackTrace();
            notifyError(e);
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }
}
