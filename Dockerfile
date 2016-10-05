FROM maven:3-jdk-8

ARG GOSU_VERSION=1.9
ARG GOSU_DOWNLOAD_URL="https://github.com/tianon/gosu/releases/download/${GOSU_VERSION}/gosu-amd64"
RUN curl -o gosu -fsSL "$GOSU_DOWNLOAD_URL" > gosu-amd64 \
 && mv gosu /usr/bin/gosu \
 && chmod +x /usr/bin/gosu

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app