
// TODO properly support relative paths
import {Kotoed} from "./kotoed-api";
import {UNKNOWN_FILE, UNKNOWN_LINE} from "../code/remote/constants";
export function fromLocationHost(path: string): string {
    let location = window.location;
    let normPath = (path.length > 0 && path[0] === '/') ? path.slice(1) : path;
    return `${location.protocol}//${location.host}/${normPath}`;
}


export function makeCodeReviewCodePath(submissionId: number, path: string, scrollTo?: number) {
    let hash = scrollTo !== undefined ? `#${scrollTo}` : "";
    return Kotoed.UrlPattern.reverse(Kotoed.UrlPattern.CodeReview.Index, {
        id: submissionId
    }, "code/" + path) + hash;
}

export function makeCodeReviewLostFoundPath(submissionId: number) {
    return Kotoed.UrlPattern.reverse(Kotoed.UrlPattern.CodeReview.Index, {
        id: submissionId
    }, "lost+found");
}

export function makeCommentPath(submissionId: number, path: string, scrollTo?: number) {
    if (path === UNKNOWN_FILE || scrollTo === UNKNOWN_LINE)
        return makeCodeReviewLostFoundPath(submissionId);
    else
        return makeCodeReviewCodePath(submissionId, path, scrollTo)
}