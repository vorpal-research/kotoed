declare module "codemirror" {
    export interface Editor {
        execCommand(name: string)
    }
}

export = {}