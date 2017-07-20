import {Comment} from "../model"

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

export function guessCmMode(filename: string): CmMode {
    let match = filename.match(/(.*)[\/\\]([^\/\\]+)\.(\w+)$/);
    if (!match || match.length < 4)
        return {};

    switch (match[3]) {
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
        case "less":
            return {
                mode: "css",
                contentType: "text/x-less"
            };
        case "css":
            return {
                mode: "css",
                contentType: "text/x-css"
            };
        case "ts":
            return {
                mode: "javascript",
                contentType: "text/typescript"
            };
        case "js":
            return {
                mode: "javascript",
                contentType: "text/javascript"
            };


        default:
            return {}
    }
}