/* Because sometimes Partial<T> is just not enough */
export type PPartial<T> = {
    [P in keyof T]?: PPartial<T[P]> | null;
};

/* Mapping<'a' | 'b' | 'c', V> is essentially the same as { ['a']: V; ['b']: V; ['c']: V }*/
export type Mapping<K extends string, V> = Pick<{ [key: string]: V }, K>

