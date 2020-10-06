package com.awslabs.iot.client.commands;

import com.awslabs.iot.client.applications.Arguments;
import com.awslabs.iot.client.data.*;
import com.awslabs.iot.client.data.edge.*;
import com.fazecast.jSerialComm.SerialPort;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.awslabs.iot.client.helpers.ANSIHelper.CRLF;

public class SharedSerial implements SharedCommunication {
    public static final String SBDIX = "+SBDIX";
    public static final String SBDS = "+SBDS";
    public static final String SBDWT = "+SBDWT";
    public static final String SBDRT = "+SBDRT";
    public static final String CGSN = "+CGSN";
    public static final String SBDD0 = "+SBDD0";
    public static final String SBDD1 = "+SBDD1";
    public static final String AT = "AT";
    public static final String AT_SBDWT = String.join("", AT, SBDWT);
    public static final String AT_SBDIX = String.join("", AT, SBDIX);
    public static final String AT_SBDIX_CRLF = String.join("", AT_SBDIX, CRLF);
    public static final String AT_SBDS = String.join("", AT, SBDS);
    public static final String AT_SBDS_CRLF = String.join("", AT, SBDS, CRLF);
    public static final String AT_CGSN = String.join("", AT, CGSN);
    public static final String AT_CGSN_CRLF = String.join("", AT_CGSN, CRLF);
    public static final String AT_SBDRT = String.join("", AT, SBDRT);
    public static final String AT_SBDRT_CRLF = String.join("", AT_SBDRT, CRLF);
    public static final String AT_SBDD0 = String.join("", AT, SBDD0);
    public static final String AT_SBDD0_CRLF = String.join("", AT_SBDD0, CRLF);
    public static final String AT_SBDD1 = String.join("", AT, SBDD1);
    public static final String AT_SBDD1_CRLF = String.join("", AT_SBDD1, CRLF);
    public static final String EMPTY = "";
    public static final String OK = "OK";
    private final Logger log = LoggerFactory.getLogger(SharedSerial.class);

    @Inject
    Arguments arguments;
    private Optional<BufferedReader> optionalBufferedReader = Optional.empty();

    @Inject
    public SharedSerial() {
    }

    private Optional throwNotSupportedOnSerialPortsException() {
        throw new RuntimeException("Not supported on serial ports");
    }

    @Override
    public Optional<List> getUuids() {
        return throwNotSupportedOnSerialPortsException();
    }

    @Override
    public Optional<QueryResponse> query() {
        return throwNotSupportedOnSerialPortsException();
    }

    @Override
    public Optional<GetResponse> getMessage(String messageId) {
        return throwNotSupportedOnSerialPortsException();
    }

    @Override
    public Optional<NextResponse> nextMessage(String messageId) {
        return throwNotSupportedOnSerialPortsException();
    }

    @Override
    public Optional<DeleteResponse> deleteMessage(String messageId) {
        return throwNotSupportedOnSerialPortsException();
    }

    @Override
    public void getAndDisplayMessage(String messageId) {
        throwNotSupportedOnSerialPortsException();
    }

    public boolean isSbdRingDetected() {
        synchronized (arguments.serialPort) {
            BufferedReader bufferedReader = getBufferedReader(arguments.serialPort);

            boolean ready = Try.of(bufferedReader::ready).get();

            if (!ready) {
                return false;
            }

            discardAndThrowIfNotMatching(bufferedReader, EMPTY);
            String data = safeReadLine(bufferedReader);

            return data.contains("SBDRING");
        }
    }

    @Override
    public Optional<SendResponse> sendMessage(String recipientUuid, String message) {
        if (!isMoBufferEmpty()) {
            log.error("MO buffer is not empty, can not queue another message");
            return Optional.empty();
        }

        String command = String.join("", AT_SBDWT, "=", message);
        String commandWithCrLf = String.join("", command, CRLF);

        synchronized (arguments.serialPort) {
            BufferedReader bufferedReader = getBufferedReader(arguments.serialPort);
            Try.run(() -> arguments.serialPort.getOutputStream().write(commandWithCrLf.getBytes())).get();
            discardAndThrowIfNotMatching(bufferedReader, command);
            discardAndThrowIfNotMatching(bufferedReader, EMPTY);
            discardAndThrowIfNotMatching(bufferedReader, OK);

            return Optional.empty();
        }
    }

