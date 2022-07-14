FROM public.ecr.aws/lambda/java:11 as base

FROM base as runtimebuild1

# Installing these tools takes a while so we do it as early as possible
RUN yum update -y
RUN yum install -y libguestfs libguestfs-tools libguestfs-java

FROM base as images

# Download the binary dependencies
RUN yum update -y
RUN yum install -y unzip curl
RUN mkdir -p /images
WORKDIR /images

# Option 1: Download
#RUN curl --retry 10 https://downloads.raspberrypi.org/raspios_lite_armhf/images/raspios_lite_armhf-2021-03-25/2021-03-04-raspios-buster-armhf-lite.zip -o image.zip
#RUN unzip image.zip
#RUN rm image.zip

# Option 2: Copy and unzip
#COPY images/2021-03-04-raspios-buster-armhf-lite.zip image.zip
#RUN unzip image.zip
#RUN rm image.zip

# Option 3: Copy unzipped
#COPY images/2021-10-30-raspios-bullseye-arm64.img 2021-10-30-raspios-bullseye-arm64.img
COPY images/2022-04-04-raspios-bullseye-armhf-lite.img 2022-04-04-raspios-bullseye-armhf-lite.img

RUN curl --retry 10 https://s3.us-east-1.amazonaws.com/amazon-ssm-us-east-1/latest/debian_arm/amazon-ssm-agent.deb -o amazon-ssm-agent-arm32.deb
RUN curl --retry 10 https://s3.us-east-1.amazonaws.com/amazon-ssm-us-east-1/latest/debian_arm64/amazon-ssm-agent.deb -o amazon-ssm-agent-arm64.deb

FROM base as javabuild0
ENV JAVA_HOME /var/lang

WORKDIR /tmp
COPY src/gradlew .
COPY src/gradle gradle
COPY src/build.gradle.minimal.kts build.gradle.kts

RUN ./gradlew clean --no-daemon
RUN rm build.gradle.kts

FROM javabuild0 as javabuild1

# Copy the runtime dependencies only when the build file changes, not on every build
WORKDIR /src
COPY src/gradlew .
COPY src/gradle gradle
COPY src/build.gradle.kts build.gradle.kts

RUN ./gradlew copyRuntimeDependencies --no-daemon

FROM runtimebuild1 as runtimebuild2

RUN mkdir ${LAMBDA_TASK_ROOT}/images
COPY --from=images /images ${LAMBDA_TASK_ROOT}/images
COPY --from=javabuild1 /src/build/dependency ${LAMBDA_TASK_ROOT}/lib

FROM javabuild1 as javabuild2

# Build the Java code
COPY src .
RUN ./gradlew build --no-daemon

FROM runtimebuild2 as runtimebuild3
WORKDIR /function

# Guidance from: https://gallery.ecr.aws/lambda/java - "Usage" tab
COPY --from=javabuild2 /src/build/classes/java/main ${LAMBDA_TASK_ROOT}

# Our JVM looks for libguestfs_jni.so, not libguestfs_jni.so.1
RUN ln -s /usr/lib64/libguestfs_jni.so.1 ${LAMBDA_TASK_ROOT}/lib/libguestfs_jni.so
RUN cp /usr/share/java/libguestfs.jar ${LAMBDA_TASK_ROOT}/lib

# Don't use libvirtd
ENV LIBGUESTFS_BACKEND direct

#ENV LIBGUESTFS_DEBUG 1
#ENV LIBGUESTFS_TRACE 1
ENV TMPDIR /tmp
ENV LIBGUESTFS_BACKEND_SETTINGS force_tcg

#ENTRYPOINT [ "/opt/java/openjdk/bin/java", "-cp", "/function/*", "com.amazonaws.services.lambda.runtime.api.client.AWSLambda" ]
CMD [ "lambda.App::handleRequest" ]
