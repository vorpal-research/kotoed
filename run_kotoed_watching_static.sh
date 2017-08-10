#!/usr/bin/env bash

# This script runs Kotoed and webpack watching static in parallel.
#
# The whole purpose of this mess is to ensure that webpack dies before Kotoed
# Otherwise webpack may recreate .vertx/file-cache- directory,
# so we will have two file caches on next run and webpack.config.watch.ts
# won't be able to find Vertx file cache next time.

trap 'await_death' INT TERM HUP

function await_death() {
    trap '' INT TERM HUP
    echo "Shutting down webpack..."
    kill -TERM ${WEBPACK_PID}
    wait ${WEBPACK_PID}


    echo "Shutting down kotoed..."
    kill -TERM ${KOTOED_PID}
    wait ${KOTOED_PID}

    cd ${DIR}

    # rm -rf .vertx/*  # Shall we?

    kill -TERM 0 # Killing cat ^(x_x)^
}

DIR=$PWD

java -jar kotoed-server/target/kotoed-server-0.1.0-SNAPSHOT-fat.jar &

KOTOED_PID=$!

sleep 10 # Is it enough to vertx to extract cache?

cd kotoed-js

webpack --progress --colors --watch --config=webpack.config.watch.ts &

WEBPACK_PID=$!

cat # Doing nothing