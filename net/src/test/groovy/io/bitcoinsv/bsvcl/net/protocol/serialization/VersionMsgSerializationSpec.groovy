package io.bitcoinsv.bsvcl.net.protocol.serialization

import io.bitcoinsv.bsvcl.net.network.PeerAddress
import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolBasicConfig
import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolVersion
import io.bitcoinsv.bsvcl.net.protocol.messages.NetAddressMsg
import io.bitcoinsv.bsvcl.net.protocol.messages.VarStrMsg
import io.bitcoinsv.bsvcl.net.protocol.messages.VersionMsg
import io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg
import io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsgBuilder
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializer
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext
import io.bitcoinsv.bsvcl.net.protocol.tools.ByteArrayArtificalStreamProducer
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayWriter
import io.bitcoinsv.bitcoinjsv.core.Utils
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bitcoinjsv.params.Net
import spock.lang.Specification

/**
 * Testing class for the VersionMsg Message Serialization.
 * The test is taken the assumption that we have already a correct serialization version of this Message, obtained
 * from another source that we trust (in this case the Java BitcoinJ library). So we serialize/deserialize some
 * messages with out code and compare the results with that reference.
 *
 * NOTE: The Reference HEX Strings used have been generated using 70013 a the protocol Version
 */
class VersionMsgSerializationSpec extends Specification {

    // These variables  have been used to generate the serialized version of the BODY of a Version Message in
    // BitcoinJ, and its used here as a reference to compare our own Serializing implementation.
    // The following Serialized value for an Address has been produced using:
    // - Main Network
    // - Timestamp: 1563373910
    // - addr_from: localhost
    // - addr_recv: localhost
    // - startBestHeight: 50
    // - user_Agent: /bitcoinj-sv:0.9.0/
    // - discoveryRelay: true

    public static final String REF_BODY_ADDRESS_MSG = "7d110100000000000000000058752f5d00000000000000000000000000000000000000000000ffff7f000001208d000000000000000000000000000000000000ffff7f000001208d0000000000000000132f626974636f696e6a2d73763a302e392e302f3200000001"
    public static long REF_BODY_TIMESTAMP = 1563391320
    public static final long REF_BODY_START_HEIGHT = 50
    public static final String REF_BODY_USER_AGENT = "/bitcoinj-sv:0.9.0/"
    public static final boolean REF_BODY_RELAY = true
    public static final int REF_BODY_PORT = 8333
    public static final PeerAddress REF_BODY_ADDRESS = new PeerAddress(InetAddress.getByName("localhost"), REF_BODY_PORT)

    // These variables have been used to generate the serialized version of a FULL Version Message, including both
    // Header and Body. Some of the values inside this message are the same as the ones used in the Body above, so
    // some are reused. Here we only define the new ones used for the Complete Message.

    private static final String REF_ADDRESS_MSG = "e3e1f3e876657273696f6e000000000069000000cea667607d1101000000000000000000532e305d00000000000000000000000000000000000000000000ffff7f000001208d000000000000000000000000000000000000ffff7f000001208d0000000000000000132f626974636f696e6a2d73763a302e392e302f3200000001"
    private static final long REF_TIMESTAMP = 1563438675


    def "testing VersionMSg BODY De-serializing"(int byteInterval, int delayMs) {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            ProtocolBasicConfig basicConfig = config.getBasicConfig().toBuilder()
                .protocolVersion(ProtocolVersion.ENABLE_FEE_FILTER.getVersion())
                .build()
            DeserializerContext context = DeserializerContext.builder()
                    .protocolBasicConfig(basicConfig)
                    .maxBytesToRead(Utils.HEX.decode(REF_BODY_ADDRESS_MSG).length)
                    .insideVersionMsg(true)
                    .build()
            VersionMsg message = null
            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(Utils.HEX.decode(REF_BODY_ADDRESS_MSG), byteInterval, delayMs);

        when:
            message = VersionMsgSerializer.getInstance().deserialize(context, byteReader)
        then:
            message.getStart_height() == REF_BODY_START_HEIGHT
            message.getUser_agent().getStr().equals(REF_BODY_USER_AGENT)
            message.getTimestamp() == REF_BODY_TIMESTAMP
            message.getAddr_from().getAddress().getIp().getCanonicalHostName().equals(REF_BODY_ADDRESS.getIp().getCanonicalHostName())
            message.getAddr_recv().getAddress().getIp().getCanonicalHostName().equals(REF_BODY_ADDRESS.getIp().getCanonicalHostName())
            message.getRelay() == REF_BODY_RELAY
        where:
            byteInterval | delayMs
                10       |    15
    }

