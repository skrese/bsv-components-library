package io.bitcoinsv.bsvcl.net.protocol.serialization

import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializer
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext
import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.bsvcl.net.protocol.messages.VersionAckMsg
import io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg
import io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsgBuilder
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext
import io.bitcoinsv.bsvcl.net.protocol.tools.ByteArrayArtificalStreamProducer
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader
import io.bitcoinsv.bitcoinjsv.core.Utils
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bitcoinjsv.params.Net
import spock.lang.Specification

/**
 * Testing class for the VerAckMsg Serialization.
 * The test is taken the assumption that we have already a correct serialization version of this Message, obtained
 * from another source that we trust (in this case the Java BitcoinJ library). So we serialize/deserialize some
 * messages with out code and compare the results with that reference.
 */
class VerackSerializationSpec extends Specification {

    // This is a VERACK Message Serialized for the Main Network in HEX format, generated by a third party (bitcoinJ)
    public static final String VERACK_MSG = "e3e1f3e876657261636b000000000000000000005df6e0e2"

    def "Testing Verack Full Message Deserializing"(int byteInterval, int delayMs) {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            DeserializerContext context = DeserializerContext.builder()
                        .protocolBasicConfig(config.getBasicConfig())
                        .build()
            BitcoinMsgSerializer bitcoinSerializer = BitcoinMsgSerializerImpl.getInstance()
                BitcoinMsg<VersionAckMsg> version = null
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(Utils.HEX.decode(VERACK_MSG), byteInterval, delayMs)
        when:
            version = bitcoinSerializer.<VersionAckMsg>deserialize(context, byteReader)
        then:
            version.getHeader().getCommand().equals(VersionAckMsg.MESSAGE_TYPE)
            version.getHeader().getMagic() == config.getBasicConfig().getMagicPackage()
        where:
            byteInterval | delayMs
                10       |    15
    }

    def "Testing Verack Full Message Serialization"() {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            SerializerContext context = SerializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            BitcoinMsg<VersionAckMsg> versionAck = new BitcoinMsgBuilder<>(config.getBasicConfig(), VersionAckMsg.builder().build()).build()
            BitcoinMsgSerializer bitcoinSerializer = BitcoinMsgSerializerImpl.getInstance()
            String msgSerializedHex = null
        when:
            byte[] serializedMsg = bitcoinSerializer.serialize(context, versionAck).getFullContent()
            msgSerializedHex = Utils.HEX.encode(serializedMsg)
        then:
            msgSerializedHex.equals(VERACK_MSG)
    }
}