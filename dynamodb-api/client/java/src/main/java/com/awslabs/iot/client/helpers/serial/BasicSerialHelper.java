package com.awslabs.iot.client.helpers.serial;

import com.awslabs.iot.client.helpers.serial.interfaces.SerialHelper;
import com.fazecast.jSerialComm.SerialPort;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;

public class BasicSerialHelper implements SerialHelper {
    private static final Logger log = LoggerFactory.getLogger(BasicSerialHelper.class);

    @Inject
    public BasicSerialHelper() {
    }

    @Override
    public SerialPort openPort(Optional<String> optionalPartialName, int bitrate, int dataBits, int parityBits, int stopBits) {
        String partialName = optionalPartialName.orElse("tty.usbserial");

        SerialPort serialPort = getPort(partialName);

        if (serialPort.isOpen()) {
            throw new RuntimeException("Port [" + serialPort.getSystemPortName() + "] is currently in use");
        }

        serialPort.setBaudRate(bitrate);
        serialPort.setNumDataBits(dataBits);
        serialPort.setNumStopBits(stopBits);
        serialPort.setParity(parityBits);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);

        if (!serialPort.openPort()) {
            throw new RuntimeException("Failed to open port [" + serialPort.getSystemPortName() + "]");
        }

        // Try to sleep but don't fail if we're interrupted (don't call get())
        Try.run(() -> Thread.sleep(1000));

        return serialPort;
    }

    /*
        out.write("AT+CIER=1,1,1,1,1\r\n".getBytes());
        out.flush();

        StringBuilder stringBuilder = new StringBuilder();

        while (true) {
            Thread.sleep(10);

            char currentChar = (char) in.read();
            if (currentChar == 10) {
                log.info(stringBuilder.toString());
                stringBuilder = new StringBuilder();
            } else {
                stringBuilder.append(currentChar);
            }
        }

     */
}
