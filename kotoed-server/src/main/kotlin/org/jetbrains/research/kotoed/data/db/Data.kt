package org.jetbrains.research.kotoed.data.db

import org.jetbrains.research.kotoed.util.Jsonable

/* UserAuthVerticle */

data class SignUpMsg(val denizenId: String, val password: String) : Jsonable
data class LoginMsg(val denizenId: String, val password: String) : Jsonable
data class InfoMsg(val denizenId: String) : Jsonable
