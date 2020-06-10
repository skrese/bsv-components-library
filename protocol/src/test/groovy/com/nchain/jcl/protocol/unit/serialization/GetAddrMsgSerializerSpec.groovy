package com.nchain.jcl.protocol.unit.serialization

import com.nchain.jcl.protocol.config.ProtocolConfig
import com.nchain.jcl.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.protocol.messages.GetAddrMsg
import com.nchain.jcl.protocol.messages.common.BitcoinMsg
import com.nchain.jcl.protocol.messages.common.BitcoinMsgBuilder
import com.nchain.jcl.protocol.serialization.common.*
import com.nchain.jcl.protocol.unit.tools.ByteArrayArtificalStreamProducer
import com.nchain.jcl.tools.bytes.HEX
import com.nchain.jcl.tools.bytes.ByteArrayReader
import spock.lang.Specification
/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 08/08/2019
 *
 * Testing class for the GetAddrMsgSerializerSpec Serialization.
 * The test is taken the assumption that we have already a correct serialization version of this Message, obtained
 * from another source that we trust (in this case the Java BitcoinJ library). So we serialize/deserialize some
 * messages with out code and compare the results with that reference.
 */
class GetAddrMsgSerializerSpec extends Specification {

    // This is a GetAddrMsg Message Serialized for the Main Network in HEX format, generated by a third party (bitcoinJ)
    public static final String GETADDR_MSG = "e3e1f3e8676574616464720000000000000000005df6e0e2"

    def "Testing GetAddr Full Message Deserializing"(int byteInterval, int delayMs) {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            DeserializerContext context = DeserializerContext.builder()
                    .protocolconfig(config).build()
            BitcoinMsgSerializer bitcoinSerializer = BitcoinMsgSerializerImpl.getInstance()
            BitcoinMsg<GetAddrMsg> getAddrMsg = null
        ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(HEX.decode(GETADDR_MSG), byteInterval, delayMs);
        when:
            getAddrMsg = bitcoinSerializer.<GetAddrMsg>deserialize(context,
                    byteReader, GetAddrMsg.MESSAGE_TYPE)
        then:
            getAddrMsg.getHeader().getCommand().equals(GetAddrMsg.MESSAGE_TYPE)
            getAddrMsg.getHeader().getMagic() == config.getMagicPackage()
        where:
            byteInterval | delayMs
                10       |    15
    }

    def "Testing GetAddr Full Message Serialization"() {
        given:
            ProtocolConfig config = new ProtocolBSVMainConfig()
            SerializerContext context = SerializerContext.builder()
                    .protocolconfig(config)
                    .build()
            BitcoinMsg<GetAddrMsg> getAddrMsg = new BitcoinMsgBuilder<>(config, GetAddrMsg.builder().build()).build()
            BitcoinMsgSerializer bitcoinSerializer = BitcoinMsgSerializerImpl.getInstance()
            String msgSerializedHex = null
        when:
            byte[] serializedMsg = bitcoinSerializer.serialize(context, getAddrMsg, GetAddrMsg.MESSAGE_TYPE).getFullContent()
            msgSerializedHex = HEX.encode(serializedMsg)
        then:
            msgSerializedHex.equals(GETADDR_MSG)
    }
}
