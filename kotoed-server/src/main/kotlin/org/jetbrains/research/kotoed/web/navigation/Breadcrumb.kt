package org.jetbrains.research.kotoed.web.navigation

data class BreadcrumbElement(val text: String, val active: Boolean, val href: String? = null)
open class Breadcrumb(val elems: List<BreadcrumbElement>)

operator fun Breadcrumb.plus(elem: BreadcrumbElement) = Breadcrumb(elems + elem)
operator fun Breadcrumb.plus(otherElems: List<BreadcrumbElement>) = Breadcrumb(elems + otherElems)
