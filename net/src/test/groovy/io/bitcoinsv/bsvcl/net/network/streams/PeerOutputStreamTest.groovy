package io.bitcoinsv.bsvcl.net.network.streams

import io.bitcoinsv.bsvcl.net.network.PeerAddress
import spock.lang.Specification

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PeerOutputStreamTest extends Specification {

    /**
     * Convenience method. It creates a Pipeline of 2 InputStreams: NumberToStringStream and StringToNumberStream
     * It sends the source data through them. As the data passes through the Streams it gets transformed, and the
     * final result is returned.
     */
    private List<Integer> runPipeline(List<Integer> sourceData) {
        List<Integer> resultData = new ArrayList<>()

        // The Peer assigned to this Stream:
        PeerAddress peerAddress = PeerAddress.localhost(5050)

        // We create The Destination of this pipeline of PeerOutputsStream:
        PeerStreamInOutSimulator<Integer> destination = new PeerStreamInOutSimulator<>(peerAddress)

        // We create our Pipeline of InputStreams:
        PeerOutputStream<String> stringToNumberStream = new StringNumberOutputStream(peerAddress, destination)
        PeerOutputStream<Integer> numberToStringStream = new NumberStringOutputStream(peerAddress, stringToNumberStream)

        destination.onData({data ->
            println("Adding " + data + " to the result list...")
            resultData.add(data)
        })

        // We send data...
        for (Integer data : sourceData) { numberToStringStream.send(data) }

        // We wait a little bit until all te data has passed through the InputStream:
        Thread.sleep(1000)

        // And we return the result:
        return resultData
    }


    /**
     * We test that the transformation function works fine, and for each data sent, we receive the right result at
     * the destination.
     * NOTE: For the Stream to work property we need to provide an ExecutorService with just one Thread
     * (single Thread), otherwise we cannot guarantee that the results sent by the Output Stream are
     * coming out in the same order as we send them)
     */
    def "Testing Transformation Function"() {
        given:
        // A single-Thread executor
        ExecutorService executor = Executors.newSingleThreadExecutor()

        // We configure the source data...
        List<Integer> sourceData = new ArrayList<>()
        for (int i = 0; i < 20; i++) { sourceData.add(i) }
        when:
        List<Integer> resultData = runPipeline(sourceData)
        // Now we compare the data sent and the data returned...
        boolean resultsMatch = sourceData.equals(resultData)
        then:
        resultsMatch
    }
}