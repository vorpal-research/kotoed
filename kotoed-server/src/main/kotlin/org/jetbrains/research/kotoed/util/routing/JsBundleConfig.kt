package org.jetbrains.research.kotoed.util.routing

data class JsBundleConfig(val jsBundleName: String?,
                          val cssBundleName: String? = jsBundleName,
                          val vendorJsBundleName: String? = "vendor",
                          val vendorCssBundleName: String? = vendorJsBundleName)