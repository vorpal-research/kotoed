import * as React from "react";
import * as ReactRouter from "react-router"
import * as cm from "codemirror"
import {render} from "react-dom";

import {Comment} from "../model";
import {default as FileReviewComponent, FileReviewProps} from "../components/FileReviewComponent";
import {CmMode, groupByLine, guessCmMode} from "../util";

const codeKt = `
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


const codeJava = `
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.codec.digest;

import java.security.MessageDigest;

/**
 * Standard {@link MessageDigest} algorithm names from the <cite>Java Cryptography Architecture Standard Algorithm Name
 * Documentation</cite>.
 * <p>
 * This class is immutable and thread-safe.
 * </p>
 * TODO This should be an enum.
 *
 * @see <a href="http://docs.oracle.com/javase/6/docs/technotes/guides/security/StandardNames.html">Java Cryptography
 *      Architecture Standard Algorithm Name Documentation</a>
 * @since 1.7
 * @version $Id: MessageDigestAlgorithms.java 1585867 2014-04-09 00:12:36Z ggregory $
 */
public class MessageDigestAlgorithms {

    private MessageDigestAlgorithms() {
        // cannot be instantiated.
    }

    /**
     * The MD2 message digest algorithm defined in RFC 1319.
     */
    public static final String MD2 = "MD2";

    /**
     * The MD5 message digest algorithm defined in RFC 1321.
     */
    public static final String MD5 = "MD5";

    /**
     * The SHA-1 hash algorithm defined in the FIPS PUB 180-2.
     */
    public static final String SHA_1 = "SHA-1";

    /**
     * The SHA-256 hash algorithm defined in the FIPS PUB 180-2.
     */
    public static final String SHA_256 = "SHA-256";

    /**
     * The SHA-384 hash algorithm defined in the FIPS PUB 180-2.
     */
    public static final String SHA_384 = "SHA-384";

    /**
     * The SHA-512 hash algorithm defined in the FIPS PUB 180-2.
     */
    public static final String SHA_512 = "SHA-512";

}
`;

const codeScala = `
// Contributed by John Williams
package examples

object lazyLib {

  /** Delay the evaluation of an expression until it is needed. */
  def delay[A](value: => A): Susp[A] = new SuspImpl[A](value)

  /** Get the value of a delayed expression. */
  implicit def force[A](s: Susp[A]): A = s()

  /** 
   * Data type of suspended computations. (The name froms from ML.) 
   */
  abstract class Susp[+A] extends Function0[A]

  /** 
   * Implementation of suspended computations, separated from the 
   * abstract class so that the type parameter can be invariant. 
   */
  class SuspImpl[A](lazyValue: => A) extends Susp[A] {
    private var maybeValue: Option[A] = None

    override def apply() = maybeValue match {
      case None =>
        val value = lazyValue
        maybeValue = Some(value)
        value
	  case Some(value) =>
        value
    }

    override def toString() = maybeValue match {
      case None => "Susp(?)"
      case Some(value) => "Susp(" + value + ")"
    }
  }
}

object lazyEvaluation {
  import lazyLib._

  def main(args: Array[String]) = {
    val s: Susp[Int] = delay { println("evaluating..."); 3 }

    println("s     = " + s)       // show that s is unevaluated
    println("s()   = " + s())     // evaluate s
    println("s     = " + s)       // show that the value is saved
    println("2 + s = " + (2 + s)) // implicit call to force()

    val sl = delay { Some(3) }
    val sl1: Susp[Some[Int]] = sl
    val sl2: Susp[Option[Int]] = sl1   // the type is covariant

    println("sl2   = " + sl2)
    println("sl2() = " + sl2())
    println("sl2   = " + sl2)
  }
}
`;

const codePlain = `
Last Christmas, I gave you my Frisbee
But the very next day, you threw it away
This year, to save me from tears
I'll give it to Harold Bishop

I'm dreaming of a special Christmas
Just like the ones I used to know
Where the stockings glisten and grandparents listen
To hear reindeer hooves in the snow

You were adorable
You were intelligent
Monarch of my hotel
When the band finished playing
They howled out for more
The crowd were swinging
All the grandparents they were singing
You asked me to love
And then we did drink

But then last Christmas, I gave you my Frisbee
And the very next day, you threw it away
This year, to save me from tears
I'll give it to Harold Bishop instead

You're grubby
You're interfering
Happy Christmas to you
I pray God it's your last

Rockin' around the Christmas tree
At the Christmas party hop, 
Rockin' around the Christmas tree, 
Let the Christmas spirit ring, 
Later I'll have some mince pies 
And me and Harold Bishop will drink

I don't want a lot for Christmas
There is just one thing I need
I don't care about new computers
I just want Harold Bishop for my own
More than you could ever know
Make my wish come true oh
All I want for Christmas is Harold Bishop

Oh, Harold Bishop yeah, oh Harold Bishop yeah
Harold Bishop the wife is a terrific, remarkable soul
Harold Bishop the wife is a fairy tale they say

Harold Bishop baby, I want you and really that is all
I'll wait up for you, dear
Harold Bishop baby, won't you drink with me tonight?

So here it is, special Christmas
Everybody's here to love
Look to the future now
It's only just begun

Simply having a special Christmastime
Simply having a special Christmastime
`;

const comments: Comment[] = [
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
            line: 9
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
            line: 9
        }
    },

];



function chooseCode(mode: CmMode) {
    switch(mode.contentType) {
        case "text/x-kotlin": return codeKt;
        case "text/x-java": return codeJava;
        case "text/x-scala": return codeScala;
        default: return codePlain;
    }
}

export default class CodeReviewContainer extends
        React.Component<ReactRouter.RouteComponentProps<{splat: Array<string>}>, FileReviewProps> {
    constructor(props) {
        super(props);
        let mode = guessCmMode(this.props.match.params["path"]);
        this.state = {
            height: 800,
            comments: groupByLine(comments),
            value: chooseCode(mode),
            ...mode
        }
    }

    render() {
        console.log(this.props);
        return <FileReviewComponent
            mode={this.state.mode}
            contentType={this.state.contentType}
            height={this.state.height}
            comments={this.state.comments}
            value={this.state.value}/>
    }
}