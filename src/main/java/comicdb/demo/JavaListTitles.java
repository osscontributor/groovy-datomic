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
package comicdb.demo;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import datomic.Connection;
import datomic.Database;
import datomic.Peer;
import datomic.Util;

public class JavaListTitles {

	public static void main(String[] args) throws Exception {
		String uri = "datomic:mem://comics";
		Peer.createDatabase(uri);
		Connection conn = Peer.connect(uri);
		
		// load the schema and the data
		Reader reader = new InputStreamReader(JavaListTitles.class.getResourceAsStream("/comic-schema.dtm"));
		List<?> transaction = (List<?>) Util.readAll(reader).get(0);
		conn.transact(transaction).get();
		reader = new InputStreamReader(JavaListTitles.class.getResourceAsStream("/comic-data.dtm"));
		transaction = (List<?>) Util.readAll(reader).get(0);
		conn.transact(transaction).get();
		
		Database db = conn.db();
		
		// retrieve comics
		Collection<List<Object>> comicResults = Peer.q("[:find ?comic ?comicName :where [?comic :comic/name ?comicName]]", db);
		for(List<?> comicResult : comicResults) {
			Object comic = comicResult.get(0);
			Object comicName = comicResult.get(1);
			System.out.println("\nTitle: " + comicName);
			
			// A map to sort issues by issue number
			Map<Long, String> issues = new TreeMap<Long, String>();
			
			// retrieve issues for this comic
			Collection<List<Object>> issueResults = Peer.q("[:find ?name ?number :in $ ?comic :where [?i :issue/name ?name][?i :issue/number ?number][?i :issue/comic ?comic]]", db, comic);
			for(List<?> issueResult: issueResults) {
				String issueName = (String) issueResult.get(0);
				Long issueNumber = (Long) issueResult.get(1);
				issues.put(issueNumber, issueName);
			}

			for(Map.Entry<Long, String> entry : issues.entrySet()) {
				Long issueNumber = entry.getKey();
				String issueName = entry.getValue();
				System.out.println("\tIssue #" + issueNumber + " - " + issueName);
			}
		}
        Peer.shutdown(true);
	}
}
