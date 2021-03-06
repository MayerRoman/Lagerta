/*
 * Copyright (c) 2017. EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.lagerta.base.jdbc;

import com.epam.lagerta.resources.H2DataBaseServer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class H2HikariDataSource {

    private static final String CONNECTION_STR_PATTERN = "jdbc:h2:tcp://%s:%s/mem:%s";
    private static final String DRIVER_NAME = "org.h2.Driver";
    private static final long CONNECTION_TIMEOUT = 5000L;
    private static final int MAX_POOL_SIZE = 5;

    public static HikariDataSource create(String dbName) {
        String dbUrl = String.format(CONNECTION_STR_PATTERN, H2DataBaseServer.HOST, H2DataBaseServer.PORT, dbName);

        HikariConfig config = new HikariConfig();
        config.setDriverClassName(DRIVER_NAME);
        config.setJdbcUrl(dbUrl);
        config.setUsername("");
        config.setPassword("");
        config.setConnectionTimeout(CONNECTION_TIMEOUT);
        config.setMaximumPoolSize(MAX_POOL_SIZE);
        return new HikariDataSource(config);
    }
}
