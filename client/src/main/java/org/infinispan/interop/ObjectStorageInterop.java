package org.infinispan.interop;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.http.HttpHeaders.ACCEPT;
import static org.infinispan.commons.dataconversion.MediaType.APPLICATION_JSON_TYPE;
import static org.infinispan.interop.Utils.Action.READ;
import static org.infinispan.interop.Utils.Action.WRITE;
import static org.infinispan.interop.Utils.Endpoint.HOT_ROD;
import static org.infinispan.interop.Utils.Endpoint.MEMCACHED;
import static org.infinispan.interop.Utils.Endpoint.REST;
import static org.infinispan.interop.Utils.logAction;

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
 * Interop when storing java objects in the server.
 */
public class ObjectStorageInterop {

   public static void main(String[] args) throws Exception {
      Configuration configuration = new ConfigurationBuilder().build();
      RemoteCacheManager remoteCacheManager = new RemoteCacheManager(configuration);
      MemcachedClient memcachedClient = new MemcachedClient(new InetSocketAddress("localhost", 11213));

      // The pojo-cache is configured with 'application/x-java-object' for keys and values
      String cacheName = "pojo-cache";

      RemoteCache<String, CryptoCurrency> remoteCache = remoteCacheManager.getCache(cacheName);

      // Write from Hot Rod
      CryptoCurrency bitcoin = new CryptoCurrency("Bitcoin", 1);
      CryptoCurrency litecoin = new CryptoCurrency("Litecoin", 2);
      CryptoCurrency dogecoin = new CryptoCurrency("Dogecoin", 100);

      remoteCache.put("BTC", bitcoin);
      logAction(WRITE, "BTC", bitcoin, HOT_ROD);

      remoteCache.put("LTC", litecoin);
      logAction(WRITE, "LTC", litecoin, HOT_ROD);

      remoteCache.put("DOGE", dogecoin);
      logAction(WRITE, "DOGE", dogecoin, HOT_ROD);

      System.out.println("Cache size after insertion = " + remoteCache.size());

      Executor requestExecutor = Executor.newInstance().auth("dev", "dev");

      // Read from REST as JSON
      String response = requestExecutor
            .execute(Request.Get(String.format("http://%s:%d/rest/%s/%s", "localhost", 8080, cacheName, "BTC"))
                  .addHeader(ACCEPT, APPLICATION_JSON_TYPE))
            .returnContent().asString();

      logAction(READ, "BTC", response, REST);

      // Write from REST in JSON format
      String json = "{\"_type\":\"org.infinispan.interop.CryptoCurrency\",\"description\":\"Monero\",\"rank\":12}";
      StatusLine status = requestExecutor.execute(
            Request.Post(String.format("http://%s:%d/rest/%s/%s", "localhost", 8080, cacheName, "XMR"))
                  .bodyString(json, ContentType.APPLICATION_JSON))
            .returnResponse().getStatusLine();

      logAction(WRITE, "XMR", json, REST);
      System.out.println("Status code: " + status);

      // Read from Hot Rod the data inserted via REST
      CryptoCurrency xmr = remoteCache.get("XMR");
      logAction(READ, "XMR", xmr, HOT_ROD);

      // Read an entry from memcached. The memcached-connector has client-encoding as 'application/json'
      Object btc = memcachedClient.get("BTC");
      logAction(READ, "BTC", btc, MEMCACHED);

      // Write an entry from memcached
      String value = "{\"_type\":\"org.infinispan.interop.CryptoCurrency\",\"description\":\"Bitcoin Gold\",\"rank\":4999}";
      memcachedClient.set("BTG", -1, value.getBytes(UTF_8)).get();
      logAction(WRITE, "BTG", value, MEMCACHED);

      // Read value inserted from memcached using HotRod and REST
      CryptoCurrency btg = remoteCache.get("BTG");
      logAction(READ, "BTG", btg, HOT_ROD);

      response = requestExecutor
            .execute(Request.Get(String.format("http://%s:%d/rest/%s/%s", "localhost", 8080, cacheName, "BTG"))
                  .addHeader(ACCEPT, APPLICATION_JSON_TYPE))
            .returnContent().asString();
      logAction(READ, "BTG", response, REST);


      remoteCache.stop();
      memcachedClient.shutdown();
   }

}



