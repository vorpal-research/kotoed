package org.jetbrains.research.kotoed.util

import io.vertx.ext.auth.User

suspend fun User.isAuthorisedAsync(authority: String) = vxa<Boolean> { this.isAuthorized(authority, it) }
