package com.nchain.jcl.net.protocol.serialization;



import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;
import com.nchain.jcl.net.protocol.messages.PingMsg;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Serializer for {@link PingMsg} messages
 */
public class PingMsgSerializer implements MessageSerializer<PingMsg> {

    private static PingMsgSerializer instance;

    private PingMsgSerializer() { }

    /** Returns the instance of this Serializer (Singleton) */
    public static PingMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (PingMsgSerializer.class) {
                instance = new PingMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public PingMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        try {
            byteReader.waitForBytes(8);
            long nonce = byteReader.readInt64LE();
            return PingMsg.builder().nonce(nonce).build();
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public void serialize(SerializerContext context, PingMsg message, ByteArrayWriter byteWriter) {
        byteWriter.writeUint64LE(message.getNonce());

    }
}
