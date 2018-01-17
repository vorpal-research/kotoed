#!/usr/bin/env bash


function clean_up() {
    if [[ -n ${COPY_DIR} ]]; then
        rm -rf ${COPY_DIR}
    fi
}

trap clean_up EXIT

ME_DIR=$(dirname $(realpath $0))

cd "$ME_DIR"

# So we can rebuild kotoed jar while running
COPY_DIR=$(mktemp -d --suffix=-kotoed)
cp -f kotoed-server/target/kotoed-server-*-SNAPSHOT-fat.jar ${COPY_DIR}/kotoed.jar

java \
    -Dkotlinx.coroutines.debug \
    -Djava.net.preferIPv4Stack=true \
    -Dkotoed.settingsFile=deploySettings.json \
    -jar ${COPY_DIR}/kotoed.jar
