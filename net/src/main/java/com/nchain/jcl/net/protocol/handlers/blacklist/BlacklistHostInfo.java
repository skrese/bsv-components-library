package com.nchain.jcl.net.protocol.handlers.blacklist;

import com.nchain.jcl.base.tools.files.CSVSerializable;
import com.nchain.jcl.base.tools.util.DateTimeUtils;
import com.nchain.jcl.net.network.events.PeersBlacklistedEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.StringTokenizer;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-15 14:55
 *
 * This class stores all the info that the Blacklist Handler needs to do its work. It sotres info for each HOST
 * (Not Peer, but a HOST. Only the IP address). If a Host is blacklisted, ALL the Peers using that IP (but with
 * different port) will be disconnected.
 */
@Slf4j
@NoArgsConstructor
@Getter
@Setter
public class BlacklistHostInfo implements CSVSerializable  {
    // Host IP Address:
    private InetAddress ip;
    // If blacklisted, these variables stores the reason and the time when it was blacklisted:
    private PeersBlacklistedEvent.BlacklistReason blacklistReason;
    private LocalDateTime blacklistTimestamp;

    // We keep track of the number of times this Host has reached specific scenarios:
    private int numFailedHandshakes;
    private int numFailedPingPongs;
    private int numConnRejections;
    private int numSerializationErrors;


    /** Constructor */
    public BlacklistHostInfo(InetAddress ip) {
        this.ip = ip;
    }

    public void reset() {
        this.blacklistReason = null;
        this.blacklistTimestamp = null;
        this.numConnRejections = 0;
        this.numFailedHandshakes = 0;
        this.numFailedPingPongs = 0;
        this.numSerializationErrors = 0;
    }

    // Convenience methods:
    public boolean isBlacklisted()          { return blacklistReason != null; }
    public void addFailedHandshakes()       { numFailedHandshakes++;}
    public void addFailedPingPongs()        { numFailedPingPongs++;}
    public void addConnRejections()         { numConnRejections++;}
    public void addSerializationErrors()    { numSerializationErrors++;}

    public void blacklist(PeersBlacklistedEvent.BlacklistReason reason) {
        this.blacklistReason = reason;
        this.blacklistTimestamp = DateTimeUtils.nowDateTimeUTC();
    }
    // TODO: CAREFUL WITH UTC AND TIMEZONES!!

    public boolean isBlacklistExpired() {
        if (!isBlacklisted()) return false;
        if (!blacklistReason.getExpirationTime().isPresent()) return false;
        return (Duration.between(blacklistTimestamp, DateTimeUtils.nowDateTimeUTC()).compareTo(blacklistReason.getExpirationTime().get()) > 0);
    }

    public void whitelist() {
        // We reset the field according to the Reason provided:
        switch (blacklistReason) {
            case CONNECTION_REJECTED: {
                this.numConnRejections = 0;
                break;
            }
            case SERIALIZATION_ERROR: {
                this.numSerializationErrors = 0;
                break;
            }
            case FAILED_HANDSHAKE: {
                this.numFailedHandshakes = 0;
                break;
            }
            case PINGPONG_TIMEOUT: {
                this.numFailedPingPongs = 0;
                break;
            }
        }
        this.blacklistReason = null;
        this.blacklistTimestamp = null;
    }

    /** It prints the info of this Host, so it can be saved in a CSV file */
    @Override
    public String toCSVLine() {
        StringBuffer result = new StringBuffer();
        result.append(ip.getHostAddress()).append(CSVSerializable.SEPARATOR);
        result.append(blacklistTimestamp.format(DateTimeFormatter.ISO_DATE_TIME)).append(CSVSerializable.SEPARATOR);
        result.append(blacklistReason);
        return result.toString();
    }

    @Override
    public void fromCSVLine(String line) {
        if (line == null) return;
        try {
            reset();
            StringTokenizer tokens = new StringTokenizer(line, CSVSerializable.SEPARATOR);
            this.ip = InetAddress.getByName(tokens.nextToken());
            this.blacklistTimestamp = LocalDateTime.parse(tokens.nextToken(), DateTimeFormatter.ISO_DATE_TIME);
            this.blacklistReason = PeersBlacklistedEvent.BlacklistReason.valueOf(tokens.nextToken());
        } catch (Exception e) {
            log.error("Error Parsing line for CSV: " + line);
            throw new RuntimeException(e);
        }
    }
}