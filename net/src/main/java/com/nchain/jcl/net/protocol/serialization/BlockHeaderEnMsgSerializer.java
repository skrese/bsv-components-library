package com.nchain.jcl.net.protocol.serialization;

import com.nchain.jcl.base.tools.bytes.ByteArrayReader;
import com.nchain.jcl.base.tools.bytes.ByteArrayWriter;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import com.nchain.jcl.net.protocol.messages.*;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;

import java.util.ArrayList;
import java.util.List;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 *  A Serializer for {@link BlockHeaderEnMsg} messages
 */
public class BlockHeaderEnMsgSerializer implements MessageSerializer<BlockHeaderEnMsg> {

    private static BlockHeaderEnMsgSerializer instance;

    private BlockHeaderEnMsgSerializer() { }

    public static  BlockHeaderEnMsgSerializer getInstance() {
        if( instance == null) {
            synchronized (BlockHeaderEnMsgSerializer.class) {
                instance = new BlockHeaderEnMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public BlockHeaderEnMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        int byteToRead = 4 +HashMsg.HASH_LENGTH+ HashMsg.HASH_LENGTH+ BlockHeaderEnMsg.TIMESTAMP_LENGTH+ BlockHeaderEnMsg.NONCE_LENGTH+ BlockHeaderEnMsg.NBITS_LENGTH+ BlockHeaderEnMsg.TX_CNT;

        byteReader.waitForBytes(byteToRead);
        byte[] blockHeaderBytes = byteReader.read(byteToRead);

        HashMsg hash =  HashMsg.builder().hash(
                Sha256Wrapper.wrapReversed(
                        Sha256Wrapper.twiceOf(blockHeaderBytes).getBytes()).getBytes())
                .build();


        // We create a Reader on the Header Bytes, since we need those values again now to serialize the
        // whole Header...
        ByteArrayReader headerReader = new ByteArrayReader(blockHeaderBytes);

        long version = headerReader.readUint32();
        HashMsg prevBlockHash = HashMsg.builder().hash(getBytesHash(HashMsgSerializer.getInstance().deserialize(context, headerReader))).build();
        HashMsg merkleRoot = HashMsg.builder().hash(getBytesHash(HashMsgSerializer.getInstance().deserialize(context, headerReader))).build();

        long creationTime = headerReader.readUint32();
        long difficultyTarget = headerReader.readUint32();
        long nonce = headerReader.readUint32();
        long txCount = headerReader.readInt64LE();

        byteReader.waitForBytes(1);
        boolean noMoreHeaders = byteReader.readBoolean();

        byteReader.waitForBytes(1);
        boolean hasCoinbaseData = byteReader.readBoolean();

        BlockHeaderEnMsg blockHeaderEnMsg;
        BaseGetDataAndHeaderMsgSerializer baseGetDataAndHeaderMsgSerializer =  BaseGetDataAndHeaderMsgSerializer.getInstance();
        if(hasCoinbaseData) {
            List hashes = new ArrayList<byte[]>();
            for(int i=0; i < txCount ; i++ ){
                hashes.add(baseGetDataAndHeaderMsgSerializer.readHashMsg(context, byteReader));
            }

            VarIntMsg txLengthValue = VarIntMsgSerializer.getInstance().deserialize(context, byteReader);
            int coinBaseTxLength = (int) txLengthValue.getValue();

            byteReader.waitForBytes(coinBaseTxLength);
            byte[]  coinbaseTxBytes=  byteReader.get(coinBaseTxLength);

            //creating a coinbaseTX which is not part of the message ,
            TxMsg tx =  TxMsgSerializer.getInstance().deserialize(context, new ByteArrayReader(coinbaseTxBytes));

            VarStrMsg coinbase = VarStrMsgSerializer.getinstance().deserialize(new ByteArrayReader(coinbaseTxBytes),
                    coinbaseTxBytes.length);

            blockHeaderEnMsg = BlockHeaderEnMsg.builder()
                    .hash(hash)
                    .version(version)
                    .prevBlockHash(prevBlockHash).merkleRoot(merkleRoot).creationTimestamp(creationTime)
                    .nBits(difficultyTarget).nonce(nonce).transactionCount(txCount).hasCoinbaseData(hasCoinbaseData)
                    .noMoreHeaders(noMoreHeaders).coinbaseTX(tx).coinbase(coinbase).coinbaseMerkleProof(hashes).build();
        }  else {
            blockHeaderEnMsg = BlockHeaderEnMsg.builder()
                    .hash(hash)
                    .version(version)
                    .prevBlockHash(prevBlockHash).merkleRoot(merkleRoot).creationTimestamp(creationTime)
                    .nBits(difficultyTarget).nonce(nonce).transactionCount(txCount).hasCoinbaseData(hasCoinbaseData)
                    .noMoreHeaders(noMoreHeaders).build();
        }

        return blockHeaderEnMsg;
    }


    @Override
    public void serialize(SerializerContext context, BlockHeaderEnMsg message, ByteArrayWriter byteWriter) {
        byteWriter.writeUint32LE(message.getVersion());
        byteWriter.write(getBytesHash(message.getPrevBlockHash()));
        byteWriter.write(getBytesHash(message.getMerkleRoot()));
        byteWriter.writeUint32LE(message.getCreationTimestamp());
        byteWriter.writeUint32LE(message.getNBits());
        byteWriter.writeUint32LE(message.getNonce());

        // We write the "nTx" field. Long 8 Bytes
        byteWriter.writeUint64LE(message.getTransactionCount());

        byteWriter.writeBoolean(message.isHasCoinbaseData());
        byteWriter.writeBoolean(message.isNoMoreHeaders());

        // We are not using the HashMsgSerializer for serialize as
        // We have to flip it around, as it's been read off the wire in little endian.
        List<HashMsg> hashes = message.getCoinbaseMerkleProof();
        for(HashMsg merkleProofHash:hashes) {
            byteWriter.write(getBytesHash(merkleProofHash));
        }

        VarStrMsgSerializer.getinstance().serialize(context,message.getCoinbase(), byteWriter);
    }

    private byte[] getBytesHash(HashMsg hashMsg) {
        return Sha256Wrapper.wrapReversed(hashMsg.getHashBytes()).getBytes();
    }
}