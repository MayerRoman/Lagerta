/*
 * Copyright (c) 2017. EPAM Systems
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

package org.apache.ignite.activestore.suites;

import org.apache.ignite.activestore.cluster.XmlOneProcessClusterManager;
import org.apache.ignite.activestore.rules.TestResourceFactory;
import org.apache.ignite.activestore.rules.TestResources;
import org.junit.ClassRule;

/**
 * Suite which runs all tests with in-memory based cache store and XML-based config.
 */
public class ActiveCacheStoreXmlConfigInMemoryTestSuite extends BasicTestSuite {
    /** */
    @ClassRule
    public static TestResources resource = TestResourceFactory.getResource();

    /** */
    static {
        resource.setClusterManager(new XmlOneProcessClusterManager("ignite-sample-inmemory-config.xml"));
    }
}
