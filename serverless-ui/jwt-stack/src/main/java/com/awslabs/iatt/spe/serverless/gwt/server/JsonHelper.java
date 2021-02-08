package com.awslabs.iatt.spe.serverless.gwt.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.vavr.control.Try;
import io.vavr.jackson.datatype.VavrModule;

public class JsonHelper {
    private static final ObjectWriter objectWriter = new ObjectMapper()
            .registerModule(new VavrModule())
            .writerWithDefaultPrettyPrinter();

    public static String toJson(Object object) {
        return Try.of(() -> objectWriter.writeValueAsString(object)).get();
    }
}
