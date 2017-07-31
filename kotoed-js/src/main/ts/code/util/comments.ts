
import {ReviewComments as ServerReviewComments} from "../remote/comments";
import {ReviewComments, FileComments, LineCommentsState, Comment} from "../state";
import {List, Map} from "immutable";

export function commentsResponseToState(reviewComments: ServerReviewComments): ReviewComments {
    let state: ReviewComments = Map<string, FileComments>();

    state = state.withMutations(function(s) {
        for (let serverFileComments of reviewComments) {
            let fileComments: FileComments = Map<number, LineCommentsState>();

            fileComments = fileComments.withMutations(function(fc) {
                for (let serverLineComments of serverFileComments.by_line) {
                    let lineComments: LineCommentsState = List<Comment>();

                    lineComments = lineComments.withMutations(function(lc) {
                        for (let serverComment of serverLineComments.comments) {
                            lc.push({
                                id: serverComment.id,
                                text: serverComment.text,
                                dateTime: serverComment.datetime,
                                authorName: serverComment.denizen_id,
                                authorId: serverComment.author_id,
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