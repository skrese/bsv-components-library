package com.nchain.jcl.net.protocol.serialization.largeMsgs;


import com.nchain.jcl.net.protocol.messages.*;
import com.nchain.jcl.net.protocol.serialization.BlockHeaderMsgSerializer;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.serialization.BitcoinSerializerUtils;
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
public class BigBlockRawTxDeserializer extends LargeMessageDeserializerImpl {

    // Size of each Chunk of TXs in byte array format:
    private static final int TX_LIST_TOTAL_BYTES = 10_000_000;    // 10 MB

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BigBlockRawTxDeserializer.class);

    // Once the Block Header is deserialzed, we keep a reference here, since we include it as well when we
    // deserialize each set of TXs:
    private BlockHeaderMsg blockHeader;

    /** Constructor */
    public BigBlockRawTxDeserializer(ExecutorService executor) {
        super(executor);
    }

    /** Constructor. Callbacks will be blocking */
    public BigBlockRawTxDeserializer() {
        super(null);
    }

    @Override
    public void deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        try {
            // We update the reader:
            adjustReaderSpeed(byteReader);

            // We first deserialize the Block Header:
            log.trace("Deserializing the Block Header...");
            blockHeader = BlockHeaderMsgSerializer.getInstance().deserialize(context, byteReader);
            PartialBlockHeaderMsg partialBlockHeader = PartialBlockHeaderMsg.builder()
                    .blockHeader(blockHeader)
                    .txsSizeInBytes(context.getMaxBytesToRead() - blockHeader.getLengthInBytes())
                    .blockTxsFormat(PartialBlockHeaderMsg.BlockTxsFormat.RAW)
                    .build();
            notifyDeserialization(partialBlockHeader);

            // Now we Deserialize the Txs, in batches..
            log.trace("Deserializing TXs...");

            long txsBytesSize = context.getMaxBytesToRead() - blockHeader.getLengthInBytes();
            long totalBytesRemaining = txsBytesSize;
            int totalSizeInBatch = 0;

            // Order of each batch of Txs within the Block
            long txsOrderNumber = 0;

            //record each tx in this batch
            List<RawTxMsg> rawTxBatch = new ArrayList<>();

            while (totalBytesRemaining > 0) {
                int totalBytesInTx = 0;

                //deserialize tx
                totalBytesInTx += 4; //version

                //input count
                long inputCount =  BitcoinSerializerUtils.deserializeVarIntWithoutExtraction(byteReader, totalBytesInTx);
                totalBytesInTx += BitcoinSerializerUtils.getVarIntSizeInBytes(inputCount);

                //calculate total bytes in txInput
                for (int i = 0; i < inputCount; i++) {
                    totalBytesInTx += 36; //outpoint;

                    //script length
                    long scriptLen = BitcoinSerializerUtils.deserializeVarIntWithoutExtraction(byteReader, totalBytesInTx);
                    totalBytesInTx += BitcoinSerializerUtils.getVarIntSizeInBytes(scriptLen);

                    //script
                    totalBytesInTx += scriptLen;

                    totalBytesInTx += 4; //sequence
                }

                long outputCount = BitcoinSerializerUtils.deserializeVarIntWithoutExtraction(byteReader, totalBytesInTx);
                totalBytesInTx += BitcoinSerializerUtils.getVarIntSizeInBytes(outputCount);

                for (int i = 0; i < outputCount; i++) {
                    totalBytesInTx += 8; //value

                    //script length
                    long scriptLen = BitcoinSerializerUtils.deserializeVarIntWithoutExtraction(byteReader, totalBytesInTx);
                    totalBytesInTx += BitcoinSerializerUtils.getVarIntSizeInBytes(scriptLen);

                    //script
                    totalBytesInTx += scriptLen;
                }

                totalBytesInTx += 4; //lock time

                //if we have enough space then add it
                if(totalSizeInBatch + totalBytesInTx <= TX_LIST_TOTAL_BYTES){
                    totalSizeInBatch += totalBytesInTx;
                    rawTxBatch.add(new RawTxMsg(byteReader.read(totalBytesInTx)));
                } else {
                    // We do not Have enough space in this Batch for this Tx. push the batch we have so far down the pipeline
                    PartialBlockRawTxMsg partialBlockRawTXs = PartialBlockRawTxMsg.builder()
                            .blockHeader(blockHeader)
                            .txs(rawTxBatch)
                            .txsOrdersNumber(txsOrderNumber)
                            .build();
                    notifyDeserialization(partialBlockRawTXs);

                    //we're now moving onto the next batch
                    rawTxBatch = new ArrayList<>();
                    txsOrderNumber++;
                    totalSizeInBatch = 0;

                    // We add this Tx tot he next Batch:
                    rawTxBatch.add(new RawTxMsg(byteReader.read(totalBytesInTx)));

                    // If the size of this individual Tx is already bigger than our Max Batch size, this Txs will be
                    // pushed down in the next iteration, but we warm of this situation here...
                    if(totalBytesInTx > TX_LIST_TOTAL_BYTES){
                        log.warn("Tx bigger than the current max Batch size has been added to the Batch, it will be pushed next.");
                    }

                }

                totalBytesRemaining -= totalBytesInTx;
            }

            //flush any remaining txs
            if(rawTxBatch.size() > 0){
                //push the batch down the pipeline
                PartialBlockRawTxMsg partialBlockRawTXs = PartialBlockRawTxMsg.builder()
                        .blockHeader(blockHeader)
                        .txs(rawTxBatch)
                        .txsOrdersNumber(txsOrderNumber)
                        .build();
                notifyDeserialization(partialBlockRawTXs);
            }

            // We reset the reader as it was before..
            resetReaderSpeed(byteReader);

        } catch (Exception e) {
            e.printStackTrace();
            notifyError(e);
        }
    }
}
