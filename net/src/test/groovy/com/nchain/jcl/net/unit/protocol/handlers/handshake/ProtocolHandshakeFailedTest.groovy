package com.nchain.jcl.net.unit.protocol.handlers.handshake

import com.nchain.jcl.net.protocol.config.ProtocolConfig
import com.nchain.jcl.net.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.net.protocol.events.PeerHandshakeRejectedEvent
import com.nchain.jcl.net.protocol.handlers.blacklist.BlacklistHandler
import com.nchain.jcl.net.protocol.handlers.discovery.DiscoveryHandler
import com.nchain.jcl.net.protocol.handlers.handshake.HandshakeHandlerConfig
import com.nchain.jcl.net.protocol.handlers.pingPong.PingPongHandler
import com.nchain.jcl.net.unit.protocol.tools.MsgTest
import com.nchain.jcl.net.protocol.wrapper.P2P
import com.nchain.jcl.net.protocol.wrapper.P2PBuilder
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Testing class for the "Bad" Scenarios, where the Handshake is rejected for some reason
 */
class ProtocolHandshakeFailedTest extends Specification {

    /**
     * We test that the Handshake fails if the clients uses a wrong "version" number
     */
    def "Failed Handshaked-Wrong Version"() {
        given:

            // Server Definition:
            // We disable all the Handlers we don't need for this Test:
            P2P server = new P2PBuilder("server")
                    .randomPort()
                    .excludeHandler(PingPongHandler.HANDLER_ID)
                    .excludeHandler(DiscoveryHandler.HANDLER_ID)
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .build()
            // Client Definition:
            // We change the "version" number, to force it to use an incorrect one:
            ProtocolConfig clientConfig = new ProtocolBSVMainConfig().toBuilder()
                .port(0).protocolVersion(0).build()

            // We disable all the Handlers we don't need for this Test:
            P2P client = new P2PBuilder("client")
                    .config(clientConfig)
                    .excludeHandler(PingPongHandler.HANDLER_ID)
                    .excludeHandler(DiscoveryHandler.HANDLER_ID)
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .build()

            // we Define the Listener for the events and keep track of them:
            AtomicBoolean serverHandshaked = new AtomicBoolean()
            AtomicBoolean clientHandshaked = new AtomicBoolean()
            AtomicReference<PeerHandshakeRejectedEvent> clientRejectedEvent   = new AtomicReference<>()

            server.EVENTS.PEERS.HANDSHAKED.forEach({ e -> serverHandshaked.set(true)})
            server.EVENTS.PEERS.HANDSHAKED_REJECTED.forEach({ e -> clientRejectedEvent.set(e)})
            client.EVENTS.PEERS.HANDSHAKED.forEach({ e -> clientHandshaked.set(true)})

        when:
            server.startServer()
            client.start()
            Thread.sleep(100)
            client.REQUESTS.PEERS.connect(server.getPeerAddress()).submit()
            Thread.sleep(1000)
            server.stop()
            client.stop()
        then:
            // We check that each there has been no handshake
            !serverHandshaked.get()
            !clientHandshaked.get()
            // we check that the Server has rejected the handshake proposed by the client, with the right reason
            clientRejectedEvent.get() != null
            clientRejectedEvent.get().reason == PeerHandshakeRejectedEvent.HandshakedRejectedReason.WRONG_VERSION
    }

