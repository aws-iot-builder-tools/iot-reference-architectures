package com.awslabs.iot.client.helpers;

public class ANSIHelper {
    private static final char ESCAPE = (char) 27;
    public static final String CRLF = "\r\n";
    public static final String RED = ESCAPE + "[31m";
    public static final String BLACK = ESCAPE + "[30m";
    public static final String WHITE = ESCAPE + "[37m";
    public static final String BOLD = ESCAPE + "[1m";
    public static final String NORMAL = ESCAPE + "[0m";
}
