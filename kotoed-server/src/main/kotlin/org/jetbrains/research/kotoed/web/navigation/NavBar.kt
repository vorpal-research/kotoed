package org.jetbrains.research.kotoed.web.navigation

import io.vertx.ext.auth.User
import org.jetbrains.research.kotoed.util.isAuthorisedAsync
import org.jetbrains.research.kotoed.web.UrlPattern
import org.jetbrains.research.kotoed.web.auth.Authority

val NavBarContextName = "navbar"

interface NavBarElement {
    val type: String // To make things easier for template engine. It cannot do instanceof
}

data class NavBarLink(val text: String, val href: String = "#") : NavBarElement {
    override val type = "link"
}

// Only one nesting level, sorry
data class NavBarMenu(val text: String, val children: List<NavBarLink>) : NavBarElement {
    override val type = "menu"
}

data class NavBar(val leftElems: List<NavBarElement> = listOf(),
                  val rightElems: List<NavBarElement> = listOf(),
                  val rootHref: String = "#")


suspend private fun User?.getUtilities() =
        when {
            this == null -> listOf()
            isAuthorisedAsync(Authority.Teacher) -> listOf(
                    NavBarLink("Comment search", UrlPattern.Comment.Search),
                    NavBarLink("Whatever", UrlPattern.NotImplemented))
            else -> listOf()
        }


suspend fun kotoedNavBar(user: User?): NavBar {
    val utils = user.getUtilities()
    val leftElems =
            if (utils.isNotEmpty())
                listOf(NavBarMenu("Utilities", children = utils))
            else
                listOf()

    val rightElems =
            if (user == null)
                listOf(NavBarLink("Anonymous"))
            else
                listOf(
                        NavBarMenu(
                                user.principal()?.getString("denizenId") ?:
                                        throw IllegalArgumentException("I can work only with UavUsers"),
                                listOf(
                                        NavBarLink("Profile", UrlPattern.NotImplemented),
                                        NavBarLink("Logout", UrlPattern.Auth.Logout)
                                )))
    return NavBar(leftElems = leftElems, rightElems = rightElems, rootHref = UrlPattern.Index)
}