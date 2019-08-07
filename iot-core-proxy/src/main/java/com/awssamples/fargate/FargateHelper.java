package com.awssamples.fargate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awscdk.services.ecs.FargateTaskDefinitionProps;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FargateHelper {
    private static final Logger log = LoggerFactory.getLogger(FargateHelper.class);
    private static List<Integer> validVCpuQuarterRam = Arrays.asList(1024 / 2, 1024, 2 * 1024);
    private static List<Integer> validVCpuHalfRam = IntStream.rangeClosed(1, 4).map(toMegabytes()).boxed().collect(Collectors.toList());
    private static List<Integer> validVCpuOneRam = IntStream.rangeClosed(2, 8).map(toMegabytes()).boxed().collect(Collectors.toList());
    private static List<Integer> validVCpuTwoRam = IntStream.rangeClosed(4, 16).map(toMegabytes()).boxed().collect(Collectors.toList());
    private static List<Integer> validVCpuFourRam = IntStream.rangeClosed(8, 30).map(toMegabytes()).boxed().collect(Collectors.toList());

    private static IntUnaryOperator toMegabytes() {
        return i -> i * 1024;
    }


    public static FargateTaskDefinitionProps.Builder getValidMemoryAndCpu(VCPU vCpu, int ramInMb) {
        FargateTaskDefinitionProps.Builder fargateTaskDefinitionPropsBuilder = FargateTaskDefinitionProps.builder()
                .cpu(vCpu.value);

        fargateTaskDefinitionPropsBuilder.memoryLimitMiB(closestTo(ramInMb, vCpu.validRam));

        return fargateTaskDefinitionPropsBuilder;
    }

    private static int closestTo(int ramInMb, List<Integer> validRamValues) {
        int value = validRamValues.stream()
                .min(Comparator.comparingInt(i -> Math.abs(i - ramInMb)))
                .orElseThrow(() -> new RuntimeException("No values present"));

        if (value != ramInMb) {
            log.warn("Requested [" + ramInMb + "] but it had to be changed to [" + value + "]");
        }

        return value;
    }

    public enum VCPU {
        Quarter(256, validVCpuQuarterRam),
        Half(512, validVCpuHalfRam),
        One(1024, validVCpuOneRam),
        Two(2048, validVCpuTwoRam),
        Four(4096, validVCpuFourRam);

        private final int value;
        private final List<Integer> validRam;

        VCPU(int value, List<Integer> validRam) {
            this.value = value;
            this.validRam = validRam;
        }
    }
}
