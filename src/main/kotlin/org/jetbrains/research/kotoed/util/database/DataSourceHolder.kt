package org.jetbrains.research.kotoed.util.database

import com.zaxxer.hikari.HikariDataSource
import io.vertx.core.Vertx
import io.vertx.core.shareddata.Shareable
import org.jetbrains.research.kotoed.util.getSharedLocal
import javax.sql.ConnectionPoolDataSource
import javax.sql.DataSource
import javax.xml.crypto.Data

fun Vertx.getSharedDataSource(name: String, url: String, username: String, password: String): DataSource =
        getSharedLocal(name){
            HikariDataSource().apply {
                this.username = username
                this.password = password
                this.jdbcUrl = url
            }
        }
