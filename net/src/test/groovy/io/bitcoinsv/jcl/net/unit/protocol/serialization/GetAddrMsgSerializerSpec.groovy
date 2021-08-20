/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.unit.protocol.serialization


import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader
import io.bitcoinsv.bitcoinjsv.core.Utils
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bitcoinjsv.params.Net
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.jcl.net.protocol.messages.GetAddrMsg
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsgBuilder
import io.bitcoinsv.jcl.net.protocol.serialization.common.BitcoinMsgSerializer
import io.bitcoinsv.jcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext
import io.bitcoinsv.jcl.net.unit.protocol.tools.ByteArrayArtificalStreamProducer
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
        ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
        DeserializerContext context = DeserializerContext.builder()
                        .protocolBasicConfig(config.getBasicConfig())
                        .build()
        BitcoinMsgSerializer bitcoinSerializer = BitcoinMsgSerializerImpl.getInstance()
        BitcoinMsg<GetAddrMsg> getAddrMsg = null
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(Utils.HEX.decode(GETADDR_MSG), byteInterval, delayMs);
        when:
            getAddrMsg = bitcoinSerializer.<GetAddrMsg>deserialize(context,
                    byteReader, GetAddrMsg.MESSAGE_TYPE)
        then:
            getAddrMsg.getHeader().getCommand().equals(GetAddrMsg.MESSAGE_TYPE)
            getAddrMsg.getHeader().getMagic() == config.getBasicConfig().getMagicPackage()
        where:
            byteInterval | delayMs
                10       |    15
    }

    def "Testing GetAddr Full Message Serialization"() {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
        SerializerContext context = SerializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            BitcoinMsg<GetAddrMsg> getAddrMsg = new BitcoinMsgBuilder<>(config.getBasicConfig(), GetAddrMsg.builder().build()).build()
            BitcoinMsgSerializer bitcoinSerializer = BitcoinMsgSerializerImpl.getInstance()
            String msgSerializedHex = null
        when:
            byte[] serializedMsg = bitcoinSerializer.serialize(context, getAddrMsg, GetAddrMsg.MESSAGE_TYPE).getFullContent()
            msgSerializedHex = Utils.HEX.encode(serializedMsg)
        then:
            msgSerializedHex.equals(GETADDR_MSG)
    }
}
