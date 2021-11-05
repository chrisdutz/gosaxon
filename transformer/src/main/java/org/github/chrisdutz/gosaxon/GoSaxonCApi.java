/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.github.chrisdutz.gosaxon;

import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;

import java.util.concurrent.ConcurrentHashMap;

public class GoSaxonCApi {

    public static ConcurrentHashMap<Long, GoSaxonTransformer> instances = new ConcurrentHashMap<>();

    @CEntryPoint(name = "Java_org_github_chrisdutz_gosaxon_GoSaxonCApi_start")
    public static CCharPointer start(@CEntryPoint.IsolateThreadContext long isolateId) {
        GoSaxonTransformer instance = new GoSaxonTransformer(false);
        instances.put(isolateId, instance);

        int[] ports = instance.getPorts();
        String response = String.format("%08X", ports[0]) + String.format("%08X", ports[1]) + String.format("%08X", ports[2]);
        return CTypeConversion.toCString(response).get();
    }

}
