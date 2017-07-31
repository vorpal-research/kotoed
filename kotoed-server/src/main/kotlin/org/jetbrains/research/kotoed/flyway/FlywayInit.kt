package org.jetbrains.research.kotoed.flyway

import io.vertx.core.Vertx
import org.flywaydb.core.Flyway
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.util.database.getSharedDataSource

object FlywayInit {
    fun doit(vertx: Vertx) {
        Flyway().run {
            dataSource = vertx.getSharedDataSource(
                    Config.Debug.Database.DataSourceId,
                    Config.Debug.Database.Url,
                    Config.Debug.Database.User,
                    Config.Debug.Database.Password
            )
            migrate()
        }
    }
}
