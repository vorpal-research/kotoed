
export type ErrorMessages<Flags> = {
    [flag in keyof Flags]: string
}

