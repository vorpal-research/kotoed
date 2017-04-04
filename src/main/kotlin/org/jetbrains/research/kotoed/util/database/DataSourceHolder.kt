package org.jetbrains.research.kotoed.util.database

import com.zaxxer.hikari.HikariDataSource
import io.vertx.core.Vertx
import org.jetbrains.research.kotoed.config.Config
import org.jetbrains.research.kotoed.util.getSharedLocal
import javax.sql.DataSource

data class KotoedDataSource(val ds: DataSource, val url: String) : DataSource by ds

fun Vertx.getSharedDataSource(name: String = Config.Debug.DB.DataSourceId,
                              url: String = Config.Debug.DB.Url,
                              username: String = Config.Debug.DB.User,
                              password: String = Config.Debug.DB.Password): KotoedDataSource =
        getSharedLocal(name) {
            KotoedDataSource(
                    ds = HikariDataSource().apply {
                        this.username = username
                        this.password = password
                        this.jdbcUrl = url
                    },
                    url = url
            )
        }
