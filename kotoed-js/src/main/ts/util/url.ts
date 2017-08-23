import * as QueryString from "query-string";

import {Kotoed} from "./kotoed-api";
import {UNKNOWN_FILE, UNKNOWN_LINE} from "../code/remote/constants";
import {ScrollTo} from "../code/state/index";
import {BaseCommentToRead} from "../data/comment";

// TODO properly support relative paths
export function fromLocationHost(path: string): string {
    let location = window.location;
    let normPath = (path.length > 0 && path[0] === '/') ? path.slice(1) : path;
    return `${location.protocol}//${location.host}/${normPath}`;
}


export function makeCodeReviewCodePath(submissionId: number, path: string, scrollTo?: ScrollTo) {
    let hash = scrollTo !== undefined ? `#${QueryString.stringify(scrollTo)}` : "";
    return Kotoed.UrlPattern.reverse(Kotoed.UrlPattern.CodeReview.Index, {
        id: submissionId
    }, "code/" + path) + hash;
}

export function makeCodeReviewLostFoundPath(submissionId: number, scrollTo?: ScrollTo) {
    let hash = scrollTo !== undefined ? `#${QueryString.stringify(scrollTo)}` : "";
    return Kotoed.UrlPattern.reverse(Kotoed.UrlPattern.CodeReview.Index, {
        id: submissionId
    }, "lost+found");
}

export function makeCommentPath(comment: BaseCommentToRead) {
    let {sourcefile, sourceline, submissionId, id} = comment;
    if (sourcefile === UNKNOWN_FILE || sourceline === UNKNOWN_LINE)
        return makeCodeReviewLostFoundPath(submissionId, {

            commentId: id
        });
    else
        return makeCodeReviewCodePath(submissionId, sourcefile, {
            line: sourceline,
            commentId: id
        })
}