declare module "codemirror" {
    interface Ruler {
        column: number,
        className?: string,
        color?: string,
        lineStyle?:
            "none" |
            "hidden" |
            "dotted" |
            "dashed" |
            "solid" |
            "double" |
            "groove" |
            "ridge" |
            "inset" |
            "outset"|
            "initial" |
            "inherit"  // These are CSS line styles
        width?: number
    }

    export interface EditorConfiguration {
        rulers?: Array<Ruler>
    }
}

export = {}