set -e -o pipefail -o errtrace -o functrace

function wait_for_ispn() {
  until `docker exec -t infinispan /opt/jboss/infinispan-server/bin/ispn-cli.sh -c ":read-attribute(name=server-state)"  | grep -q running`; do
    sleep 3
    echo "Waiting for the server to start..."
  done
}

mvn clean install
docker run -v $PWD/entities/target:/opt/jboss/infinispan-server/standalone/deployments \
           -v $PWD/client/config:/opt/jboss/infinispan-server/standalone/configuration/demo \
           -p 8080:8080 -p 11211-11222:11211-11222 \
           -e "APP_USER=dev" -e "APP_PASS=dev" -e "JAVA_OPTS=-Dinfinispan.deserialization.whitelist.regexps=.\*" \
           --name infinispan -d jboss/infinispan-server:9.4.0.CR3 demo/clustered.xml

wait_for_ispn

mvn -pl client exec:java -Dexec.mainClass=org.infinispan.interop.JsonInterop -Dexec.cleanupDaemonThreads=false
mvn -pl client exec:java -Dexec.mainClass=org.infinispan.interop.MarshalledStorageInterop -Dexec.cleanupDaemonThreads=false
mvn -pl client exec:java -Dexec.mainClass=org.infinispan.interop.ObjectStorageInterop -Dexec.cleanupDaemonThreads=false
mvn -pl client exec:java -Dexec.mainClass=org.infinispan.interop.ProtobufInterop -Dexec.cleanupDaemonThreads=false
mvn -pl client exec:java -Dexec.mainClass=org.infinispan.interop.StringInterop -Dexec.cleanupDaemonThreads=false

docker rm -f infinispan