    def "testing VersionMsg BODY Serializing"() {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            ProtocolBasicConfig basicConfig = config.getBasicConfig().toBuilder()
                .protocolVersion(ProtocolVersion.ENABLE_FEE_FILTER.getVersion())
                .build()
            SerializerContext context = SerializerContext.builder()
                    .protocolBasicConfig(basicConfig)
                    .insideVersionMsg(true)
                    .build()
            VarStrMsg userAgentMsg = VarStrMsg.builder().str(REF_BODY_USER_AGENT).build();
            NetAddressMsg body_addr = NetAddressMsg.builder()
                .address(REF_BODY_ADDRESS)
                .timestamp(System.currentTimeMillis())
                .build();

            VersionMsg message = VersionMsg.builder()
                    .version(basicConfig.getProtocolVersion())
                    .timestamp(REF_BODY_TIMESTAMP)
                    .user_agent(userAgentMsg)
                    .start_height(REF_BODY_START_HEIGHT)
                    .addr_from(body_addr)
                    .addr_recv(body_addr)
                    .relay(true)
                    .build()
            ByteArrayWriter byteWriter = new ByteArrayWriter()
            String messageSerialized = null
        when:
            VersionMsgSerializer.getInstance().serialize(context, message, byteWriter)
            byte[] messageBytes = byteWriter.reader().getFullContent()
            messageSerialized = Utils.HEX.encode(messageBytes)
        then:
            messageSerialized.equals(REF_BODY_ADDRESS_MSG)
    }

    def "testing Version Message COMPLETE de-serializing"(int byteInterval, int delayMs) {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            ProtocolBasicConfig basicConfig = config.getBasicConfig().toBuilder()
                .protocolVersion(ProtocolVersion.ENABLE_FEE_FILTER.getVersion())
                .build()
            DeserializerContext context = DeserializerContext.builder()
                .protocolBasicConfig(basicConfig)
                .maxBytesToRead(Utils.HEX.decode(REF_ADDRESS_MSG).length)
                .insideVersionMsg(true)
                .calculateChecksum(true)
                .build()

            ByteArrayReader byteReader = ByteArrayArtificalStreamProducer.stream(Utils.HEX.decode(REF_ADDRESS_MSG), byteInterval, delayMs);
            BitcoinMsgSerializer bitcoinSerializer = new BitcoinMsgSerializerImpl()
        when:
            BitcoinMsg<VersionMsg> message = bitcoinSerializer.deserialize(context, byteReader)
        then:
            message.getHeader().getMagic().equals(config.getBasicConfig().getMagicPackage())
            message.getHeader().getCommand().toUpperCase().equals(VersionMsg.MESSAGE_TYPE.toUpperCase())
            message.getBody().getStart_height() == REF_BODY_START_HEIGHT
            message.getBody().getUser_agent().getStr().equals(REF_BODY_USER_AGENT)
            message.getBody().getTimestamp() == REF_TIMESTAMP
            message.getBody().getAddr_from().getAddress().getIp().getCanonicalHostName().equals(REF_BODY_ADDRESS.getIp().getCanonicalHostName())
            message.getBody().getAddr_recv().getAddress().getIp().getCanonicalHostName().equals(REF_BODY_ADDRESS.getIp().getCanonicalHostName())
            message.getBody().getRelay() == REF_BODY_RELAY
        where:
            byteInterval | delayMs
                10       |    25
    }

