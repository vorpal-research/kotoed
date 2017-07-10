/**
 * Created by gagarski on 7/10/17.
 */

import {Comment} from "./model"

export function groupByLine(comments: Comment[]): Comment[][] {
    let grouped: Comment[][] = [];

    for (let comment of comments) {
        let current = grouped[comment.location.line] || [];
        current.push(comment);
        grouped[comment.location.line] = current;
    }

    return grouped;
}