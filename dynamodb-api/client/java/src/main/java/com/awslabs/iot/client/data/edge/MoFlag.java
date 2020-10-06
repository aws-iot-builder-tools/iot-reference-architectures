package com.awslabs.iot.client.data.edge;

public enum MoFlag {
    NO_MESSAGE_IN_MO_BUFFER(0),
    MESSAGE_IN_MO_BUFFER(1);

    public int getValue() {
        return value;
    }

    private final int value;

    MoFlag(int value) {
        this.value = value;
    }

    public static MoFlag fromInt(int intValue) {
        for (MoFlag moStatus : MoFlag.values()) {
            if (moStatus.value == intValue) {
                return moStatus;
            }
        }

        throw new RuntimeException("No matching value for [" + intValue + "]");
    }
}
