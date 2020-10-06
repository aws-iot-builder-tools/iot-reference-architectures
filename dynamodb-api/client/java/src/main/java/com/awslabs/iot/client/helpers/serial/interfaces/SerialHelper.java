package com.awslabs.iot.client.helpers.serial.interfaces;

import com.fazecast.jSerialComm.SerialPort;

import java.util.Arrays;
import java.util.Optional;

public interface SerialHelper {
    SerialPort openPort(Optional<String> optionalPartialName, int bitrate, int dataBits, int parityBits, int stopBits);

    default SerialPort getPort(String partialName) {
        Optional<SerialPort> optionalSerialPort = Arrays.stream(SerialPort.getCommPorts())
                .filter(port -> port.getSystemPortName().contains(partialName))
                .findFirst();

        if (!optionalSerialPort.isPresent()) {
            throw new RuntimeException("No suitable ports found");
        }

        return optionalSerialPort.get();
    }
}