    def "testing Version Message COMPLETE Serializing"() {
        given:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            ProtocolBasicConfig basicConfig = config.getBasicConfig().toBuilder()
                .protocolVersion(ProtocolVersion.ENABLE_FEE_FILTER.getVersion())
                .build()
            SerializerContext context = SerializerContext.builder()
                    .protocolBasicConfig(basicConfig)
                    .insideVersionMsg(true)
                    .build()
            VarStrMsg userAgentMsg = VarStrMsg.builder().str(REF_BODY_USER_AGENT).build();
            NetAddressMsg body_addr = NetAddressMsg.builder()
                .address(REF_BODY_ADDRESS)
                .timestamp(System.currentTimeMillis())
                .build();
            VersionMsg versionMsg  = VersionMsg.builder()
                    .version(basicConfig.protocolVersion)
                    .timestamp(REF_TIMESTAMP)
                    .user_agent(userAgentMsg)
                    .start_height(REF_BODY_START_HEIGHT)
                    .addr_from(body_addr)
                    .addr_recv(body_addr)
                    .relay(true).build()
            BitcoinMsg<VersionMsg> bitcoinVersionMsg = new BitcoinMsgBuilder<>(config.getBasicConfig(), versionMsg).build()
            BitcoinMsgSerializer bitcoinSerializer = new BitcoinMsgSerializerImpl()
        when:
            byte[] versionMsgBytes = bitcoinSerializer.serialize(context, bitcoinVersionMsg).getFullContent()
            String versionMsgDeserialzed = Utils.HEX.encode(versionMsgBytes)
        then:
            versionMsgDeserialzed.equals(REF_ADDRESS_MSG)
    }

    /**
     * The "start_height" field contains the Height we are at in the chain. But the field is defined as a SIGNED Int,
     * so it could hold a negative value, which is used sometimes during TESTING, when we are just "listening" and we
     * do not want to advertise our height. If we put a negative value there, it should be read as a Negative value,
     * it should NOT be converted to a positive value.
     */
    def "testing -1 as start_height"() {
        given:
            // We build a VERSION MSg manually, using a -1 as "start_height"
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            ProtocolBasicConfig basicConfig = config.getBasicConfig().toBuilder()
                    .protocolVersion(ProtocolVersion.ENABLE_FEE_FILTER.getVersion())
                    .build()
            NetAddressMsg body_addr = NetAddressMsg.builder()
                    .address(REF_BODY_ADDRESS)
                    .timestamp(System.currentTimeMillis())
                    .build();
            VersionMsg versionMsg  = VersionMsg.builder()
                    .version(basicConfig.protocolVersion)
                    .timestamp(REF_TIMESTAMP)
                    .user_agent(VarStrMsg.builder().str("testing").build())
                    .start_height(-1) // NEGATIVE VALUE!!!
                    .addr_from(body_addr)
                    .addr_recv(body_addr)
                    .relay(true).build()
        when:
            // We Serialize the Msg, then we Deserialize and build a Message back Both Messages (original and
            // the deserialized one) should match and the negative value in "start_height" should be retrieved
            // properly
            SerializerContext serContext = SerializerContext.builder()
                .protocolBasicConfig(basicConfig)
                .insideVersionMsg(true)
                .build()
            ByteArrayWriter byteWriter = new ByteArrayWriter()
            VersionMsgSerializer.getInstance().serialize(serContext, versionMsg, byteWriter)
            byte[] messageBytes = byteWriter.reader().getFullContent()

            DeserializerContext desContext = DeserializerContext.builder()
                .protocolBasicConfig(basicConfig)
                .maxBytesToRead(Utils.HEX.decode(REF_BODY_ADDRESS_MSG).length)
                .insideVersionMsg(true)
                .build()
            ByteArrayReader byteReader = new ByteArrayReader(messageBytes)
            VersionMsg desVersionMsg = VersionMsgSerializer.getInstance().deserialize(desContext, byteReader)
        then:
            versionMsg.getStart_height() == desVersionMsg.getStart_height()
            desVersionMsg.getStart_height() == -1
    }
}
