package io.bitcoinsv.bsvcl.net.protocol.serialization


import io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayWriter
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bitcoinjsv.params.Net
import io.bitcoinsv.bsvcl.net.protocol.tools.ByteArrayArtificalStreamProducer
import spock.lang.Specification

/**
 * @author m.jose@nchain.com
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 18/09/2019
 *
 *
 * Testing class for the Hash Message Serialization.
 * The test is taken the assumption that we have already a correct serialization version of this Message, obtained
 * from another source that we trust (in this case the Java BitcoinJ library). So we serialize/deserialize some
 * messages with out code and compare the results with that reference.
 */
class HashMsgSerializerSpec extends Specification {

    // Hash Content: a String containing a Test Message.
    // This is the bytes we are using as the content of the Hash.
    public static final byte[] REF_HASH_CONTENT_BYTES = "Testing Serialization of HashMsg".getBytes();

    // Serialized version of the Hash (generated by bitcoinJ)
    public static final byte[] REF_HASH_BYTES = Sha256Hash.wrap("54657374696e672053657269616c697a6174696f6e206f6620486173684d7367").getBytes();

    def "Testing HashMsg Deserializing"(int byteInterval, int delayMs) {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext context = io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            io.bitcoinsv.bsvcl.net.protocol.serialization.HashMsgSerializer serializer = io.bitcoinsv.bsvcl.net.protocol.serialization.HashMsgSerializer.getInstance()
            io.bitcoinsv.bsvcl.net.protocol.messages.HashMsg message
        ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(REF_HASH_BYTES, byteInterval, delayMs);
        when:

             message = serializer.deserialize(context, byteReader)
        then:
             message.getHashBytes() == REF_HASH_CONTENT_BYTES
             message.messageType == io.bitcoinsv.bsvcl.net.protocol.messages.HashMsg.MESSAGE_TYPE
        where:
            byteInterval | delayMs
                10       |    15
    }

    def "Testing HashMsg Serializing"() {
        given:
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext context = io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext.builder()
                    .protocolBasicConfig(config.getBasicConfig())
                    .build()
            io.bitcoinsv.bsvcl.net.protocol.messages.HashMsg message = io.bitcoinsv.bsvcl.net.protocol.messages.HashMsg.builder().hash(REF_HASH_CONTENT_BYTES).build()
            io.bitcoinsv.bsvcl.net.protocol.serialization.HashMsgSerializer serializer = io.bitcoinsv.bsvcl.net.protocol.serialization.HashMsgSerializer.getInstance()
            byte[] messageSerializedBytes
        when:
            ByteArrayWriter byteWriter = new ByteArrayWriter()
            serializer.serialize(context, message, byteWriter)
            messageSerializedBytes = byteWriter.reader().getFullContent()
        then:
            messageSerializedBytes == REF_HASH_BYTES
    }
}
