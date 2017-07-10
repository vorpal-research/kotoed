import $ = require('jquery');

import * as cm from "codemirror"
import "codemirror/mode/clike/clike"
import "codemirror/addon/fold/foldcode"
import "codemirror/addon/fold/foldgutter"
import "codemirror/addon/fold/brace-fold"
import "codemirror/addon/fold/comment-fold"
import * as moment from "moment"
import * as cmr from "./cm_review";

import "less/kotoed-bootstrap/bootstrap.less";
import "less/code.less";
import "codemirror/addon/fold/foldgutter.css";
import "codemirror/lib/codemirror.css";

var code = `
package org.jetbrains.research.kotoed.web.handlers

import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.SessionHandler

interface SessionProlongator: Handler<RoutingContext> {
   fun setMaxAge(maxAge: Long)
   fun setSessionCookieName(name: String)

   companion object {
       val DEFAULT_MAX_AGE = 30L * 24L * 60L * 60L
       val DEFAULT_SESSION_COOKIE_NAME = SessionHandler.DEFAULT_SESSION_COOKIE_NAME
       fun create(): SessionProlongator = SessionProlongatorImpl(DEFAULT_MAX_AGE, DEFAULT_SESSION_COOKIE_NAME)
   }
}

internal class SessionProlongatorImpl(private var maxAge: Long,
                                     private var sessionCookieName: String) : SessionProlongator {
   override fun setMaxAge(maxAge: Long) {
       this.maxAge = maxAge
   }

   override fun setSessionCookieName(name: String) {
       this.sessionCookieName = sessionCookieName
   }

   override fun handle(context: RoutingContext) {
       val cookie = context.getCookie(sessionCookieName)
       cookie.setMaxAge(maxAge)
       context.next()
   }

}
`;

$(document).ready(function() {
    let fr = new cmr.FileReview(
        document.getElementById("editor") as HTMLTextAreaElement,
        {
            lineNumbers: true,
            mode: "text/x-kotlin",
            readOnly: true,
            foldGutter: true,
            gutters: ["CodeMirror-linenumbers", "CodeMirror-foldgutter", "review-gutter"],
            lineWrapping: true,
        },
        true,
        [
            {
                id: 0,
                text: "Hello!",
                dateTime: ("1941-06-22"),
                author: "Ffff",
                state: "open",
                location: {
                    file: "do not care",
                    line: 2
                }
            },
            {
                id: 1,
                text: "fsasdfdasf!",
                dateTime: ("1941-06-22"),
                author: "Ffff",
                state: "open",
                location: {
                    file: "do not care",
                    line: 2
                }
            },
            {
                id: 2,
                text: "dfasdasfdasfasdf!",
                dateTime: ("1941-06-22"),
                author: "Ffff",
                state: "open",
                location: {
                    file: "do not care",
                    line: 2
                }
            },
            {
                id: 3,
                text: "!!!!",
                dateTime: ("1941-06-22"),
                author: "Ffff",
                state: "open",
                location: {
                    file: "do not care",
                    line: 8
                }
            },
            {
                id: 4,
                text: "???",
                dateTime: ("1941-06-22"),
                author: "Ffff",
                state: "open",
                location: {
                    file: "do not care",
                    line: 8
                }
            },


        ]
    )
});