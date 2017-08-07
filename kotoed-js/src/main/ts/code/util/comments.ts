import {List, Map} from "immutable";

import {CommentToRead, ReviewComments as ServerReviewComments} from "../remote/comments";
import {ReviewComments, FileComments, LineComments, Comment} from "../state/comments";
import {Capabilities} from "../remote/capabilities";

export function addRenderingProps(comment: CommentToRead, capabilities: Capabilities): Comment {
    return {
        ...comment,
        canStateBeChanged: capabilities.permissions.changeStateAllComments ||
            (comment.authorId == capabilities.principal.id) && capabilities.permissions.changeStateOwnComments,
        canBeEdited: capabilities.permissions.editAllComments ||
            (comment.authorId == capabilities.principal.id) && capabilities.permissions.editOwnComments,
        collapsed: comment.state === "closed"
    }
}

export function commentsResponseToState(reviewComments: ServerReviewComments, capabilities: Capabilities): ReviewComments {
    let state: ReviewComments = Map<string, FileComments>();

    state = state.withMutations(function(s) {
        for (let serverFileComments of reviewComments) {
            let fileComments: FileComments = Map<number, LineComments>();

            fileComments = fileComments.withMutations(function(fc) {
                for (let serverLineComments of serverFileComments.byLine) {
                    let lineComments: LineComments = List<Comment>();

                    lineComments = lineComments.withMutations(function(lc) {
                        for (let serverComment of serverLineComments.comments) {
                            lc.push(addRenderingProps(serverComment, capabilities));
                        }
                    });

                    fc.set(serverLineComments.line, lineComments);
                }
            });

            s.set(serverFileComments.filename, fileComments);
        }
    });
    return state;
}