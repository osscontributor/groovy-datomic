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
package comicdb.demo

import datomic.Peer

uri = 'datomic:mem://comics'

Peer.open(uri, true) {
	// load the schema and the data
	load '/comic-schema.dtm'
	load '/comic-data.dtm'

	// retrieve comics
	q('[:find ?comic ?comicName :where [?comic :comic/name ?comicName]]') { comic, comicName ->
		println "\nTitle: ${comicName}"

		// A map to sort issues by issue number
		issues = new TreeMap()

		// retrieve issues for this comic
		q('[:find ?name ?number :in $ ?comic :where [?i :issue/name ?name][?i :issue/number ?number][?i :issue/comic ?comic]]', [comic]) { name, number ->
			issues[number] = name
		}
		issues.each { number, name ->
			println "\tIssue #${number} - ${name}"
		}
	}
}
Peer.shutdown(true)

