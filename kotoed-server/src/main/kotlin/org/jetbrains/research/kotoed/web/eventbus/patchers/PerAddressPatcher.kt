package org.jetbrains.research.kotoed.web.eventbus.patchers

import io.vertx.ext.web.handler.sockjs.BridgeEvent
import org.jetbrains.research.kotoed.util.get
import org.jetbrains.research.kotoed.web.eventbus.filters.BridgeEventFilter

class PerAddressPatcher(vararg filters: Pair<String, BridgeEventPatcher>): BridgeEventPatcher {

    val patchersByAddress = mapOf(*filters)

    override fun patch(be: BridgeEvent) {
        be.rawMessage?.get("address")?.let {
            patchersByAddress[it]?.patch(be)?.also {
                logPatch(be)
            }
        }
    }

}