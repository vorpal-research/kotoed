/**
 * Created by gagarski on 7/6/17.
 */

import * as ace from 'brace'
import * as moment from 'moment'

// TODO we'll probably drop ace and delete this file

namespace AceReview {

    namespace Util {

        export function groupByLine(comments: Comment[]): Comment[][] {
            let grouped: Comment[][] = [];

            for (let comment of comments) {
                let current = grouped[comment.location.line] || [];
                current.push(comment);
                grouped[comment.location.line] = current;
            }

            return grouped;
        }
    }

    export type CommentState = "unpublished" | "open" | "closed";

    export class CommentLocation {
        constructor(
            readonly file: string,
            readonly line: number
        ) {}
    }

    export abstract class Comment {
        constructor(
            public text: string,
            // public dateTime: moment.MomentInput,
            // public author: string,
            public location: CommentLocation,
            // public state: CommentState = "unpublished"
        ) {
            // this.dateTime = moment(dateTime);
        }

        abstract render(): HTMLElement
    }


    export class DummyComment extends Comment {
        render(): HTMLElement {
            let el = ace.acequire("ace/lib/dom").createElement("div");
            el.className = "dummy_comment";
            el.innerHTML = this.text;
            return el
        }

    }

    export abstract class CommentPublisher {
        private queue: Array<Comment> = [];

        enqueue(comment: Comment): void {
            this.queue.push(comment);
        }

        getQueueLength(): number {
            return this.queue.length;
        }

        abstract publish(comment: Comment): void

        publishAll(): void {
            for (let comment of this.queue) {
                this.publish(comment);
            }
        }

        abstract update(comment: Comment, patch: {state?: CommentState, text?: string}): void

    }

    export class FileReview {

        private widgetManager: any;

        constructor(private editor: ace.Editor,
                    // private publisher: CommentPublisher,
                    private readOnly: boolean = true,
                    private initialComments: Array<Comment> = []
        ) {
            // Non-public API magic
            let session = editor.session as any;  // TODO types!
            let dom = ace.acequire("ace/lib/dom");

            if (!session.widgetManager) {
                session.widgetManager = new (ace.acequire("ace/line_widgets").LineWidgets)(session);
                session.widgetManager.attach(editor);
            }

            let grouped = Util.groupByLine(initialComments);

            for (let line in grouped) {
                editor.session.unfold(parseInt(line) - 1, false);
                let comments = grouped[line];

                let multipleCommentsDiv = dom.createElement("div");
                multipleCommentsDiv.className = "multiple_comments error_widget_wrapper";
                let ew = multipleCommentsDiv.appendChild(dom.createElement("div"));
                ew.className = "error_widget ace_ok";
                let lineWidget = {
                    row: parseInt(line) - 1,
                    fixedWidth: true,
                    coverGutter: true,
                    el: multipleCommentsDiv
                };

                for (let comment of comments) {
                    ew.appendChild(comment.render());
                }
                session.widgetManager.addLineWidget(lineWidget);
            }

        }

        foo(): void {
            this.editor.getSession().addGutterDecoration(2, "fuck");
            this.editor.on("gutterclick", function(event) {
                event.getDocumentPosition();
            });
        }
    }
}

export = AceReview