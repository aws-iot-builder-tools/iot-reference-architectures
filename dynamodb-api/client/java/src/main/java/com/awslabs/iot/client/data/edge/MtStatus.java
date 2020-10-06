package com.awslabs.iot.client.data.edge;

public enum MtStatus {
    NO_MESSAGE_TO_RECEIVE(0),
    SBD_MESSAGE_SUCCESSFULLY_RECEIVED(1),
    ERROR_WHILE_RECEIVING_SBD_MESSAGE(2);

    private final int value;

    MtStatus(int value) {
        this.value = value;
    }

    public static MtStatus fromInt(int intValue) {
        for (MtStatus mtStatus : MtStatus.values()) {
            if (mtStatus.value == intValue) {
                return mtStatus;
            }
        }

        throw new RuntimeException("No matching value for [" + intValue + "]");
    }
}
