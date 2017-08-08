
declare module "codemirror" {
    export interface CmMode{
        mode?: string
        mime?: string
    }

    export function findModeByExtension(ext: string): CmMode
}

export = {}