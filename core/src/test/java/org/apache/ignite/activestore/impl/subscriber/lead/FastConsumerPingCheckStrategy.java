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

package org.apache.ignite.activestore.impl.subscriber.lead;

/**
 * @author Evgeniy_Ignatiev
 * @since 12/30/2016 12:53 PM
 */
public class FastConsumerPingCheckStrategy extends DefaultConsumerPingCheckStrategy {
    public static final long FAST_CONSUMER_TIMEOUT = 3_000;

    public FastConsumerPingCheckStrategy() {
        super(FAST_CONSUMER_TIMEOUT);
    }
}
