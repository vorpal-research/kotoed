#!/usr/bin/env bash

# This script runs Kotoed and webpack watching static in parallel.
#
# The whole purpose of this mess is to ensure that webpack dies before Kotoed
# Otherwise webpack may recreate .vertx/file-cache- directory,
# so we will have two file caches on next run and webpack.config.watch.ts
# won't be able to find Vertx file cache next time.

trap 'await_death' INT TERM HUP

function await_death() {
    DIE_REQUESTED=true
    trap '' INT TERM HUP
    if [[ -n "${WEBPACK_PID}" ]]; then
        echo "Shutting down webpack..."
        kill -TERM ${WEBPACK_PID}
        wait ${WEBPACK_PID}
        echo "Webpack is down!"
    fi

    if [[ -n "${KOTOED_PID}" ]]; then
        echo "Shutting down kotoed..."
        kill -TERM ${KOTOED_PID}
        wait ${KOTOED_PID}
        echo "Kotoed is down!"
    fi
    cd ${DIR}

    # rm -rf .vertx/*  # Shall we?

    kill -TERM 0 # Killing cat ^(x_x)^
}

DIR=$PWD

if [[ -z "${DIE_REQUESTED}" ]]; then
    java \
        -Dkotlinx.coroutines.debug \
        -Djava.net.preferIPv4Stack=true \
        -Dkotoed.settingsFile=deploySettings.json \
        -Dvertx.cacheDirBase=./.vertx \
        -jar kotoed-all/target/kotoed-all-0.1.0-SNAPSHOT-fat.jar &
    KOTOED_PID=$!
fi
sleep 10 # Is it enough to vertx to extract cache?

cd kotoed-js

if [[ -z "${DIE_REQUESTED}" ]]; then
    ./node_modules/.bin/webpack --progress --colors --watch --config=webpack.config.watch.ts &
    WEBPACK_PID=$!
fi

if [[ -z "${DIE_REQUESTED}" ]]; then
    cat # Doing nothing
fi