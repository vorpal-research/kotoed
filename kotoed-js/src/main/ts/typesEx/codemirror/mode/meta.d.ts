
declare module "codemirror" {
    export interface CmMode{
        mode?: string
        mime?: string
    }

    export function findModeByExtension(ext: string): CmMode | undefined
    export function findModeByName(name: string): CmMode | undefined
    export function findModeByFileName(filename: string): CmMode | undefined

}

export = {}