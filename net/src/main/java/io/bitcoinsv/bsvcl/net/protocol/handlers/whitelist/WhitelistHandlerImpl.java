package io.bitcoinsv.bsvcl.net.protocol.handlers.whitelist;

import io.bitcoinsv.bsvcl.net.network.events.*;
import io.bitcoinsv.bsvcl.net.tools.LoggerUtil;
import io.bitcoinsv.bsvcl.common.config.RuntimeConfig;
import io.bitcoinsv.bsvcl.common.handlers.HandlerConfig;
import io.bitcoinsv.bsvcl.common.handlers.HandlerImpl;
import io.bitcoinsv.bsvcl.common.util.StringUtils;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 16/06/2022
 */
public class WhitelistHandlerImpl extends HandlerImpl<InetAddress, WhitelistHostInfo> implements WhitelistHandler {

    // Suffix of the File that stores Peers from the Pool:
    private static final String NET_FOLDER = "net";
    private static final String FILE_WHITELIST_SUFFIX = "-whitelist-handler.csv";

    private LoggerUtil logger;
    private WhitelistHandlerConfig config;

    /** Constructor */
    public WhitelistHandlerImpl(String id, RuntimeConfig runtimeConfig, WhitelistHandlerConfig config) {
        super(id, runtimeConfig);
        this.config = config;
        this.logger = new LoggerUtil(id, HANDLER_ID, this.getClass());
    }

    public void registerForEvents() {
        subscribe(NetStartEvent.class, this::onNetStart);
        subscribe(NetStopEvent.class, this::onNetStop);
        subscribe(WhitelistPeerRequest.class, this::onWhitelistPeerRequest);
        subscribe(RemovePeerFromWhitelistRequest.class, this::onRemovePeerFromWhitelist);
        subscribe(ClearWhitelistRequest.class, this::onClearQuitelistRequest);
    }

    // Event Handler:
    public void onNetStart(NetStartEvent e) {
        logger.trace("Starting...");
    }

    // Event Handler:
    public void onNetStop(NetStopEvent e) {
        saveWhitelistToDisk();
    }

    // Event Handler:
    public void onWhitelistPeerRequest(WhitelistPeerRequest event) {
        whitelist(event.getAddress());
    }

    // Event Handler:
    public void onRemovePeerFromWhitelist(RemovePeerFromWhitelistRequest event) {
        removeFromWhitelist(List.of(event.getAddress()));
    }

    // Event Handler:
    public void onClearQuitelistRequest(ClearWhitelistRequest event) {
        clearWhitelist();
    }

    // It whitelists the host given
    private synchronized void whitelist(InetAddress address) {
        if (!super.handlerInfo.containsKey(address)) {
            super.handlerInfo.put(address, new WhitelistHostInfo(address));
            super.eventBus.publish(new PeersWhitelistedEvent(address));
            logger.trace(address, "Whitelisted.");
            super.eventBus.publish(new RemovePeerFromBlacklistRequest(address));
        }
    }

    // It removes the host given from the whitelist
    private synchronized void removeFromWhitelist(List<InetAddress> address) {
        logger.trace("Removing from Whitelist {} Ips...", address.size());
        // We only remove those that are actually whitelisted:
        List<InetAddress> peersToRemove = address.stream().filter(addr -> handlerInfo.containsKey(addr)).collect(Collectors.toList());
        if (!peersToRemove.isEmpty()) {
            peersToRemove.forEach(addr -> super.handlerInfo.remove(addr));
            super.eventBus.publish(new PeersRemovedFromWhitelistEvent(peersToRemove));
            logger.trace("{} Removed from Whitelist", peersToRemove.size());
        }
    }

    // It clears the Whitelist
    private synchronized void clearWhitelist() {
        logger.trace("Clearing Whitelist...");
        var peersToRemove = handlerInfo.values().stream().map(h -> h.getIp()).collect(Collectors.toList());
        removeFromWhitelist(peersToRemove);
    }

    @Override
    public void init() {
        registerForEvents();

        // We load the list of blacklist hosts form disk
        loadWhitelistFromDisk();

        // We trigger an Event for each Peer to remove it from the blacklist if its in there:
        handlerInfo.values().forEach(v -> super.eventBus.publish(new RemovePeerFromBlacklistRequest(v.getIp())));

        // We trigger an Event to notify that these Hosts have been whitelisted:
        Set<InetAddress> hostsToWhitelist = handlerInfo.values().stream().map(v -> v.getIp()).collect(Collectors.toSet());
        super.eventBus.publish(new PeersWhitelistedEvent(hostsToWhitelist));

    }

    @Override
    public WhitelistHandlerConfig getConfig() { return this.config;}

    @Override
    public synchronized void updateConfig(HandlerConfig config) {
        if (!(config instanceof WhitelistHandlerConfig)) {
            throw new RuntimeException("config class is NOT correct for this Handler");
        }
        this.config = (WhitelistHandlerConfig) config;
    }

    private void loadWhitelistFromDisk() {
        String csvFileName = StringUtils.fileNamingFriendly(config.getBasicConfig().getId()) + FILE_WHITELIST_SUFFIX;
        Path csvPath = Paths.get(runtimeConfig.getFileUtils().getRootPath().toString(), NET_FOLDER, csvFileName);
        if (Files.exists(csvPath)) {
            List<WhitelistHostInfo> hosts = runtimeConfig.getFileUtils().readCV(csvPath, () -> new WhitelistHostInfo());
            hosts.forEach(h -> handlerInfo.put(h.getIp(), h));
        }
    }

    private void saveWhitelistToDisk() {
        String csvFileName = StringUtils.fileNamingFriendly(config.getBasicConfig().getId()) + FILE_WHITELIST_SUFFIX;
        Path csvPath = Paths.get(runtimeConfig.getFileUtils().getRootPath().toString(), NET_FOLDER, csvFileName);
        List<WhitelistHostInfo> hostsToSave = handlerInfo.values().stream().collect(Collectors.toList());
        runtimeConfig.getFileUtils().writeCSV(csvPath, hostsToSave);
    }


    @Override
    public synchronized WhitelistHandlerState getState() {
        WhitelistHandlerState state = WhitelistHandlerState.builder()
                .whitelistedHosts(handlerInfo.keySet().stream().collect(Collectors.toSet()))
                .build();
        return state;
    }

    @Override
    public Set<WhitelistView> getWhitelistedHosts() {
        return handlerInfo.values().stream().map(v -> WhitelistView.from(v)).collect(Collectors.toSet());
    }
}