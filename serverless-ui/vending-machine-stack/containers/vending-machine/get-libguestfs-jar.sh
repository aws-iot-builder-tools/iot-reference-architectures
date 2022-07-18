#!/usr/bin/env bash

set -e

image_uuid=(uuidgen)

echo "
FROM public.ecr.aws/lambda/java:11 as base
#FROM debian:stable-20210511

# Installing these tools takes a while so we do it as early as possible
RUN yum update -y
#RUN yum install -y yum-utils
#RUN yum-config-manager --enable epel
#RUN yum update -y
#RUN yum upgrade -y
RUN yum install -y libguestfs-java
RUN yum info libguestfs-java
#RUN apt update -y
##RUN apt upgrade -y
#RUN apt install -y libguestfs-java
" | docker build --progress=plain -f - -t $image_uuid .
container_id=$(docker create $image_uuid)
#docker cp $container_id:/usr/share/java/libguestfs-1.40.2.jar src/libs/libguestfs-1.40.2.jar
mkdir -p src/libs
docker cp $container_id:/usr/share/java/libguestfs.jar src/libs/libguestfs.jar
docker rm -v $container_id
