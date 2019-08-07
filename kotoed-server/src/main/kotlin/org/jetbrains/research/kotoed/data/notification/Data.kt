package org.jetbrains.research.kotoed.data.notification

import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECPublicKeySpec
import org.jetbrains.research.kotoed.util.Jsonable
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Security
import java.util.*

data class CurrentNotificationsQuery(val denizenId: Int) : Jsonable

enum class NotificationService { EMAIL }
enum class MessageFormat { PLAIN, HTML }

data class NotificationMessage(
        val receiverId: Int,
        val service: NotificationService,
        val subject: String,
        val contentsFormat: MessageFormat,
        val contents: String
) : Jsonable

enum class NotificationType {
    NEW_COMMENT,
    COMMENT_REPLIED_TO,
    COMMENT_CLOSED,
    COMMENT_REOPENED,
    NEW_SUBMISSION_RESULTS,
    RESUBMISSION,
    SUBMISSION_UPDATE
}

data class LinkData(
        val entity: String,
        val id: String
) : Jsonable

data class RenderedData(
        val id: Int,
        val contents: String,
        val linkTo: LinkData
) : Jsonable

data class WebNotificationSubscriptionKeys(
        val p256dh: String,
        val auth: String
) : Jsonable

private object BouncyCastleAccess {
    val provider: BouncyCastleProvider = BouncyCastleProvider()

    init {
        Security.addProvider(provider)
    }

    val keyFactory = KeyFactory.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME)
}

data class WebNotificationSubscription(
        val denizenId: Int,
        val endpoint: String,
        val key: String,
        val auth: String
) : Jsonable {

    fun getKeyBytes() = Base64.getDecoder().decode(key)
    fun getAuthBytes() = Base64.getDecoder().decode(auth)

    fun getUserPublicKey(): PublicKey {
        val kf = BouncyCastleAccess.keyFactory
        val ecSpec = ECNamedCurveTable.getParameterSpec("secp256r1")
        val point = ecSpec.curve.decodePoint(getKeyBytes())
        val pubSpec = ECPublicKeySpec(point, ecSpec)

        return kf.generatePublic(pubSpec)
    }
}
