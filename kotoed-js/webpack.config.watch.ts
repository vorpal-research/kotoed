import * as path from 'path';
import * as fs from 'fs';

declare const __dirname: string;

import config from "./webpack.config.dev"

const dotVertx = path.resolve(__dirname, "../.vertx");

const cacheDirs = fs.readdirSync(dotVertx).filter((f) => f.startsWith("file-cache-"));

if (cacheDirs.length === 0) {
    throw new Error("No Vertx file cache found. Is Kotoed running?");
}

if (cacheDirs.length !== 1) {
    throw new Error("Multiple Vertx file caches found. I don't know what to do...");
}

config!.output!.path = path.resolve(dotVertx, cacheDirs[0], "webroot/static/");

console.log(config!.output!.path);

export default config;