Introduction
------------

groovy-datomic is a very small and simple harness for interacting with
[datomic](http://datomic.com) from 
[the Groovy programming language](http://groovy.codehaus.org).  The
library's primary purpose is to demonstrate how simple it is to use the new
extension mechanism provided by Groovy 2 for extending existing APIs which are
not necessarily written in or for Groovy.  Specifically, the library extends
the datomic API to simplify some specific use cases for interacting with datomic.

Running The Examples
--------------------

The examples may be run by executing tasks provided by the project's
[Gradle](http://gradle.org) build.

To run the Java example:

    ./gradlew javaListTitles

To run the Groovy example:

    ./gradlew groovyListTitles

The Java and Groovy examples are implemented differently (described below) but
should produce the same results.  The demo includes a small database including
information about comic book titles and issues of those titles.  The commands
above will generate simple output which describes the contents of the database.

Extension API Overview
----------------------

The following Java code connects to an in memory datomic database then loads
schema and sample data into the database.

    import java.io.InputStreamReader;
    import java.io.Reader;
    import java.util.List;

    import datomic.Connection;
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
		}
	}

The Groovy extension simplifies that to allow for the following Groovy code to
accomplish the same thing.

    import datomic.Peer

    uri = 'datomic:mem://comics'

    Peer.open(uri, true) {
    	// load the schema and the data
    	load '/comic-schema.dtm'
    	load '/comic-data.dtm'
    }

The Groovy code shown there is invoking a static method on the `datomic.Peer`
class named `open` which accepts 3 arguments.  The first argument is a string
containing a valid database uri.  The second argument is an optional boolean
which indicates if the database should be created or not.  The default is
`false`.  The final argument is a closure which will be executed in a context
which provides access to useful properties and methods for interacting with
datomic. 

The Java code below will send queries to the datomic database to retrieve
information about comics and issues that are in the database and print
the results of those queries to stdout.

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
        }
    }

The code is available in the [src/main/java/comicdb/demo/JavaListTitles.java](groovy-datomic/tree/master/src/main/java/comicdb/demo/JavaListTitles.java) file.

The extension API simplifies that to allow the following Groovy code to
accomplish the same thing.

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

That code is available in the [src/main/groovy/comicdb/demo/GroovyListTitles.groovy](groovy-datomic/tree/master/src/main/groovy/comicdb/demo/GroovyListTitles.groovy) file.

The first query invocation supplies 2 arguments.  The first argument is the
query string and the second argument is a closure which will be executed once
for each set of results returned from the query.  In this case 2 elements are
being returned from the query, `?comic` and `?comicName`.  Those values
are being passed into the closure.

The second query invocation supplies 3 arguments.  The first argument again
is the query string.  In this case the query string expects an input value,
`?comic`. The second argument is a list of all of the query input values. 
The third argument again is a closure which will be executed once for each
set of results returned from the query.

Note that the code above is structured the way that it is to demonstrate
parameterized query usage.  That code could be simplified to use entities
and a single query with something like this:

    import datomic.Peer

    uri = 'datomic:mem://comics'

    Peer.open(uri, true) {
        // load the schema and the data
        load '/comic-schema.dtm'
        load '/comic-data.dtm'

        // retrieve comics
        q('[:find ?comic :where [?comic :comic/name]]') { comic ->
            comicEntity = entity(comic)

            println "\nTitle: ${comicEntity[':comic/name']}"

            issueEntities = new ArrayList(comicEntity[':issue/_comic'])
            issueEntities.sort { it[':issue/number'] }

            issueEntities.each { issueEntity ->
                println "\tIssue #${issueEntity[':issue/number']} - ${issueEntity[':issue/name']}"
            }
        }
    }

The specification at [src/test/groovy/groovy/datomic/extension/QuerySpec.groovy](groovy-datomic/tree/master/src/test/groovy/groovy/datomic/extension/QuerySpec.groovy)
describes basic usage of the extension query API.

API Implementation Summary
--------------------------

The extension api provdes a static `open` method
which is added to the `datomic.Peer` class.

The `Peer` extension API is defined by the `groovy.datomic.extension.DatomicPeerExtension` 
class which is defined in [src/main/groovy/groovy/datomic/extension/DatomicPeerExtension.groovy](groovy-datomic/tree/master/src/main/groovy/groovy/datomic/extension/DatomicPeerExtension.groovy).

    package groovy.datomic.extension

    import datomic.Peer

    class DatomicPeerExtension {

        static open(Peer selfClass, String uri, boolean create = false, Closure closure) {
            if(create) {
                Peer.createDatabase(uri)
            }
            def conn = Peer.connect(uri)
            def helper = new DatomicPeerHelper(conn: conn)
            closure.delegate = helper
            closure()
        }
    }

The extension class provides a single method.  The first argument to the
method, in this case the `Peer` argument, represents which class this
extension method should be added to.  The rest of the arguments are the
arguments that the extension method will accept.  This extension
effectively adds an `open` method to the `Peer` class which accepts a `String`,
an optional boolean and a `Closure` argument.  This allows for the following.

    Peer.open(uri, true) {
        // ....
    }

    Peer.open(uri) {
        // ....
    }

The extension api also adds a convenience method to the
`datomic.query.EntityMap` class which simplifies retrieving attributes from
entities.  In addition to the standard `comicEntity.get(':comic/name')` the extension supports
`comicEntity[':comic/name']`.

The `EntityMap` extension API is defined by the `groovy.datomic.extension.EntityMapExtension` 
class which is defined in [src/main/groovy/groovy/datomic/extension/EntityMapExtension.groovy](groovy-datomic/tree/master/src/main/groovy/groovy/datomic/extension/EntityMapExtension.groovy).

The Groovy runtime needs to know about these extension classes and the way
to make that happen is to define a file named `META-INF/services/org.codehaus.groovy.runtime.ExtensionModule`
which contains metadata about extensions provided by this library.  The file at [src/main/resources/META-INF/services/org.codehaus.groovy.runtime.ExtensionModule](groovy-datomic/tree/master/src/main/resources/META-INF/services/org.codehaus.groovy.runtime.ExtensionModule) looks like this.

    moduleName = DatomicExtension
    moduleVersion = 1.0
    staticExtensionClasses = groovy.datomic.extension.DatomicPeerExtension
    extensionClasses = groovy.datomic.extension.EntityMapExtension

More details about the new extension mechanism are described at http://www.infoq.com/articles/new-groovy-20.

Using The Extension In A Program
--------------------------------

The groovy-datomic library is intended only as a simple example and not
intended to be a full featured library for use in real programs.  However, if
you want to experiment with the extension in your own programs you may do that.
The first step is to build the library.  Build the library's jar with the
following command.

    ./gradlew jar

That should create a jar file at `build/libs/groovy-datomic-1.0.0.BUILD-SNAPSHOT.jar`.
Simply add that jar to any Groovy project's classpath and the extension will be
available to that program.

Note that this extension requires Groovy 2.




