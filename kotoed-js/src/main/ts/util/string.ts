export function truncateString(str: string, len: number): string {
    if (str.length <= len) return str;
    else {
        let truc = (len - 3) / 2;
        let [left, right] = [Math.ceil(truc), Math.floor(truc)];
        return str.substr(0, left) + "..." + str.substr(str.length - right, right)
    }
}