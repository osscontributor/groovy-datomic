/* Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package groovy.datomic.extension

import datomic.Connection
import datomic.Database
import datomic.Entity
import datomic.Peer
import datomic.Util
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

@CompileStatic
class DatomicPeerHelper {
    Connection conn

    void load(String resourceName) {
        DatomicPeerHelper.getResourceAsStream(resourceName).withReader { Reader reader ->
            List tx = Util.readAll(reader).get(0)
            conn.transact(tx).get()
        }
    }

    Database getDb() {
        conn.db()
    }

    Entity entity(arg) {
        db.entity(arg)
    }

    Collection<List<Object>> q(String query, List args = []) {
        q query, args, null
    }

    @CompileDynamic
    Collection<List<Object>> q(String query, List args = [], Closure closure) {
        def results = Peer.q(query, db, *args)
        if(closure) {
            results.each { result ->
                closure(*result)
            }
        }
        results
    }
}

