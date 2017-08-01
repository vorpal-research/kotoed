import {Comment} from "../model"
import {Map} from "immutable"

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

const CM_MODES_BY_EXT: Map<string, CmMode> = Map([
    ["kt", {
        mode: "clike",
        contentType: "text/x-kotlin"
    }],
    ["java", {
        mode: "clike",
        contentType: "text/x-java"
    }],
    ["scala", {
        mode: "clike",
        contentType: "text/x-scala"
    }],
    ["less", {
        mode: "css",
        contentType: "text/x-less"
    }],
    ["css", {
        mode: "css",
        contentType: "text/x-css"
    }],
    ["ts", {
        mode: "javascript",
        contentType: "text/typescript"
    }],
    ["js", {
        mode: "javascript",
        contentType: "text/javascript"
    }],
    ["tsx", {
        mode: "jsx",
        contentType: "text/typescript-jsx"
    }],
    ["jsx", {
        mode: "jsx",
        contentType: "text/jsx"
    }],
    ["yml", {
        mode: "yaml",
        contentType: "text/x-yaml"
    }],
    ["py", {
        mode: "python",
        contentType: "text/x-python"
    }],
    ["sql", {
        mode: "sql",
        contentType: "text/x-sql"
    }],
    ["json", {
        mode: "javascript",
        contentType: "application/json"
    }],
    ["pug", {
        mode: "pug",
        contentType: "text/x-pug"
    }],
    ["jade", {
        mode: "pug",
        contentType: "text/x-pug"
    }],

]);

export function guessCmMode(filename: string): CmMode {
    let match = filename.match(/(.*)[\/\\]([^\/\\]+)\.(\w+)$/);
    if (!match || match.length < 4)
        return {};

    return CM_MODES_BY_EXT.get(match[3], {});
}

export function requireCmMode(mode: CmMode) {
    if (mode.mode)
        require(`codemirror/mode/${mode}/${mode}`);
}

const DEFAULT_MODE = "text/plain";

export function editorModeParam(mode: CmMode): string {
    return mode.contentType || mode.mode || DEFAULT_MODE;
}

// Standard gutters
export const FOLD_GUTTER = "CodeMirror-foldgutter";
export const LINE_NUMBER_GUTTER = "CodeMirror-linenumbers";