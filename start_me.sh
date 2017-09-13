#!/usr/bin/env bash

ME_DIR=$(dirname $(realpath $0))

cd "$ME_DIR"

java \
    -Dkotlinx.coroutines.debug \
    -Djava.net.preferIPv4Stack=true \
    -Dkotoed.settingsFile=deploySettings.json \
    -jar kotoed-server/target/kotoed-server-*-SNAPSHOT-fat.jar