    /**
     * We test that the HAndshake is rejected when a banned "user_agent" has been used
     */
    def "Failed Handshaked-Wrong UserAgent"() {
        given:

            // Server Definition:
            // We disable all the Handlers we don't need for this Test:
            P2P server = new P2PBuilder("server")
                    .randomPort()
                    .excludeHandler(PingPongHandler.HANDLER_ID)
                    .excludeHandler(DiscoveryHandler.HANDLER_ID)
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .build()
            // Client Definition:
            // We change the "User Agent" used by the Client, to use an incorrect one (in the protocolBSVMainConfig
            // class, any user_agent containing "ABC" and some other patterns are blacklisted)
            ProtocolConfig clientConfig = new ProtocolBSVMainConfig().toBuilder().port(0).build()

            HandshakeHandlerConfig handshakeConfig = clientConfig.getHandshakeConfig().toBuilder()
                .userAgent("ABC")
                .build()
            // We disable all the Handlers we don't need for this Test:
            P2P client = new P2PBuilder("client")
                    .config(clientConfig)
                    .config(handshakeConfig)
                    .excludeHandler(PingPongHandler.HANDLER_ID)
                    .excludeHandler(DiscoveryHandler.HANDLER_ID)
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .build()

            // we Define the Listener for the events and keep track of them:
            AtomicBoolean serverHandshaked = new AtomicBoolean()
            AtomicBoolean clientHandshaked = new AtomicBoolean()
            AtomicReference<PeerHandshakeRejectedEvent> clientRejectedEvent   = new AtomicReference<>()

            server.EVENTS.PEERS.HANDSHAKED.forEach({ e -> serverHandshaked.set(true)})
            server.EVENTS.PEERS.HANDSHAKED_REJECTED.forEach({ e -> clientRejectedEvent.set(e)})
            client.EVENTS.PEERS.HANDSHAKED.forEach({ e -> clientHandshaked.set(true)})

        when:
            server.startServer()
            client.start()
            Thread.sleep(100)
            client.REQUESTS.PEERS.connect(server.getPeerAddress()).submit()
            Thread.sleep(1000)
            server.stop()
            client.stop()

        then:
            // We check that each there has been no handshake
            !serverHandshaked.get()
            !clientHandshaked.get()
            // we check that the Server has rejected the handshake proposed by the client, with the right reason
            clientRejectedEvent.get() != null
            clientRejectedEvent.get().reason == PeerHandshakeRejectedEvent.HandshakedRejectedReason.WRONG_USER_AGENT
    }

    /**
     * We test that the Handshake is rejected when an extra message is sent (like an extra VersionAckMsg, in this
     * case). NOTE: In this particular scenario, the Handshake is properly stablished, and THEN it's rejected (when
     * the extra VersionAckMsg is sent).
     */
    def "Failed Handshaked-Duplicated ACK"() {
        given:
            // Server and Client Definition:
            // We disable all the Handlers we don't need for this Test:
            P2P server = new P2PBuilder("server")
                    .randomPort()
                    .excludeHandler(PingPongHandler.HANDLER_ID)
                    .excludeHandler(DiscoveryHandler.HANDLER_ID)
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .build()
            // We disable all the Handlers we don't need for this Test:
            P2P client = new P2PBuilder("client")
                    .randomPort()
                    .excludeHandler(PingPongHandler.HANDLER_ID)
                    .excludeHandler(DiscoveryHandler.HANDLER_ID)
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .build()

            // we Define the Listener for the events and keep track of them:
            AtomicBoolean serverHandshaked = new AtomicBoolean()
            AtomicBoolean clientHandshaked = new AtomicBoolean()
            AtomicReference<PeerHandshakeRejectedEvent> clientRejectedEvent   = new AtomicReference<>()
            server.EVENTS.PEERS.HANDSHAKED.forEach({ e -> serverHandshaked.set(true)})
            server.EVENTS.PEERS.HANDSHAKED_REJECTED.forEach({ e -> clientRejectedEvent.set(e)})
            client.EVENTS.PEERS.HANDSHAKED.forEach({ e -> clientHandshaked.set(true)})

        when:
            server.startServer()
            client.start()
            Thread.sleep(100)
            client.REQUESTS.PEERS.connect(server.getPeerAddress()).submit()
            Thread.sleep(1000)
            // At his moment, the handshake must have been stablished.
            // Now we send and additional VersionAck Msg, which will cause the handshake to be rejected
            client.REQUESTS.MSGS.send(server.getPeerAddress(), MsgTest.getVersionAckMsg()).submit()
            Thread.sleep(100)
            server.stop()
            client.stop()

        then:
            // We check that each on of them (Server and client) have received and triggered a Handshake)
            serverHandshaked.get()
            clientHandshaked.get()
            clientRejectedEvent.get() != null
            clientRejectedEvent.get().reason == PeerHandshakeRejectedEvent.HandshakedRejectedReason.PROTOCOL_MSG_DUPLICATE
    }
}