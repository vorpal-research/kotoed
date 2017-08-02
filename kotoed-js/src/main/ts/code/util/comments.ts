import {List, Map} from "immutable";

import {ReviewComments as ServerReviewComments} from "../remote/comments";
import {ReviewComments, FileComments, LineComments, Comment} from "../state";

export function commentsResponseToState(reviewComments: ServerReviewComments): ReviewComments {
    let state: ReviewComments = Map<string, FileComments>();

    state = state.withMutations(function(s) {
        for (let serverFileComments of reviewComments) {
            let fileComments: FileComments = Map<number, LineComments>();

            fileComments = fileComments.withMutations(function(fc) {
                for (let serverLineComments of serverFileComments.byLine) {
                    let lineComments: LineComments = List<Comment>();

                    lineComments = lineComments.withMutations(function(lc) {
                        for (let serverComment of serverLineComments.comments) {
                            lc.push({
                                id: serverComment.id,
                                text: serverComment.text,
                                dateTime: serverComment.datetime,
                                authorName: serverComment.denizenId,
                                authorId: serverComment.authorId,
                                state: serverComment.state
                            });
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