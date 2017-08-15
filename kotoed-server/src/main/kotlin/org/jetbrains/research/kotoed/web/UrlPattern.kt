package org.jetbrains.research.kotoed.web

object UrlPattern {
    // TODO maybe make parameter names const values
    const val Star = "/*"
    const val Index =  "/"

    object CodeReview {
        const val Index = "/codereview/:id/*"
        const val Capabilities = "/codereview-api/caps/:id"
    }

    object Auth {
        const val Index = "/login"
        const val DoLogin = "/login/doLogin"
        const val DoSignUp = "/login/doSignUp"
        const val LoginDone = "/login/done"
        const val Logout = "/logout"
        const val OAuthStart = "/login/oauth/start/:providerName"
        const val OAuthCallback = "/login/oauth/callback/:providerName"
    }

    object Submission {
        const val Results = "/views/submission/:id/results"
    }
    
    object Comment {
        const val Search = "/views/comment/search"
    }

    object Debug {
        const val Navigation = "/web/debug/navigation"
    }

    const val EventBus = "/eventbus/*"
    const val Static = "/static/*"

    /**
     * Each Any in parameter will be converted to string
     */
    fun reverse(pattern: String, params: Map<String, Any>, star: Any = ""): String {
        var url = pattern
        for ((k, v) in params) {
            url = url.replace(":$k", "$v")
        }

        url = url.replace("*", "$star")

        return url
    }
}
