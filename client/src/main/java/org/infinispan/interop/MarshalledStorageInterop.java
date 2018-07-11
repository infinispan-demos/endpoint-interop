package org.infinispan.interop;

import static org.apache.http.HttpHeaders.ACCEPT;
import static org.infinispan.commons.dataconversion.MediaType.APPLICATION_JSON_TYPE;

import java.io.IOException;

import org.apache.http.StatusLine;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

/**
 * Interop when storing binary content (marshalled objects) in the server
 */
public class MarshalledStorageInterop {

   public static void main(String[] args) throws IOException {
      Configuration configuration = new ConfigurationBuilder().build();
      RemoteCacheManager remoteCacheManager = new RemoteCacheManager(configuration);

      // The pojo-cache is configured with 'application/x-jboss-marshalling' for both keys and values
      String cacheName = "marshalled-pojo-cache";

      RemoteCache<Integer, CryptoCurrency> remoteCache = remoteCacheManager.getCache(cacheName);

      // Write from Hot Rod
      remoteCache.put(10, new CryptoCurrency("Bitcoin", 1));
      remoteCache.put(20, new CryptoCurrency("Litecoin", 2));
      remoteCache.put(30, new CryptoCurrency("Dogecoin", 100));

      System.out.println("Cache size after insertion = " + remoteCache.size());

      Executor requestExecutor = Executor.newInstance().auth("dev", "dev");

      // Read from REST as JSON
      String response = requestExecutor
            .execute(Request.Get(String.format("http://%s:%d/rest/%s/%s", "localhost", 8080, cacheName, 20))
                  .addHeader(ACCEPT, APPLICATION_JSON_TYPE)
                  .addHeader("Key-Content-Type", "application/x-java-object;type=java.lang.Integer"))
            .returnContent().asString();

      System.out.println("/GET `LTC` as JSON: " + response);

      // Write a new value in JSON format
      // Write from REST in JSON format
      String json = "{\"_type\":\"org.infinispan.interop.CryptoCurrency\",\"description\":\"Monero\",\"rank\":20}";
      StatusLine status = requestExecutor.execute(
            Request.Post(String.format("http://%s:%d/rest/%s/%s", "localhost", 8080, cacheName, 40))
                  .addHeader("key-content-type","application/x-java-object; type=java.lang.Integer")
                  .bodyString(json, ContentType.APPLICATION_JSON))
            .returnResponse().getStatusLine();

      System.out.println("Inserted new entry in JSON format: " + json + ", status code: " + status);

      // Read from Hot Rod the data inserted via REST
      CryptoCurrency eth = remoteCache.get(40);
      System.out.println("Reading as Java Object: " + eth);
   }
}



