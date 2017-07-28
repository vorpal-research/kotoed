package org.jetbrains.research.kotoed.web.eventbus

import io.vertx.ext.web.handler.sockjs.BridgeEventType
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.web.eventbus.filters.*
import org.jetbrains.research.kotoed.web.eventbus.patchers.BridgeEventPatcher

val HarmlessTypes =
        ByTypes(BridgeEventType.RECEIVE,
                BridgeEventType.SOCKET_IDLE,
                BridgeEventType.SOCKET_PING,
                BridgeEventType.SOCKET_CREATED)

val Send = ByType(BridgeEventType.SEND)

val KotoedPerAddress = PerAddress(
        Address.Api.Submission.Code.Read to BridgeEventFilter.permissive(),
        Address.Api.Submission.Code.List to BridgeEventFilter.permissive(),
        Address.Api.Submission.Comments to BridgeEventFilter.permissive()
)

val KotoedFilter = LoginRequired and (HarmlessTypes or (Send and KotoedPerAddress))

val KotoedPatcher = BridgeEventPatcher.noop()