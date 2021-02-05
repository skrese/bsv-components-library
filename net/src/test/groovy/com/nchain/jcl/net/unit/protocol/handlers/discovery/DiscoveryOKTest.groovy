package com.nchain.jcl.net.unit.protocol.handlers.discovery

import com.nchain.jcl.net.protocol.config.ProtocolConfig
import com.nchain.jcl.net.protocol.config.ProtocolConfigBuilder
import com.nchain.jcl.net.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.net.protocol.events.InitialPeersLoadedEvent
import com.nchain.jcl.net.protocol.handlers.blacklist.BlacklistHandler
import com.nchain.jcl.net.protocol.handlers.discovery.DiscoveryHandlerConfig
import com.nchain.jcl.net.protocol.wrapper.P2P
import com.nchain.jcl.net.protocol.wrapper.P2PBuilder
import com.nchain.jcl.tools.config.RuntimeConfig
import com.nchain.jcl.tools.config.provided.RuntimeConfigDefault
import io.bitcoinj.params.MainNetParams
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicReference

/**
 * Testing Class for the Discovery Handler (Happy path/scenarios)
 */
class DiscoveryOKTest extends Specification {

    /**
     * We check that on startup, the Discovery handler successfully loads the initial set of Peers from the CSV
     * fie, located in the working folder.
     * NOTE: This tests assumes that there is a file named "BSV[mainNet]-discovery-handler-seed.csv" in the
     * test/resources/jcl folder.
     */
    def "Testing Initial Peers loaded from CSV"() {
        given:
            // We set up the Configuration to use the Peers hardcoded in a CSV file:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams())

            // Since the initial peers are in a CSV in our classpath, we need to use a "RuntimeConfig" specifying a
            // classpath, so we instead of using the Default, we use an specific one
            RuntimeConfig runtimeConfig = new RuntimeConfigDefault(this.getClass().getClassLoader());

            DiscoveryHandlerConfig discoveryConfig = config.getDiscoveryConfig()
                    .toBuilder()
                    .discoveryMethod(DiscoveryHandlerConfig.DiscoveryMethod.PEERS)
                    .build()

            P2P server = new P2PBuilder("testing")
                    .config(runtimeConfig)
                    .config(config)
                    .config(discoveryConfig)
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .build()

            // We store the Event triggered when the initial Peers are loaded:
            AtomicReference<InitialPeersLoadedEvent> event = new AtomicReference<>()
            server.EVENTS.PEERS.INITIAL_PEERS_LOADED.forEach({ e -> event.set(e)})

        when:
            println("Starting P2P ...")
            server.startServer()
            Thread.sleep(500)
            println("P2P Stopping...")
            server.stop()
            println("P2P Stopped.")
        then:
            event.get() != null
            event.get().numPeersLoaded > 0

    }

    /**
     * We check that on startup, the Discovery Handler successfully loads the initial set of Peers from th DNS List
     */
    def "Testing Initial Peers loaded from DNS"() {
        given:
            // We set up the Configuration to use the Peers obtained by looking up oin the hardcoded DNS
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams())

            DiscoveryHandlerConfig discoveryConfig = config.getDiscoveryConfig().toBuilder()
                .discoveryMethod(DiscoveryHandlerConfig.DiscoveryMethod.DNS)
                .build()
            P2P server = new P2PBuilder("testing")
                    .config(config)
                    .config(discoveryConfig)
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .build()

            // We store the Event triggered when the initial Peers are loaded:
            AtomicReference<InitialPeersLoadedEvent> event = new AtomicReference<>()
            server.EVENTS.PEERS.INITIAL_PEERS_LOADED.forEach({ e ->
                println(" >>> EVENT DETECTED: Initial Set of Peers loaded " + e.toString())
                event.set(e)
            })

        when:
            try {
                println("Starting P2P ...")
                server.startServer()
                Thread.sleep(10000)
                println("P2P Stopping...")
                server.stop()
                println("P2P Stopped.")
            } catch (Throwable e) {
                e.printStackTrace()
            }
        then:
            event.get() != null
            event.get().numPeersLoaded > 0
    }
}
