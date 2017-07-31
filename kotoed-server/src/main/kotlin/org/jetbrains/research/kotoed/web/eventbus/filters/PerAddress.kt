package org.jetbrains.research.kotoed.web.eventbus.filters

import io.vertx.ext.web.handler.sockjs.BridgeEvent
import io.vertx.ext.web.handler.sockjs.PermittedOptions
import org.jetbrains.research.kotoed.util.get

class PerAddress(
        vararg filters: Pair<String, BridgeEventFilter>,
        private val allowUnknown: Boolean = false) : BridgeEventFilter {

    private val filtersByAddress = mapOf(*filters)

    override suspend fun isAllowed(be: BridgeEvent): Boolean {
        val address = be.rawMessage?.get("address")

        return (if (address in filtersByAddress)
            filtersByAddress[address]!!.isAllowed(be)
        else
            allowUnknown).also { logResult(be, it) }
    }

    override fun toString(): String {
        return "PerAddress(allowUnknown=$allowUnknown, filtersByAddress=$filtersByAddress)"
    }

    fun makePermittedOptions(): Iterable<PermittedOptions> =
            filtersByAddress.keys.map { PermittedOptions().setAddress(it) }

}

