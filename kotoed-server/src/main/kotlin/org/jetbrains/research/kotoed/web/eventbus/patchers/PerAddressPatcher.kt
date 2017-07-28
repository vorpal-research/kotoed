package org.jetbrains.research.kotoed.web.eventbus.patchers

import io.vertx.ext.web.handler.sockjs.BridgeEvent
import org.jetbrains.research.kotoed.util.get

class PerAddressPatcher(vararg filters: Pair<String, BridgeEventPatcher>) : BridgeEventPatcher {

    val patchersByAddress = mapOf(*filters)

    override suspend fun patch(be: BridgeEvent) {
        be.rawMessage?.get("address")?.let {
            patchersByAddress[it]?.patch(be)?.also {
                logPatch(be)
            }
        }
    }

}