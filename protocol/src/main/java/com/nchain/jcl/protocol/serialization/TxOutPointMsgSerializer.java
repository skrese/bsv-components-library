package com.nchain.jcl.protocol.serialization;


import com.nchain.jcl.protocol.messages.HashMsg;
import com.nchain.jcl.protocol.messages.TxOutPointMsg;
import com.nchain.jcl.protocol.serialization.common.*;
import com.nchain.jcl.tools.bytes.ByteTools;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;
import com.nchain.jcl.tools.bytes.HEX;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 *
 * @date 24/09/2019
 *
 * A Serializer for instance of {@Link HashMsg} messages
 */
public class TxOutPointMsgSerializer implements MessageSerializer<TxOutPointMsg> {

    private static TxOutPointMsgSerializer instance;

    // Reference to singleton instances used during serialization/Deserialization. Defined here for performance
    private HashMsgSerializer hashMsgSerializer = HashMsgSerializer.getInstance();

    private TxOutPointMsgSerializer() { }

    public static TxOutPointMsgSerializer getInstance(){
        if(instance == null) {
            synchronized (TxOutPointMsgSerializer.class) {
                instance = new TxOutPointMsgSerializer();
            }
        }

        return instance;
    }

    @Override
    public TxOutPointMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        HashMsg hashMsg = hashMsgSerializer.deserialize(context, byteReader);
        //System.out.println("after hashMsg: " + HEX.encode(byteReader.getFullContent()));
        HashMsg reverseHash = HashMsg.builder().hash(reverseBytes(hashMsg)).build();
        long index = byteReader.readUint32();
        TxOutPointMsg txOutPointMsg = TxOutPointMsg.builder().hash(reverseHash).index(index).build();
        return txOutPointMsg;
    }

    @Override
    public void serialize(SerializerContext context, TxOutPointMsg message, ByteArrayWriter byteWriter) {
        byteWriter.write(reverseBytes(message.getHash()));
        byteWriter.writeUint32LE(message.getIndex());
    }

    private byte[] reverseBytes(HashMsg hashMsg) {
        return ByteTools.reverseBytes(hashMsg.getHashBytes());
    }

}
