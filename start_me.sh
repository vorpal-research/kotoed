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
cp -f kotoed-all/target/kotoed-all-*-SNAPSHOT-fat.jar ${COPY_DIR}/kotoed.jar
cp -f -r lib ${COPY_DIR}/lib

java \
    -cp "${COPY_DIR}/lib/*:${COPY_DIR}/kotoed.jar" \
    -Dkotlinx.coroutines.debug \
    -Djava.net.preferIPv4Stack=true \
    -Dkotoed.settingsFile=deploySettings.json \
    org.jetbrains.research.kotoed.MainKt
