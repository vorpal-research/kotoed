
export function fromLocationHost(path: string): string {
    let location = window.location;
    return `${location.protocol}://${location.host}/${path}`;
}