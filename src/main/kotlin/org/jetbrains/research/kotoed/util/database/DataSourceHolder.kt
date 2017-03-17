package org.jetbrains.research.kotoed.util.database

import io.vertx.core.shareddata.Shareable
import javax.sql.DataSource

data class DataSourceHolder(val delegate: DataSource): DataSource by delegate, Shareable
