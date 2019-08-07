package com.awssamples.shared;

import java.util.Random;
import java.util.UUID;

public class CdkHelper {
    private static String stackName;
    private static Random random;

    public static void setStackName(String stackName) {
        CdkHelper.stackName = stackName;
        random = new Random(UUID.nameUUIDFromBytes(CdkHelper.stackName.getBytes()).getLeastSignificantBits());
    }

    public static String getRandomId() {
        return "id" + random.nextLong();
    }
}