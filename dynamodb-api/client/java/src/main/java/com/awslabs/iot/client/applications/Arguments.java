package com.awslabs.iot.client.applications;

import com.beust.jcommander.Parameter;
import com.fazecast.jSerialComm.SerialPort;

public class Arguments {
    private final String LONG_UUID_OPTION = "--uuid";
    private final String SHORT_UUID_OPTION = "-u";
    private final String LONG_SERIAL_OPTION = "--serial";
    private final String SHORT_SERIAL_OPTION = "-s";

    @Parameter(names = {LONG_UUID_OPTION, SHORT_UUID_OPTION})
    public String uuid;
    @Parameter(names = {LONG_SERIAL_OPTION, SHORT_SERIAL_OPTION})
    public boolean serial;

    public SerialPort serialPort;
}
