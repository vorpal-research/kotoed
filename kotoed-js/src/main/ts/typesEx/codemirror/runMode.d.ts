
// Copy-paste from @types/codemirrorcodemirror-runmode.d.ts
// TODO find a way to use that file instead of copy-pasting
declare module "codemirror" {
    function runMode(text: string, modespec: any, callback: (HTMLElement | ((text: string, style: string | null) => void)), options? : { tabSize?: number; state?: any; }): void;
}

export = {}