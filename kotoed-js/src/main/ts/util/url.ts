
// TODO properly support relative paths
export function fromLocationHost(path: string): string {
    let location = window.location;
    let normPath = (path.length > 0 && path[0] === '/') ? path.slice(1) : path;
    return `${location.protocol}//${location.host}/${normPath}`;
}