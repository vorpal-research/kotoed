export function truncateString(str: string, len: number): string {
    if (str.length <= len) return str;
    else {
        let truc = (len - 3) / 2;
        return str.substr(0, truc) + "..." + str.substr(str.length - truc, truc)
    }
}