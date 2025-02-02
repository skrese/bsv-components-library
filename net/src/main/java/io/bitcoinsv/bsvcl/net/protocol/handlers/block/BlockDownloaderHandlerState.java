package io.bitcoinsv.bsvcl.net.protocol.handlers.block;


import io.bitcoinsv.bsvcl.net.network.PeerAddress;
import io.bitcoinsv.bsvcl.net.protocol.handlers.block.strategies.DownloadResponse;
import io.bitcoinsv.bsvcl.common.handlers.HandlerState;
import io.bitcoinsv.bsvcl.common.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * It stores the state of the blockDownloader Handler at a point in time.
 */
public final class BlockDownloaderHandlerState extends HandlerState {
    // Handler Config:
    private final BlockDownloaderHandlerConfig config;

    // Downloading State:
    private final BlockDownloaderHandlerImpl.DonwloadingState downloadingState;

    // List of blocks in different States:
    private final List<String> pendingBlocks;
    private final Set<String> downloadedBlocks;
    private final List<String> discardedBlocks;
    private final List<String> pendingToCancelBlocks;
    private final List<String> cancelledBlocks;
    private final Set<String> blocksInLimbo;

    // Blocks download History:
    private final Map<String, List<BlocksDownloadHistory.HistoricItem<String, PeerAddress>>> blocksHistory;

    // Download Events: A list of Notes/Events we might want to be logged
    private final List<String> downloadEvents;
    private static final int NUM_EVENTS_TO_LOG = 3; // we only log the last "X" Events

    // Complete info of all the Peers, but only those who are hanshaked at the moment (they might be idle or not)
    private final List<BlockPeerInfo> peersInfo;

    // Number of total download re-attemps since the beginning:
    private final long totalReattempts;

    // Map with the current attempts per block:
    private Map<String, Integer> blocksNumDownloadAttempts;

    // Track of last activity timestamp for each block:
    private Map<String, Instant> blocksLastActivity = new ConcurrentHashMap<>();

    // We keep track also of the list of Rejections obtained for each Peer that can NOT download any block at all
    private Map<PeerAddress, DownloadResponse> downloadRejections = new ConcurrentHashMap<>();

    // percentage of "busy": If 100, then the Block downloader is downloading all the possible blocks simultaneously,
    // based con configuration. If 50, only half of the blocks that could be are being downloaded, etc.
    // This parameter is NOT a snapshot, but more like an accumulative aggregation: Its the average between the
    // current value and the previous one...
    private final int busyPercentage;

    // If FALSE, then the download process is not allowed at the moment in order to prevent bandwith:
    private final boolean bandwidthRestricted;

    private final long blocksDownloadingSize;

    public BlockDownloaderHandlerState( BlockDownloaderHandlerConfig config,
                                        BlockDownloaderHandlerImpl.DonwloadingState downloadingState,
                                        List<String> pendingBlocks,
                                        Set<String> downloadedBlocks,
                                        List<String> discardedBlocks,
                                        List<String> pendingToCancelBlocks,
                                        List<String> cancelledBlocks,
                                        Set<String> blocksInLimbo,
                                        Map<String, List<BlocksDownloadHistory.HistoricItem<String, PeerAddress>>> blocksHistory,
                                        List<String> downloadEvents,
                                        Map<String, Instant> blocksLastActivity,
                                        Map<PeerAddress, DownloadResponse> downloadRejections,
                                        List<BlockPeerInfo> peersInfo,
                                        long totalReattempts,
                                        Map<String, Integer> blocksNumDownloadAttempts,
                                        int busyPercentage,
                                        boolean bandwidthRestricted,
                                        long blocksDownloadingSize) {
        this.config = config;
        this.downloadingState = downloadingState;
        this.pendingBlocks = pendingBlocks;
        this.downloadedBlocks = downloadedBlocks;
        this.discardedBlocks = discardedBlocks;
        this.pendingToCancelBlocks = pendingToCancelBlocks;
        this.cancelledBlocks = cancelledBlocks;
        this.blocksInLimbo = blocksInLimbo;
        this.blocksHistory = blocksHistory;
        this.downloadEvents = downloadEvents;
        this.blocksLastActivity = blocksLastActivity;
        this.downloadRejections = downloadRejections;
        this.peersInfo = peersInfo;
        this.totalReattempts = totalReattempts;
        this.blocksNumDownloadAttempts = blocksNumDownloadAttempts;
        this.busyPercentage = busyPercentage;
        this.bandwidthRestricted = bandwidthRestricted;
        this.blocksDownloadingSize = blocksDownloadingSize;
    }