    private boolean isMoBufferEmpty() {
        return !checkPendingMessages().getMoFlag().equals(MoFlag.MESSAGE_IN_MO_BUFFER);
    }

    public String getUuid() {
        synchronized (arguments.serialPort) {
            BufferedReader bufferedReader = getBufferedReader(arguments.serialPort);
            Try.run(() -> arguments.serialPort.getOutputStream().write(AT_CGSN_CRLF.getBytes())).get();
            discardAndThrowIfNotMatching(bufferedReader, AT_CGSN);
            discardAndThrowIfNotMatching(bufferedReader, EMPTY);
            String uuid = safeReadLine(bufferedReader);
            discardAndThrowIfNotMatching(bufferedReader, EMPTY);
            discardAndThrowIfNotMatching(bufferedReader, OK);

            return uuid;
        }
    }

    public String getTextMessage() {
        synchronized (arguments.serialPort) {
            BufferedReader bufferedReader = getBufferedReader(arguments.serialPort);
            Try.run(() -> arguments.serialPort.getOutputStream().write(AT_SBDRT_CRLF.getBytes())).get();
            discardAndThrowIfNotMatching(bufferedReader, AT_SBDRT);
            discardAndThrowIfNotMatching(bufferedReader, EMPTY);
            discardAndThrowIfNotMatching(bufferedReader, String.join("", SBDRT, ":"));
            String message = safeReadLine(bufferedReader);
            // Trim the first two bytes from all inbound text messages (first is 1 - start of heading, second is length)
            if (message.length() >= 2) {
                message = message.substring(2);
            } else {
                log.warn("Message was not at least two bytes, what could this be?");
                for (byte data : message.getBytes()) {
                    log.warn("BYTE: [" + (int) data + "]");
                }
            }

            discardAndThrowIfNotMatching(bufferedReader, OK);

            return message;
        }
    }

    public void clearMoBuffer() {
        synchronized (arguments.serialPort) {
            BufferedReader bufferedReader = getBufferedReader(arguments.serialPort);
            Try.run(() -> arguments.serialPort.getOutputStream().write(AT_SBDD0_CRLF.getBytes())).get();
            discardAndThrowIfNotMatching(bufferedReader, AT_SBDD0);
            discardAndThrowIfNotMatching(bufferedReader, EMPTY);
            discardAndThrowIfNotMatching(bufferedReader, "0");
            discardAndThrowIfNotMatching(bufferedReader, EMPTY);
            discardAndThrowIfNotMatching(bufferedReader, OK);
        }
    }

    public void clearMtBuffer() {
        synchronized (arguments.serialPort) {
            BufferedReader bufferedReader = getBufferedReader(arguments.serialPort);
            Try.run(() -> arguments.serialPort.getOutputStream().write(AT_SBDD1_CRLF.getBytes())).get();
            discardAndThrowIfNotMatching(bufferedReader, AT_SBDD1);
            discardAndThrowIfNotMatching(bufferedReader, EMPTY);
            discardAndThrowIfNotMatching(bufferedReader, "0");
            discardAndThrowIfNotMatching(bufferedReader, EMPTY);
            discardAndThrowIfNotMatching(bufferedReader, OK);
        }
    }

    public SbdixResponse startSbdSession() {
        List<Integer> numbers = sendCommandAndReturnNumericResponses(AT_SBDIX_CRLF, AT_SBDIX, SBDIX, 6);

        ImmutableSbdixResponse immutableSbdixResponse = ImmutableSbdixResponse.builder()
                .moStatus(MoStatus.fromInt(numbers.get(0)))
                .moMsn(numbers.get(1))
                .mtStatus(MtStatus.fromInt(numbers.get(2)))
                .mtMsn(numbers.get(3))
                .mtLength(numbers.get(4))
                .mtQueued(numbers.get(5))
                .build();

        synchronized (arguments.serialPort) {
            BufferedReader bufferedReader = getBufferedReader(arguments.serialPort);
            discardAndThrowIfNotMatching(bufferedReader, EMPTY);
            discardAndThrowIfNotMatching(bufferedReader, OK);

            return immutableSbdixResponse;
        }
    }

