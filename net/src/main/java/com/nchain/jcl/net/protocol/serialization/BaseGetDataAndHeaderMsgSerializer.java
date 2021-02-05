package com.nchain.jcl.net.protocol.serialization;



import com.nchain.jcl.net.protocol.messages.BaseGetDataAndHeaderMsg;
import com.nchain.jcl.net.protocol.messages.HashMsg;
import com.nchain.jcl.net.protocol.messages.VarIntMsg;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;
import io.bitcoinj.core.Sha256Hash;
import io.bitcoinj.core.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Serializer for {@link BaseGetDataAndHeaderMsg} messages
 */
public class BaseGetDataAndHeaderMsgSerializer implements MessageSerializer<BaseGetDataAndHeaderMsg> {
    private static BaseGetDataAndHeaderMsgSerializer instance;

    private BaseGetDataAndHeaderMsgSerializer() {}

    public static BaseGetDataAndHeaderMsgSerializer getInstance() {
        if(instance == null) {
            synchronized (BaseGetDataAndHeaderMsgSerializer.class){
                instance = new BaseGetDataAndHeaderMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public BaseGetDataAndHeaderMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        byteReader.waitForBytes(4);
        long version = byteReader.readUint32();
        VarIntMsg hashCount = VarIntMsgSerializer.getInstance().deserialize(context,byteReader);

        List locaterhashes = new ArrayList<byte[]>();
        for(int i=0; i < hashCount.getValue() ; i++ ){
            locaterhashes.add(readHashMsg(context, byteReader));
        }
        HashMsg stopHash = readHashMsg(context, byteReader);

        BaseGetDataAndHeaderMsg  baseGetDataAndHeaderMsg =  BaseGetDataAndHeaderMsg.builder()
                .version(version)
                .hashCount(hashCount)
                .blockLocatorHash(locaterhashes)
                .hashStop(stopHash)
                .build();

        return baseGetDataAndHeaderMsg;
    }

    @Override
    public void serialize(SerializerContext context, BaseGetDataAndHeaderMsg message, ByteArrayWriter byteWriter) {
        byteWriter.writeUint32LE(message.getVersion());
        VarIntMsgSerializer.getInstance().serialize(context, message.getHashCount(), byteWriter);

        // We are not using the HashMsgSerializer for serialize as
        // We have to flip it around, as it's been read off the wire in little endian.
        List<HashMsg> locatorhashes = message.getBlockLocatorHash();
        for(HashMsg locatorHash:locatorhashes) {
            byteWriter.write(Utils.reverseBytes(locatorHash.getHashBytes()));
        }

        byteWriter.write(Utils.reverseBytes(message.getHashStop().getHashBytes()));

    }

    protected HashMsg readHashMsg(DeserializerContext context, ByteArrayReader byteReader)  {
        byteReader.waitForBytes(HashMsg.HASH_LENGTH);
        // We are not using the HashMsgSerializer for deserialize as
        // We have to flip it around, as it's been read off the wire in little endian.
        // Not the most efficient way to do this but the clearest.
        Sha256Hash byteWrapper =  Sha256Hash.wrapReversed(byteReader.read(HashMsg.HASH_LENGTH));
        HashMsg hashMsg =  HashMsg.builder().hash(byteWrapper.getBytes()).build();
        return  hashMsg;
    }
}
