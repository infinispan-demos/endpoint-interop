package org.infinispan.interop;

import static org.apache.http.HttpHeaders.ACCEPT;
import static org.infinispan.commons.dataconversion.MediaType.APPLICATION_JSON_TYPE;
import static org.infinispan.interop.Utils.Action.READ;
import static org.infinispan.interop.Utils.Action.WRITE;
import static org.infinispan.interop.Utils.Endpoint.HOT_ROD;
import static org.infinispan.interop.Utils.Endpoint.MEMCACHED;
import static org.infinispan.interop.Utils.Endpoint.REST;
import static org.infinispan.interop.Utils.logAction;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.http.StatusLine;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

import net.spy.memcached.MemcachedClient;

/**
 * Interop when storing binary content (marshalled objects) in the server
 */
public class MarshalledStorageInterop {

   public static void main(String[] args) throws IOException {
      Configuration configuration = new ConfigurationBuilder().build();
      RemoteCacheManager remoteCacheManager = new RemoteCacheManager(configuration);
      MemcachedClient memcachedClient = new MemcachedClient(new InetSocketAddress("localhost", 11212));

      // The pojo-cache is configured with 'application/x-jboss-marshalling' for both keys and values
      String cacheName = "marshalled-pojo-cache";

      RemoteCache<Object, CryptoCurrency> remoteCache = remoteCacheManager.getCache(cacheName);

      // Write from Hot Rod
      CryptoCurrency bitcoin = new CryptoCurrency("Bitcoin", 1);
      CryptoCurrency litecoin = new CryptoCurrency("Litecoin", 2);
      CryptoCurrency dogecoin = new CryptoCurrency("Dogecoin", 100);

      remoteCache.put(10, bitcoin);
      logAction(WRITE, 10, bitcoin, HOT_ROD);

      remoteCache.put(20, litecoin);
      logAction(WRITE, 20, litecoin, HOT_ROD);

      remoteCache.put(30, dogecoin);
      logAction(WRITE, 30, dogecoin, HOT_ROD);

      System.out.println("Cache size after insertion = " + remoteCache.size());

      Executor requestExecutor = Executor.newInstance().auth("dev", "dev");

      // Read from REST as JSON
      String response = requestExecutor
            .execute(Request.Get(String.format("http://%s:%d/rest/%s/%s", "localhost", 8080, cacheName, 20))
                  .addHeader(ACCEPT, APPLICATION_JSON_TYPE)
                  .addHeader("Key-Content-Type", "application/x-java-object;type=java.lang.Integer"))
            .returnContent().asString();
      logAction(READ, "'LTC' as JSON", response, REST);

      // Write from REST in JSON format
      String json = "{\"_type\":\"org.infinispan.interop.CryptoCurrency\",\"description\":\"Monero\",\"rank\":20}";
      StatusLine status = requestExecutor.execute(
            Request.Post(String.format("http://%s:%d/rest/%s/%s", "localhost", 8080, cacheName, 40))
                  .addHeader("key-content-type","application/x-java-object; type=java.lang.Integer")
                  .bodyString(json, ContentType.APPLICATION_JSON))
            .returnResponse().getStatusLine();

      logAction(WRITE, 40, json, REST);
      System.out.println("Status code: " + status);

      // Read from Hot Rod the data inserted via REST
      CryptoCurrency eth = remoteCache.get(40);
      logAction(READ, 40, eth, HOT_ROD);

      // Write entry with a String key, as memcached does not work with other types
      CryptoCurrency cardano = new CryptoCurrency("Cardano", 9);
      remoteCache.put("120", cardano);
      logAction(WRITE, "\"120\"", cardano, HOT_ROD);

      // Read from memcached. Format will be JSON since the memcached server's encoding is configured as 'application/json'
      Object o = memcachedClient.get("120");
      logAction(READ, "120", o, MEMCACHED);

      remoteCacheManager.close();
      memcachedClient.shutdown();
   }
}



