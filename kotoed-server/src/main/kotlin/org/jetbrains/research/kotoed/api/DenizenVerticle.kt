package org.jetbrains.research.kotoed.api

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.data.api.*
import org.jetbrains.research.kotoed.data.db.ComplexDatabaseQuery
import org.jetbrains.research.kotoed.data.db.LoginMsg
import org.jetbrains.research.kotoed.data.db.setPageForQuery
import org.jetbrains.research.kotoed.data.db.textSearch
import org.jetbrains.research.kotoed.database.Tables
import org.jetbrains.research.kotoed.database.Tables.DENIZEN_TEXT_SEARCH
import org.jetbrains.research.kotoed.database.Tables.PROFILE
import org.jetbrains.research.kotoed.database.enums.DiffModePreference
import org.jetbrains.research.kotoed.database.tables.records.DenizenRecord
import org.jetbrains.research.kotoed.database.tables.records.OauthProfileRecord
import org.jetbrains.research.kotoed.database.tables.records.ProfileRecord
import org.jetbrains.research.kotoed.db.condition.lang.formatToQuery
import org.jetbrains.research.kotoed.eventbus.Address
import org.jetbrains.research.kotoed.util.*

@AutoDeployable
@CleanupJsonFields(arrayOf("password", "initiatorPassword", "newPassword"))
class DenizenVerticle: AbstractKotoedVerticle() {

    @JsonableEventBusConsumerFor(Address.Api.Denizen.Create)
    suspend fun handleCreate(denizen: DenizenRecord): DbRecordWrapper<DenizenRecord> =
            DbRecordWrapper(dbCreateAsync(denizen), VerificationData.Processed)

    @JsonableEventBusConsumerFor(Address.Api.Denizen.Read)
    suspend fun handleRead(denizen: DenizenRecord): DbRecordWrapper<DenizenRecord> =
            DbRecordWrapper(dbFetchAsync(denizen), VerificationData.Processed)

    @JsonableEventBusConsumerFor(Address.Api.Denizen.Profile.Read)
    suspend fun handleProfileRead(denizen: DenizenRecord): ProfileInfo {
        // FIXME belyaev: do this properly

        val dbDenizen = dbFetchAsync(denizen)
        val profile = dbFindAsync(ProfileRecord().apply { denizenId = denizen.id }).firstOrNull()

        val oauth = dbQueryAsync(
                ComplexDatabaseQuery(Tables.OAUTH_PROFILE)
                .join(Tables.OAUTH_PROVIDER)
                .find(OauthProfileRecord().apply { denizenId = denizen.id })
        ).map { (it.safeNav("oauth_provider", "name") as String) to it.getString("oauth_user_id") }
                .toMap()

        return ProfileInfo(
                id = denizen.id,
                denizenId = dbDenizen.denizenId,
                email = dbDenizen.email,
                oauth = oauth,
                firstName = profile?.firstName,
                lastName = profile?.lastName,
                group = profile?.groupId,
                emailNotifications = profile?.emailNotifications ?: false,
                diffModePreference = profile?.diffModePreference ?: DiffModePreference.PREVIOUS_CLOSED
        )
    }

    @JsonableEventBusConsumerFor(Address.Api.Denizen.Profile.Update)
    suspend fun handleProfileUpdate(update: ProfileInfoUpdate): Unit {
        // FIXME belyaev: do this properly

        if(update.email != null) {
            dbUpdateAsync(DenizenRecord().apply { id = update.id; email = update.email })
        }

        val profile = dbFindAsync(ProfileRecord().apply{ denizenId = update.id }).firstOrNull();

        val newProf = ProfileRecord().apply {
            denizenId = update.id
            update.firstName?.let { firstName = it }
            update.lastName?.let { lastName = it }
            update.group?.let { groupId = it }
            update.emailNotifications?.let { emailNotifications = it }
            update.diffModePreference?.let { diffModePreference = it }
        }

        if (profile != null) {
            dbUpdateAsync(newProf.apply{ id = profile.id})
        } else {
            dbCreateAsync(newProf)
        }
    }

    @JsonableEventBusConsumerFor(Address.Api.Denizen.Profile.UpdatePassword)
    suspend fun handlePasswordUpdate(update: PasswordChangeRequest) {
        run<Unit> { sendJsonableAsync(Address.User.Auth.Login, LoginMsg(update.initiatorDenizenId, update.initiatorPassword)) }
        val target = dbFetchAsync(DenizenRecord().apply{ id = update.targetId })
        run<Unit> { sendJsonableAsync(Address.User.Auth.SetPassword, LoginMsg(target.denizenId, update.newPassword)) }
    }


    @JsonableEventBusConsumerFor(Address.Api.Denizen.Search)
    suspend fun handleSearch(query: SearchQuery): JsonArray {
        val req: List<JsonObject> = dbQueryAsync(DENIZEN_TEXT_SEARCH) {
            setPageForQuery(query)
            rjoin(PROFILE, field = "denizen_id")
            textSearch(query.text)
        }

        return JsonArray(req)
    }

    @JsonableEventBusConsumerFor(Address.Api.Denizen.SearchCount)
    suspend fun handleSearchCount(query: SearchQuery): CountResponse =
            dbCountAsync(DENIZEN_TEXT_SEARCH) {
                textSearch(query.text)
            }
}