    public static BlockDownloaderHandlerStateBuilder builder() {
        return new BlockDownloaderHandlerStateBuilder();
    }

    @Override
    public String toString() {
        // The state of this Handler is too big so it takes several lines:
        StringBuilder result = new StringBuilder("\n\n DOWNLOAD STATE: ");
        result.append(downloadingState);
        if (bandwidthRestricted) {
            result.append(" [bandwidth restricted to " + config.getMaxMBinParallel() + " MB]");
        }

        // Blocks Info:
        result.append("\n Blocks        : ");
        result.append(downloadedBlocks.size()).append(" downloaded");
        result.append(", ").append(pendingBlocks.size()).append(" pending");
        result.append(", ").append(blocksInLimbo.size()).append(" in limbo");
        result.append(", ").append(cancelledBlocks.size()).append(" cancelled");
        result.append(", ").append(discardedBlocks.size()).append( " discarded");
        result.append(", ").append(totalReattempts + " re-attempts");

        // Peers Info:
        long numPeersHandshaked = peersInfo.stream().filter(p -> p.isHandshaked()).count();
        long numPeersDownloading = peersInfo.stream().filter(p -> p.isProcessing()).count();
        long numPeersIdle = peersInfo.stream().filter(p -> p.isIdle()).count();
        long numPeersInLimbo = peersInfo.stream().filter(p -> p.isInLimbo()).count();
        result.append("\n Peers         : ");
        result.append(numPeersHandshaked).append(" peers");
        result.append(", ").append(numPeersDownloading).append(" downloading");
        result.append(" (" + busyPercentage + "% busy)");
        result.append(", ").append(numPeersIdle).append(" idle");
        result.append(", ").append(numPeersInLimbo).append(" in Limbo");

        // Download progress:
        if (numPeersDownloading > 0) {
            result.append("\n Progress      :\n");
            result.append("  > ");
            result.append(peersInfo.stream().filter(p -> p.isProcessing()).map(p -> p.toString()).collect(Collectors.joining("\n  > ")));
        }

        // Blocks in Limbo:
        if (blocksInLimbo.size() > 0) {
            result.append("\n Retry later   :\n  > ");

            String blocksInLimboStr = blocksInLimbo.stream().map(h -> {
                if (blocksLastActivity.containsKey(h)) {
                    Duration timePassedSinceLastActivity = Duration.between(blocksLastActivity.get(h), Instant.now());
                    long secsToRetry = config.getInactivityTimeoutToFail().toSeconds() - timePassedSinceLastActivity.toSeconds();
                    return (h + " Retrying in " + secsToRetry + " secs...");
                } else {
                    return " Retrying (calculating retry-time...)";
                }
            }).collect(Collectors.joining("\n  > "));
            result.append(blocksInLimboStr);
        }

        // Download Rejections:
        if (!downloadRejections.isEmpty()) {
            result.append("\n Rejections    :\n  > ");
            String rejectionsStr = this.downloadRejections.entrySet().stream().map(e -> {
                String peerStr = StringUtils.fixedLength(e.getKey().toString(), 36);
                return "Peer [" + peerStr + "] rejected: " + e.getValue() .toString();
            }).collect(Collectors.joining("\n  > "));
            result.append(rejectionsStr);
        }

        // Download Events:
        if (!downloadEvents.isEmpty()) {
            result.append("\n Last Events   :\n  > ");
            String eventsStr = IntStream.range(0, Math.min(this.downloadEvents.size(), NUM_EVENTS_TO_LOG))
                    .mapToObj(i -> this.downloadEvents.get(this.downloadEvents.size() - (i + 1)))
                    .collect(Collectors.joining("\n  > "));
            result.append(eventsStr);
        }

        result.append("\n");
        return result.toString();
    }

