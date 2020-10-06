package com.awslabs.iot.client.data.edge;

public enum MtFlag {
    NO_MESSAGE_IN_MT_BUFFER(0),
    MESSAGE_IN_MT_BUFFER(1);

    public int getValue() {
        return value;
    }

    private final int value;

    MtFlag(int value) {
        this.value = value;
    }

    public static MtFlag fromInt(int intValue) {
        for (MtFlag moStatus : MtFlag.values()) {
            if (moStatus.value == intValue) {
                return moStatus;
            }
        }

        throw new RuntimeException("No matching value for [" + intValue + "]");
    }
}
