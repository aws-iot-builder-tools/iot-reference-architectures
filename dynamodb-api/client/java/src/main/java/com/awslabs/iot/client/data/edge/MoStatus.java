package com.awslabs.iot.client.data.edge;

public enum MoStatus {
    GSS_MO_MESSAGE_IF_ANY_TRANSFERRED_SUCCESSFULLY(0, true),
    GSS_MO_MESSAGE_IF_ANY_TRANSFERRED_SUCCESSFULLY_BUT_QUEUE_TOO_BIG(1, true),
    GSS_MO_MESSAGE_IF_ANY_TRANSFERRED_SUCCESSFULLY_BUT_LOCATION_UPDATE_NOT_ACCEPTED(2, true),
    GSS_MO_SESSION_SUCCESS_RESERVED_1(3, true),
    GSS_MO_SESSION_SUCCESS_RESERVED_2(4, true),
    GSS_MO_SESSION_FAILURE_RESERVED_1(5, false),
    GSS_MO_SESSION_FAILURE_RESERVED_2(6, false),
    GSS_MO_SESSION_FAILURE_RESERVED_3(7, false),
    GSS_MO_SESSION_FAILURE_RESERVED_4(8, false),
    GSS_CALL_DID_NOT_COMPLETE_IN_ALLOWED_TIME(10, false),
    GSS_MO_QUEUE_AT_GSS_IS_FULL(11, false),
    GSS_MO_MESSAGE_HAS_TOO_MANY_SEGMENTS(12, false),
    GSS_SESSION_DID_NOT_COMPLETE(13, false),
    GSS_INVALID_SEGMENT_SIZE(14, false),
    GSS_ACCESS_DENIED(15, false),
    ISU_LOCKED(16, false),
    ISU_GATEWAY_NOT_RESPONDING(17, false),
    ISU_CONNECTION_LOST(18, false),
    ISU_LINK_FAILURE(19, false),
    ISU_FAILURE_RESERVED_1(20, false),
    ISU_FAILURE_RESERVED_2(21, false),
    ISU_FAILURE_RESERVED_3(22, false),
    ISU_FAILURE_RESERVED_4(23, false),
    ISU_FAILURE_RESERVED_5(24, false),
    ISU_FAILURE_RESERVED_6(25, false),
    ISU_FAILURE_RESERVED_7(26, false),
    ISU_FAILURE_RESERVED_8(27, false),
    ISU_FAILURE_RESERVED_9(28, false),
    ISU_FAILURE_RESERVED_10(29, false),
    ISU_FAILURE_RESERVED_11(30, false),
    ISU_FAILURE_RESERVED_12(31, false),
    ISU_NO_NETWORK_SERVICE(32, false),
    ISU_ANTENNA_FAULT(33, false),
    ISU_RADIO_DISABLED(34, false),
    ISU_BUSY(35, false),
    ISU_TRY_LATER_3_MINUTES(36, false),
    ISU_SBD_SERVICE_TEMPORARILY_DISABLED(37, false),
    ISU_TRY_LATER_CHECK_SBDLOE(38, false),
    ISU_FAILURE_RESERVED_13(39, false),
    ISU_FAILURE_RESERVED_14(40, false),
    ISU_FAILURE_RESERVED_15(41, false),
    ISU_FAILURE_RESERVED_16(42, false),
    ISU_FAILURE_RESERVED_17(43, false),
    ISU_FAILURE_RESERVED_18(44, false),
    ISU_FAILURE_RESERVED_19(45, false),
    ISU_FAILURE_RESERVED_20(46, false),
    ISU_FAILURE_RESERVED_21(47, false),
    ISU_FAILURE_RESERVED_22(48, false),
    ISU_FAILURE_RESERVED_23(49, false),
    ISU_FAILURE_RESERVED_24(50, false),
    ISU_FAILURE_RESERVED_25(51, false),
    ISU_FAILURE_RESERVED_26(52, false),
    ISU_FAILURE_RESERVED_27(53, false),
    ISU_FAILURE_RESERVED_28(54, false),
    ISU_FAILURE_RESERVED_29(55, false),
    ISU_FAILURE_RESERVED_30(56, false),
    ISU_FAILURE_RESERVED_31(57, false),
    ISU_FAILURE_RESERVED_32(58, false),
    ISU_FAILURE_RESERVED_33(59, false),
    ISU_FAILURE_RESERVED_34(60, false),
    ISU_FAILURE_RESERVED_35(61, false),
    ISU_FAILURE_RESERVED_36(62, false),
    ISU_FAILURE_RESERVED_37(63, false),
    ISU_BAND_VIOLATION(64, false),
    ISU_PLL_LOCK_FAILURE(65, false);

    public boolean isSuccess() {
        return success;
    }

    public boolean isMoTransferSuccess() {
        if (value == GSS_MO_MESSAGE_IF_ANY_TRANSFERRED_SUCCESSFULLY.value) {
            return true;
        }

        if (value == GSS_MO_MESSAGE_IF_ANY_TRANSFERRED_SUCCESSFULLY_BUT_QUEUE_TOO_BIG.value) {
            return true;
        }

        if (value == GSS_MO_MESSAGE_IF_ANY_TRANSFERRED_SUCCESSFULLY_BUT_LOCATION_UPDATE_NOT_ACCEPTED.value) {
            return true;
        }

        return false;
    }

    public int getValue() {
        return value;
    }

    private final int value;
    private final boolean success;

    MoStatus(int value, boolean success) {
        this.value = value;
        this.success = success;
    }

    public static MoStatus fromInt(int intValue) {
        for (MoStatus moStatus : MoStatus.values()) {
            if (moStatus.value == intValue) {
                return moStatus;
            }
        }

        throw new RuntimeException("No matching value for [" + intValue + "]");
    }
}