    public BlockDownloaderHandlerImpl.DonwloadingState getDownloadingState()
    { return this.downloadingState; }
    public List<String> getPendingBlocks()          { return this.pendingBlocks; }
    public Set<String> getDownloadedBlocks()        { return this.downloadedBlocks; }
    public List<String> getDiscardedBlocks()        { return this.discardedBlocks; }
    public List<String> getPendingToCancelBlocks()  { return this.pendingToCancelBlocks; }
    public List<String> getCancelledBlocks()        { return cancelledBlocks; }
    public Set<String>  getBlocksInLimbo()          { return blocksInLimbo; }

    public List<BlockPeerInfo> getPeersInfo()   { return this.peersInfo; }
    public long getTotalReattempts()            { return this.totalReattempts;}
    public int getBusyPercentage()              { return this.busyPercentage;}
    public boolean isRunning()                  { return this.downloadingState.equals(BlockDownloaderHandlerImpl.DonwloadingState.RUNNING);}
    public boolean isPaused()                   { return this.downloadingState.equals(BlockDownloaderHandlerImpl.DonwloadingState.PAUSED);}
    public Map<String, Integer> getBlocksNumDownloadAttempts()
    { return this.blocksNumDownloadAttempts; }
    public Map<String, List<BlocksDownloadHistory.HistoricItem<String, PeerAddress>>> getBlocksHistory()
    { return this.blocksHistory;}
    public List<String> getDownloadEvents()     { return this.downloadEvents;}
    public boolean isBandwidthRestricted()      { return bandwidthRestricted; }
    public long getBlocksDownloadingSize()      { return blocksDownloadingSize;}

    public long getNumPeersDownloading() {
        if (peersInfo == null) return 0;
        return peersInfo.stream()
                .filter( p -> p.getWorkingState().equals(BlockPeerInfo.PeerWorkingState.PROCESSING))
                .count();
    }

    public Optional<BlockPeerInfo> getPeerInfo(String blockHash) {
        if (this.peersInfo == null ) { return Optional.empty();}

        return this.peersInfo.stream()
                .filter(p -> p.getCurrentBlockInfo() != null)
                .filter(p -> p.getCurrentBlockInfo().getHash().equals(blockHash))
                .findFirst();
    }

    public BlockDownloaderHandlerStateBuilder toBuilder() {
        return new BlockDownloaderHandlerStateBuilder()
                .pendingBlocks(this.pendingBlocks)
                .downloadedBlocks(this.downloadedBlocks)
                .discardedBlocks(this.discardedBlocks)
                .pendingToCancelBlocks(this.pendingToCancelBlocks)
                .cancelledBlocks(this.cancelledBlocks)
                .blocksInLimbo(this.blocksInLimbo)
                .blocksHistory(this.blocksHistory)
                .downloadEvents(this.downloadEvents)
                .peersInfo(this.peersInfo)
                .blocksNumDownloadAttempts(this.blocksNumDownloadAttempts)
                .busyPercentage(this.busyPercentage)
                .bandwidthRestricted(this.bandwidthRestricted)
                .blocksDownloadingSize(this.blocksDownloadingSize);
    }

    /**
     * Builder
     */
    public static class BlockDownloaderHandlerStateBuilder {
        private BlockDownloaderHandlerConfig config;
        private BlockDownloaderHandlerImpl.DonwloadingState downloadingState;
        private List<String> pendingBlocks;
        private Set<String> downloadedBlocks;
        private List<String> discardedBlocks;
        private List<String> pendingToCancelBlocks;
        private List<String> cancelledBlocks;
        private Set<String> blocksInLimbo;
        private Map<String, List<BlocksDownloadHistory.HistoricItem<String, PeerAddress>>> blocksHistory;
        private List<String> downloadEvents;
        private Map<String, Instant> blocksLastActivity;
        private Map<PeerAddress, DownloadResponse> downloadRejections;
        private List<BlockPeerInfo> peersInfo;
        private long totalReattempts;
        private Map<String, Integer> blocksNumDownloadAttempts;
        private int busyPercentage;
        private boolean bandwidthRestricted;
        private long blocksDownloadingSize;

        BlockDownloaderHandlerStateBuilder() {
        }

        public BlockDownloaderHandlerState.BlockDownloaderHandlerStateBuilder config(BlockDownloaderHandlerConfig config) {
            this.config = config;
            return this;
        }

        public BlockDownloaderHandlerState.BlockDownloaderHandlerStateBuilder downloadingState(BlockDownloaderHandlerImpl.DonwloadingState downloadingState) {
            this.downloadingState = downloadingState;
            return this;
        }

