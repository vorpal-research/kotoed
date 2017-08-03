package org.jetbrains.research.kotoed.util

import io.vertx.ext.auth.User

suspend fun User.isAuthorised(authority: String) = vxa<Boolean> { this.isAuthorised(authority, it) }