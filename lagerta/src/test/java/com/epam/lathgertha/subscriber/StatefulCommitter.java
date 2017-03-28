/*
 *  Copyright 2017 EPAM Systems.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.epam.lathgertha.subscriber;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StatefulCommitter implements Committer {

    private final Map<Object, Object> writtenKeysAndValues = new ConcurrentHashMap<>();

    @Override
    public void commit(List<String> names, List<List<?>> keys, List<List<?>> values) {
        for (int i = 0; i < keys.size(); i++) {
            List<?> keysList = keys.get(i);
            List<?> valuesList = values.get(i);
            for (int j = 0; j < keysList.size(); j++) {
                writtenKeysAndValues.put(keysList.get(j), valuesList.get(j));
            }
        }
    }

    public Map<Object, Object> getWrittenKeysAndValues() {
        return writtenKeysAndValues;
    }
}