        public BlockDownloaderHandlerState.BlockDownloaderHandlerStateBuilder pendingBlocks(List<String> pendingBlocks) {
            this.pendingBlocks = pendingBlocks;
            return this;
        }

        public BlockDownloaderHandlerState.BlockDownloaderHandlerStateBuilder downloadedBlocks(Set<String> downloadedBlocks) {
            this.downloadedBlocks = downloadedBlocks;
            return this;
        }

        public BlockDownloaderHandlerState.BlockDownloaderHandlerStateBuilder discardedBlocks(List<String> discardedBlocks) {
            this.discardedBlocks = discardedBlocks;
            return this;
        }

        public BlockDownloaderHandlerState.BlockDownloaderHandlerStateBuilder pendingToCancelBlocks(List<String> pendingToCancelBlocks) {
            this.pendingToCancelBlocks = pendingToCancelBlocks;
            return this;
        }

        public BlockDownloaderHandlerState.BlockDownloaderHandlerStateBuilder cancelledBlocks(List<String> cancelledBlocks) {
            this.cancelledBlocks = cancelledBlocks;
            return this;
        }

        public BlockDownloaderHandlerState.BlockDownloaderHandlerStateBuilder blocksInLimbo(Set<String> blocksInLimbo) {
            this.blocksInLimbo = blocksInLimbo;
            return this;
        }

        public BlockDownloaderHandlerState.BlockDownloaderHandlerStateBuilder blocksHistory(Map<String, List<BlocksDownloadHistory.HistoricItem<String, PeerAddress>>> blocksHistory) {
            this.blocksHistory = blocksHistory;
            return this;
        }

        public BlockDownloaderHandlerState.BlockDownloaderHandlerStateBuilder downloadEvents(List<String> downloadEvents) {
            this.downloadEvents = downloadEvents;
            return this;
        }

        public BlockDownloaderHandlerState.BlockDownloaderHandlerStateBuilder downloadRejections(Map<PeerAddress, DownloadResponse> downloadRejections) {
            this.downloadRejections = downloadRejections;
            return this;
        }

        public BlockDownloaderHandlerState.BlockDownloaderHandlerStateBuilder blocksLastActivity(Map<String, Instant> blocksLastActivity) {
            this.blocksLastActivity = blocksLastActivity;
            return this;
        }

        public BlockDownloaderHandlerState.BlockDownloaderHandlerStateBuilder peersInfo(List<BlockPeerInfo> peersInfo) {
            this.peersInfo = peersInfo;
            return this;
        }

        public BlockDownloaderHandlerState.BlockDownloaderHandlerStateBuilder totalReattempts(long totalReattempts) {
            this.totalReattempts = totalReattempts;
            return this;
        }

        public BlockDownloaderHandlerState.BlockDownloaderHandlerStateBuilder blocksNumDownloadAttempts(Map<String, Integer> blocksNumDownloadAttempts) {
            this.blocksNumDownloadAttempts = blocksNumDownloadAttempts;
            return this;
        }

        public BlockDownloaderHandlerState.BlockDownloaderHandlerStateBuilder busyPercentage(int busyPercentage) {
            this.busyPercentage = busyPercentage;
            return this;
        }

        public BlockDownloaderHandlerState.BlockDownloaderHandlerStateBuilder bandwidthRestricted(boolean bandwidthRestricted) {
            this.bandwidthRestricted = bandwidthRestricted;
            return this;
        }

        public BlockDownloaderHandlerState.BlockDownloaderHandlerStateBuilder blocksDownloadingSize(long blocksDownloadingSize) {
            this.blocksDownloadingSize = blocksDownloadingSize;
            return this;
        }

        public BlockDownloaderHandlerState build() {
            return new BlockDownloaderHandlerState(
                    config,
                    downloadingState,
                    pendingBlocks,
                    downloadedBlocks,
                    discardedBlocks,
                    pendingToCancelBlocks,
                    cancelledBlocks,
                    blocksInLimbo,
                    blocksHistory,
                    downloadEvents,
                    blocksLastActivity,
                    downloadRejections,
                    peersInfo,
                    totalReattempts,
                    blocksNumDownloadAttempts,
                    busyPercentage,
                    bandwidthRestricted,
                    blocksDownloadingSize);
        }
    }
}