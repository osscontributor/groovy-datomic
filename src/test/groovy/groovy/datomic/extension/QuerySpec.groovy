package groovy.datomic.extension

import datomic.Peer
import spock.lang.Shared
import spock.lang.Specification

class QuerySpec extends Specification {

    @Shared
    def comicDbUri = 'datomic:mem://comics'

    def setupSpec() {
        Peer.open(comicDbUri, true) {
            load '/comic-schema.dtm'
            load '/comic-data.dtm'
        }
    }

    void 'Test query with iteration closure argument'() {
        when:
        def comicNames = [] as SortedSet
        Peer.open(comicDbUri) {

            q('[:find ?comicName :where [?comic :comic/name ?comicName]]') { comicName ->
                comicNames << comicName
            }
        }
        
        then:
        assert comicNames == ['Batman',
                              'Batman And Robin',
                              'Batman Incorporated',
                              'Batman: The Dark Knight',
                              'Before Watchmen: Comedian',
                              'Before Watchmen: Minutemen',
                              'Before Watchmen: Nite Owl',
                              'Before Watchmen: Silk Spectre',
                              'Detective Comics',
                              'Earth 2',
                              'Watchmen'] as SortedSet
    }

    void 'Test query return value'() {
        when:
        def comicNames = [] as SortedSet
        Peer.open(comicDbUri) {

            def results = q('[:find ?comicName :where [?comic :comic/name ?comicName]]')
            results.each { result ->
                comicNames << result[0]
            }
        }
        
        then:
        assert comicNames == ['Batman',
                              'Batman And Robin',
                              'Batman Incorporated',
                              'Batman: The Dark Knight',
                              'Before Watchmen: Comedian',
                              'Before Watchmen: Minutemen',
                              'Before Watchmen: Nite Owl',
                              'Before Watchmen: Silk Spectre',
                              'Detective Comics',
                              'Earth 2',
                              'Watchmen'] as SortedSet
    }

    void 'Test entity method'() {
        when:
        def issueNumber
        Peer.open(comicDbUri) {

            def results = q('[:find ?issue :in $ ?issueName :where [?issue :issue/name ?issueName]]', ['The Final Curtain']) as List
            def issueEntity = entity(results[0][0])
            issueNumber = issueEntity[':issue/number']
        }

        then:
        7 == issueNumber
    }
}
