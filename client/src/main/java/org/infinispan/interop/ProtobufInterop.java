package org.infinispan.interop;

import java.io.IOException;

import org.apache.http.StatusLine;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.ProtoSchemaBuilder;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;

/**
 * Interoperability between REST and Hot Rod using protobuf storage. Demonstrates reading and writing data and queries.
 */
public class ProtobufInterop {

   public static void main(String[] args) throws IOException {
      Configuration configuration = new ConfigurationBuilder().marshaller(new ProtoStreamMarshaller()).build();
      RemoteCacheManager remoteCacheManager = new RemoteCacheManager(configuration);

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
      remoteCache.put("BTC", new CryptoCurrency("Bitcoin", 1));
      remoteCache.put("LTC", new CryptoCurrency("Litecoin", 2));
      remoteCache.put("DOGE", new CryptoCurrency("Dogecoin", 100));

      System.out.println("Cache size after insertion from Hot Rod: " + remoteCache.size());

      // Read from REST
      Executor requestExecutor = Executor.newInstance().auth("dev", "dev");

      String response = requestExecutor
            .execute(Request.Get(String.format("http://%s:%d/rest/%s/%s", "localhost", 8080, cacheName, "BTC")))
            .returnContent().asString();

      System.out.println("/GET 'BTC' from REST: " + response);

      // Write from REST using JSON format
      String newEntry = "{\"_type\": \"CryptoCurrency\",\"description\": \"Tether\",\"rank\": 4}";
      StatusLine status = requestExecutor.execute(
            Request.Post(String.format("http://%s:%d/rest/%s/%s", "localhost", 8080, cacheName, "USDT"))
                  .bodyString(newEntry, ContentType.APPLICATION_JSON))
            .returnResponse().getStatusLine();
      System.out.println("New value inserted via REST, status = " + status);

      // Read from Hot Rod
      CryptoCurrency usdt = remoteCache.get("USDT");
      System.out.println("Reading as Java Object: " + usdt);

      // Query from REST
      String queryResponse = requestExecutor
            .execute(Request.Get("http://localhost:8080/rest/indexed?action=search&query=from%20CryptoCurrency%20order%20by%20rank"))
            .returnContent().asString();

      System.out.println("Query all elements sorted by rank:" + queryResponse);
   }
}
