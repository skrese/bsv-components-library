package com.nchain.jcl.net.protocol.serialization;


import com.nchain.jcl.base.tools.bytes.ByteArrayReader;
import com.nchain.jcl.base.tools.bytes.ByteArrayWriter;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;
import com.nchain.jcl.net.protocol.messages.PongMsg;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2009-2010 Satoshi Nakamoto
 * Copyright (c) 2009-2016 The Bitcoin Core developers
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 30/07/2019 11:55
 */
public class PongMsgSerializer implements MessageSerializer<PongMsg> {

    private static PongMsgSerializer instance;

    private PongMsgSerializer() { }

    /** Returns the instance of this Serializer (Singleton) */
    public static PongMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (PongMsgSerializer.class) {
                instance = new PongMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public PongMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        byteReader.waitForBytes(8);
        long nonce = byteReader.readInt64LE();
        return  PongMsg.builder().nonce(nonce).build();
    }

    @Override
    public void serialize(SerializerContext context, PongMsg message, ByteArrayWriter byteWriter) {
        byteWriter.writeUint64LE(message.getNonce());

    }
}