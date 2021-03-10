package com.nchain.jcl.tools.config;


import com.nchain.jcl.tools.bytes.ByteArrayConfig;
import com.nchain.jcl.tools.files.FileUtils;

import java.time.Duration;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An interface to provide values adjusted for a specific Hardware Configuration (Memory, Network Speed, etc)
 */
public interface RuntimeConfig {
    /**
     * Returns a ByteArrayMemoryConfiguration, which determines the amount of memory that the ByteArrays are using
     * during Serialization/Deserialization.
     */
    ByteArrayConfig getByteArrayMemoryConfig();

    /** Returns a number of Bytes. Any Message bigger than that value, is a candidate for Real-Time Deserialization */
    int getMsgSizeInBytesForRealTimeProcessing();

    /**
     * If we are processing bytes in real time and wehave to wait for longer than the value returned by this method,
     * then the process is interrupted.
     */
    Duration getMaxWaitingTimeForBytesInRealTime();

    /** File Utils used to read/write info in disk */
    FileUtils getFileUtils();
}
