package com.nchain.jcl.net.protocol.serialization;


import com.nchain.jcl.net.protocol.messages.CompleteBlockHeaderMsg;
import com.nchain.jcl.net.protocol.messages.HeadersMsg;
import com.nchain.jcl.net.protocol.messages.VarIntMsg;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;

import java.util.ArrayList;
import java.util.List;


/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 *  A Serializer for {@link HeadersMsg} messages
 */
public class HeadersMsgSerializer implements MessageSerializer<HeadersMsg> {

    private static HeadersMsgSerializer instance;

    private HeadersMsgSerializer() { }

    /** Returns an instance of this Serializer (Singleton) */
    public static HeadersMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (HeadersMsgSerializer.class) {
                instance = new HeadersMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public HeadersMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        List<CompleteBlockHeaderMsg> blockHeaderMsgs = deserializeList(context, byteReader);
        HeadersMsg headersMsg = HeadersMsg.builder().blockHeaderMsgList(blockHeaderMsgs).build();

        return headersMsg;
    }

    /**
     * Deserialize blockHeadersMsg list
     *
     * @param context
     * @param byteReader
     * @return
     */
    protected List<CompleteBlockHeaderMsg> deserializeList(DeserializerContext context, ByteArrayReader byteReader) {
        VarIntMsg count = VarIntMsgSerializer.getInstance().deserialize(context, byteReader);
        CompleteBlockHeaderMsg blockHeaderMsg;
        List<CompleteBlockHeaderMsg> blockHeaderMsgs = new ArrayList<>();

        CompleteBlockHeaderMsgSerializer blockHeaderMsgSerializer = CompleteBlockHeaderMsgSerializer.getInstance();
        for(int i =0 ; i < count.getValue(); i++) {
            blockHeaderMsg = blockHeaderMsgSerializer.deserialize(context, byteReader);
            blockHeaderMsgs.add(blockHeaderMsg);
        }

        return blockHeaderMsgs;
    }

    @Override
    public void serialize(SerializerContext context, HeadersMsg message, ByteArrayWriter byteWriter) {
        VarIntMsgSerializer.getInstance().serialize(context, message.getCount(), byteWriter);
        List<CompleteBlockHeaderMsg> blockHeaderMsg = message.getBlockHeaderMsgList();
        serializeList(context, blockHeaderMsg , byteWriter);
    }

    /**
     * Serialize blockHeadersMsg List
     * @param context
     * @param blockHeaderMsgList
     * @param byteWriter
     */
    protected void serializeList(SerializerContext context, List<CompleteBlockHeaderMsg> blockHeaderMsgList, ByteArrayWriter byteWriter) {
        for (CompleteBlockHeaderMsg blockHeaderMsg:blockHeaderMsgList) {
            CompleteBlockHeaderMsgSerializer.getInstance().serialize(context, blockHeaderMsg, byteWriter);
        }
    }
}