    private List<Integer> sendCommandAndReturnNumericResponses(String commandToSend, String responseString1, String
            responseString2Prefix, int numberOfResponseComponents) {
        synchronized (arguments.serialPort) {
            BufferedReader bufferedReader = getBufferedReader(arguments.serialPort);
            Try.run(() -> arguments.serialPort.getOutputStream().write(commandToSend.getBytes())).get();

            discardAndThrowIfNotMatching(bufferedReader, responseString1);

            discardAndThrowIfNotMatching(bufferedReader, EMPTY);

            String responseString2 = safeReadLine(bufferedReader);

            if (!responseString2.startsWith(responseString2Prefix)) {
                throw new RuntimeException("Expected response starting with [" + responseString2Prefix + "], received [" + responseString2 + "]");
            }

            // Trim the leading characters
            responseString2 = responseString2.substring(responseString2Prefix.length() + 2);

            // Split the string
            String[] splitReponseString = responseString2.split(", ");

            if (splitReponseString.length != numberOfResponseComponents) {
                throw new RuntimeException("Expected a split string with " + numberOfResponseComponents + " components, received [" + splitReponseString.length + "] [" + responseString2 + "]");
            }

            return Arrays.stream(splitReponseString).map(Integer::parseInt).collect(Collectors.toList());
        }
    }

    private BufferedReader getBufferedReader(SerialPort serialPort) {
        synchronized (arguments.serialPort) {
            if (!optionalBufferedReader.isPresent()) {
                optionalBufferedReader = Optional.of(new BufferedReader(new InputStreamReader(serialPort.getInputStream())));
            }

            return optionalBufferedReader.get();
        }
    }

    public SbdsResponse checkPendingMessages() {
        synchronized (arguments.serialPort) {
            List<Integer> numbers = sendCommandAndReturnNumericResponses(AT_SBDS_CRLF, AT_SBDS, SBDS, 4);

            ImmutableSbdsResponse immutableSbdsResponse = ImmutableSbdsResponse.builder()
                    .moFlag(MoFlag.fromInt(numbers.get(0)))
                    .moMsn(numbers.get(1))
                    .mtFlag(MtFlag.fromInt(numbers.get(2)))
                    .mtMsn(numbers.get(3))
                    .build();

            synchronized (arguments.serialPort) {
                BufferedReader bufferedReader = getBufferedReader(arguments.serialPort);
                discardAndThrowIfNotMatching(bufferedReader, EMPTY);
                discardAndThrowIfNotMatching(bufferedReader, OK);

                return immutableSbdsResponse;
            }
        }
    }

    private String safeReadLine(BufferedReader bufferedReader) {
        String line = Try.of(bufferedReader::readLine).get();

        if (line.length() == 0) {
            return line;
        }

        if (line.getBytes()[0] == 0) {
            // Trim leading NULL
            line = line.substring(1);
        }

        return line;
    }

    public void discardAndThrowIfNotMatching(BufferedReader bufferedReader, String expected) {
        String line = safeReadLine(bufferedReader);

        if (!throwIfNotMatching(line, expected)) {
            while (Try.of(bufferedReader::ready).get()) {
                line = safeReadLine(bufferedReader);
                log.error("DEBUG LINE: " + line);
            }

            log.error("No more data waiting");
            System.exit(1);
        }
    }

    private boolean throwIfNotMatching(String line, String expected) {
        if (line.equals(expected)) {
            return true;
        }

        log.error("Expected [" + expected + "], received [" + line + "]");
        for (byte data : line.getBytes()) {
            log.error("BYTE: [" + (int) data + "]");
        }
        log.error("Dumping remaining data...");

        return false;
    }
}