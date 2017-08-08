import * as cm from "codemirror"
import "codemirror/mode/meta"
import {CmMode} from "codemirror";
import {Map} from "immutable";

// Extra aliases for markdown, not known by CodeMirror
export const ALIASES: Map<string, string> = Map([
    ["kotlin", "kt"],
    ["typescript", "ts"],
    ["javascript", "js"],
    ["typescript-jsx", "tsx"],
    ["python", "py"],
]);

const DEFAULT_MODE = "text/plain";

export function toCmLine(line: number) {
    return line - 1;
}

export function fromCmLine(line: number) {
    return line + 1;
}

export function guessCmModeForLang(lang: string) {
    let ext = ALIASES.get(lang, lang);
    return guessCmModeForExt(ext);
}

export function guessCmModeForExt(ext: string) {
    return cm.findModeByExtension(ext);
}

export function guessCmModeForFile(filename: string): CmMode {
    let match = filename.match(/(?:(?:.*)[\/\\])?(?:[^\/\\]+)\.(\w+)$/);
    if (!match || match.length < 2)
        return {};

    return guessCmModeForExt(match[1].toLowerCase())
}

export function requireCmMode(mode: CmMode) {
    if (mode.mode)
        require(`codemirror/mode/${mode.mode}/${mode.mode}`);
}


export function editorModeParam(mode: CmMode): string {
    return mode.mime || mode.mode || DEFAULT_MODE;
}

// Standard gutters
export const FOLD_GUTTER = "CodeMirror-foldgutter";
export const LINE_NUMBER_GUTTER = "CodeMirror-linenumbers";