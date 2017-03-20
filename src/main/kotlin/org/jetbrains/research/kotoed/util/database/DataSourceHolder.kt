package org.jetbrains.research.kotoed.util.database

import com.zaxxer.hikari.HikariDataSource
import io.vertx.core.Vertx
import org.jetbrains.research.kotoed.util.getSharedLocal
import javax.sql.DataSource

data class KotoedDataSource(val ds: DataSource, val url: String) : DataSource by ds

fun Vertx.getSharedDataSource(name: String, url: String, username: String, password: String): KotoedDataSource =
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
