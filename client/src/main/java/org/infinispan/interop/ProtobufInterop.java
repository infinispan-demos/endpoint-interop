package org.infinispan.interop;

import static org.infinispan.interop.Utils.Action.QUERY;
import static org.infinispan.interop.Utils.Action.READ;
import static org.infinispan.interop.Utils.Action.WRITE;
import static org.infinispan.interop.Utils.Endpoint.HOT_ROD;
import static org.infinispan.interop.Utils.Endpoint.MEMCACHED;
import static org.infinispan.interop.Utils.Endpoint.REST;
import static org.infinispan.interop.Utils.logAction;

import java.net.InetSocketAddress;
import java.util.List;

import org.apache.http.StatusLine;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.Search;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.ProtoSchemaBuilder;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;

import net.spy.memcached.MemcachedClient;

/**
 * Interoperability between REST, Hot Rod and memcached using protobuf storage. Demonstrates reading and writing data and queries.
 */
public class ProtobufInterop {

   public static void main(String[] args) throws Exception {
      Configuration configuration = new ConfigurationBuilder().marshaller(new ProtoStreamMarshaller()).build();
      RemoteCacheManager remoteCacheManager = new RemoteCacheManager(configuration);

      MemcachedClient memcachedClient = new MemcachedClient(new InetSocketAddress("localhost", 11214));

      SerializationContext serializationContext = ProtoStreamMarshaller.getSerializationContext(remoteCacheManager);

      // Obtain the 'indexed' cache from the server
      String cacheName = "indexed";

      // Use the proto schema builder to generate the proto file from the annotated entity class.
      ProtoSchemaBuilder protoSchemaBuilder = new ProtoSchemaBuilder();
      String schemaFile = protoSchemaBuilder.fileName("crypto.proto").addClass(CryptoCurrency.class).build(serializationContext);

      // Registers the schema in the client and in the server.
      serializationContext.registerProtoFiles(FileDescriptorSource.fromString("crypto.proto", schemaFile));
      RemoteCache<String, String> metadataCache = remoteCacheManager.getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);
      metadataCache.put("crypto.proto", schemaFile);

      RemoteCache<String, CryptoCurrency> remoteCache = remoteCacheManager.getCache(cacheName);

      // Write from Hot Rod using POJOs
      CryptoCurrency bitcoin = new CryptoCurrency("Bitcoin", 1);
      CryptoCurrency litecoin = new CryptoCurrency("Litecoin", 2);
      CryptoCurrency dogecoin = new CryptoCurrency("Dogecoin", 100);

      remoteCache.put("BTC", bitcoin);
      logAction(WRITE, "BTC", bitcoin, HOT_ROD);

      remoteCache.put("LTC", litecoin);
      logAction(WRITE, "LTC", litecoin, HOT_ROD);

      remoteCache.put("DOGE", dogecoin);
      logAction(WRITE, "DOGE", dogecoin, HOT_ROD);

      System.out.println("Cache size after insertion from Hot Rod: " + remoteCache.size());

      // Read from REST
      Executor requestExecutor = Executor.newInstance().auth("dev", "dev");

      String response = requestExecutor
            .execute(Request.Get(String.format("http://%s:%d/rest/%s/%s", "localhost", 8080, cacheName, "BTC")))
            .returnContent().asString();

      logAction(READ, "BTC", response, REST);

      // Write from REST using JSON format
      String newEntry = "{\"_type\": \"CryptoCurrency\",\"description\": \"Tether\",\"rank\": 4}";
      StatusLine status = requestExecutor.execute(
            Request.Post(String.format("http://%s:%d/rest/%s/%s", "localhost", 8080, cacheName, "USDT"))
                  .bodyString(newEntry, ContentType.APPLICATION_JSON))
            .returnResponse().getStatusLine();
      logAction(WRITE, "USDT", newEntry, REST);
      System.out.println("Status = " + status);

      // Read from Hot Rod
      CryptoCurrency usdt = remoteCache.get("USDT");
      logAction(READ, "USDT", usdt, HOT_ROD);

      // Query from REST
      String queryResponse = requestExecutor
            .execute(Request.Get("http://localhost:8080/rest/indexed?action=search&query=from%20CryptoCurrency%20order%20by%20rank"))
            .returnContent().asString();
      logAction(QUERY, "all elements sorted by rank", queryResponse, REST);

      // Write entry from memcached. The memcached endopoint is configured with "client-encoding='application/json'" so that memcached clients
      // can use json UTF-8 content
      String eth = "{\"_type\": \"CryptoCurrency\",\"description\": \"Ethereum\",\"rank\": 7}";
      memcachedClient.set("ETH", -1, eth).get();
      logAction(WRITE, "ETH", eth, MEMCACHED);

      // Query the newly inserted entry via Hot Rod
      QueryFactory queryFactory = Search.getQueryFactory(remoteCache);
      Query q = queryFactory.create("FROM CryptoCurrency c where description:'eth*'");
      List<CryptoCurrency> results = q.list();

      logAction(QUERY, "entry inserted from memcached", results.iterator().next(), HOT_ROD);

      remoteCacheManager.stop();
      memcachedClient.shutdown();
   }
}
