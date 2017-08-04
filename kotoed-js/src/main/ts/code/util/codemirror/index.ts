
import {CM_MODES_BY_EXT} from "./data";

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


export function guessCmModeForExt(ext: string) {
    return CM_MODES_BY_EXT.get(ext, {});
}

export function guessCmModeForFile(filename: string): CmMode {
    let match = filename.match(/(.*)[\/\\]([^\/\\]+)\.(\w+)$/);
    if (!match || match.length < 4)
        return {};

    return guessCmModeForExt(match[3])
}

export function requireCmMode(mode: CmMode) {
    if (mode.mode)
        require(`codemirror/mode/${mode.mode}/${mode.mode}`);
}

const DEFAULT_MODE = "text/plain";

export function editorModeParam(mode: CmMode): string {
    return mode.contentType || mode.mode || DEFAULT_MODE;
}

// Standard gutters
export const FOLD_GUTTER = "CodeMirror-foldgutter";
export const LINE_NUMBER_GUTTER = "CodeMirror-linenumbers";