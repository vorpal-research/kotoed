package org.jetbrains.research.kotoed.web.data

import org.jetbrains.research.kotoed.util.Jsonable

object Auth {
    data class LoginResponse(val succeeded: Boolean = true, val error: String? = null): Jsonable
    data class SignUpResponse(val succeeded: Boolean = true, val error: String? = null): Jsonable
}