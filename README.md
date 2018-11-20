## Infinispan Remote Endpoints Interoperability demo

This repo contains a series of samples to demonstrate how to access data from Infinispan Server from multiple clients
simultaneously (REST, Hot Rod, Memcached), in different formats (text, JSON, binary, POJO).


To run the demos, do a ```mvn clean install``` first, then launch the server with:

```
docker run -v $PWD/entities/target:/opt/jboss/infinispan-server/standalone/deployments \
           -v $PWD/client/config:/opt/jboss/infinispan-server/standalone/configuration/demo \
           -p 8080:8080 -p 11211-11222:11211-11222 \
           -e "APP_USER=dev" -e "APP_PASS=dev" \
           -e "JAVA_OPTS=-Dinfinispan.deserialization.whitelist.regexps=.\*" \
           --name infinispan -d jboss/infinispan-server:9.4.1.Final demo/clustered.xml
```

This instructs the server to be started with credentials ```dev/dev```, exposing ports ```8080``` and ```11211``` to ```11222``` to ```localhost``` and using the configuration file ```client/config/clustered.xml```. It also enables all deserialization in the server.

### Running the demos

To verify interop between all endpoints using JSON, including querying and indexing, while storing protobuf in the cache, run:

_**NOTE:** This is the recommended setup that allows the best degree of interoperability with all clients._


```
mvn -pl client exec:java -Dexec.mainClass=org.infinispan.interop.ProtobufInterop -Dexec.cleanupDaemonThreads=false
```


To verify reading/writing data in multiple formats when storing JSON in the server, run:

```
mvn -pl client exec:java -Dexec.mainClass=org.infinispan.interop.JsonInterop -Dexec.cleanupDaemonThreads=false
```

To verify reading/writing data in multiple formats when storing POJOs in the server, run:

```
mvn -pl client exec:java -Dexec.mainClass=org.infinispan.interop.ObjectStorageInterop -Dexec.cleanupDaemonThreads=false
```

To verify reading/writing data in multiple formats when storing binary (marshalled Java objects) in the server, run:

```
mvn -pl client exec:java -Dexec.mainClass=org.infinispan.interop.MarshalledStorageInterop -Dexec.cleanupDaemonThreads=false
```

To verify interop between REST, Hot Rod and Memcached endpoints when storing strings in the cache, run:

 ```
 mvn -pl client exec:java -Dexec.mainClass=org.infinispan.interop.StringInterop -Dexec.cleanupDaemonThreads=false
 ```

### Running all

Execute ```run-all.sh``` to launch the server and run all the demos sequentially.
