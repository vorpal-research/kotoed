import * as cm from "codemirror"
import "codemirror/mode/meta"
import {CmMode} from "codemirror";
import {Map} from "immutable";

const DEFAULT_MODE = "text/plain";

export function toCmLine(line: number) {
    return line - 1;
}

export function fromCmLine(line: number) {
    return line + 1;
}

export function guessCmModeForLang(lang: string) {
    if (!lang)
        return {};
    return cm.findModeByName(lang) || {};
}

export function guessCmModeForExt(ext: string) {
    if (!ext)
        return {};
    return cm.findModeByExtension(ext) || {};
}

export function guessCmModeForLangOrExt(lang: string) {
    if (!lang)
        return {};
    return cm.findModeByName(lang) || cm.findModeByExtension(lang) || {};
}


export function guessCmModeForFile(filename: string): CmMode {
    if (!filename)
        return {};
    return cm.findModeByFileName(filename) || {};
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