
// TODO properly support relative paths
export function fromLocationHost(path: string): string {
    let location = window.location;
    let normPath = (path.length > 0 && path[0] === '/') ? path.slice(1) : path;
    return `${location.protocol}//${location.host}/${normPath}`;
}


export const CODE_REVIEW_BASE_ADDR = "/codereview";

export function makeCodePath(submissionId: number, path: string, scrollTo?: number) {
    let hash = scrollTo !== undefined ? `#${scrollTo}` : "";
    return `/${submissionId}/code/${path}${hash}`
}