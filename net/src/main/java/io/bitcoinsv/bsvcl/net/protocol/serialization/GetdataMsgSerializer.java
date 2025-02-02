package io.bitcoinsv.bsvcl.net.protocol.serialization;



import io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.bsvcl.net.protocol.messages.GetdataMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.InventoryVectorMsg;
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader;
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayWriter;

import java.util.List;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 *  A Serializer for {@link GetdataMsg} messages
 */
public class GetdataMsgSerializer  implements MessageSerializer<GetdataMsg> {

    private static GetdataMsgSerializer instance;

    private GetdataMsgSerializer() { }

    public static GetdataMsgSerializer getInstance() {
        if( instance == null) {
            synchronized (GetdataMsgSerializer.class) {
                instance = new GetdataMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public GetdataMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        InvMsgSerializer serializer = InvMsgSerializer.getInstance();
        List<InventoryVectorMsg> inventoryVectorMsgs = serializer.deserializeList(context, byteReader);

        //Builds both the count and inventory list from the messages
        GetdataMsg getdataMsg = GetdataMsg.builder().invVectorList(inventoryVectorMsgs).build();
        return getdataMsg;
    }

    @Override
    public void serialize(SerializerContext context, GetdataMsg message, ByteArrayWriter byteWriter) {
        VarIntMsgSerializer.getInstance().serialize(context, message.getCount(), byteWriter);
        InvMsgSerializer serializer = InvMsgSerializer.getInstance();
        serializer.serializeList(context, message.getInvVectorList(), byteWriter);
    }
}
