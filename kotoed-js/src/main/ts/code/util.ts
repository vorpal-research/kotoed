/**
 * Created by gagarski on 7/10/17.
 */

import {Comment} from "./model"

export function groupByLine(comments: Comment[]): Comment[][] {
    let grouped: Comment[][] = [];

    for (let comment of comments) {
        let current = grouped[comment.location.line] || [];
        current.push(comment);
        grouped[comment.location.line] = current;
    }

    return grouped;
}

export function toCmLine(line: number) {
    return line - 1;
}

export function fromCmLine(line: number) {
    return line + 1;
}

export interface CmMode {
    mode?: string,
    contentType?: string
}

export function guessCmMode(filename: string) {
    let extension = filename.match(/(.*)[\/\\]([^\/\\]+)\.(\w+)$/)[3];
    switch (extension) {
        case "kt":
            return {
                mode: "clike",
                contentType: "text/x-kotlin"
            };
        case "scala":
            return {
                mode: "clike",
                contentType: "text/x-scala"
            };
        case "java":
            return {
                mode: "clike",
                contentType: "text/x-java"
            };
        default:
            return {}
    }
}