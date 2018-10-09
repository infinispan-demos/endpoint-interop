package org.infinispan.interop;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.infinispan.interop.Utils.Action.READ;
import static org.infinispan.interop.Utils.Action.WRITE;
import static org.infinispan.interop.Utils.Endpoint.HOT_ROD;
import static org.infinispan.interop.Utils.Endpoint.MEMCACHED;
import static org.infinispan.interop.Utils.Endpoint.REST;
import static org.infinispan.interop.Utils.logAction;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.commons.marshall.UTF8StringMarshaller;

import net.spy.memcached.MemcachedClient;

/**
 * Interoperability using JSON data between REST, Hot Rod, Memcached clients.
 * <p>
 */
public class JsonInterop {

   public static void main(String[] args) throws Exception {
      UTF8StringMarshaller marshaller = new UTF8StringMarshaller();
      Configuration configuration = new ConfigurationBuilder().marshaller(marshaller).build();
      RemoteCacheManager remoteCacheManager = new RemoteCacheManager(configuration);

      MemcachedClient memcachedClient = new MemcachedClient(new InetSocketAddress("localhost", 11215));

      // The 'json-cache' is configured with 'application/json' for keys and values
      String cacheName = "json-cache";

      RemoteCache<String, String> cache = remoteCacheManager.getCache(cacheName);

      // Write from Hot Rod
      String bitcoin = "{\"description\": \"Bitcoin\",\"rank\": 1}";
      String litecoin = "{\"description\": \"Litecon\",\"rank\": 11}";
      String dogecoin = "{\"description\": \"Dogecoin\",\"rank\": 1123}";

      cache.put("BTC", bitcoin);
      cache.put("LTC", litecoin);
      cache.put("DOG", dogecoin);

      logAction(WRITE, "BTC", bitcoin, HOT_ROD);
      logAction(WRITE, "LTC", litecoin, HOT_ROD);
      logAction(WRITE, "DOG", dogecoin, HOT_ROD);

      System.out.println("Cache size after insertion from Hot Rod: " + cache.size());

      // Read from REST
      Executor requestExecutor = Executor.newInstance().auth("dev", "dev");

      String response = requestExecutor
            .execute(Request.Get(String.format("http://%s:%d/rest/%s/%s", "localhost", 8080, cacheName, "BTC")))
            .returnContent().asString();

      logAction(READ, "BTC", response, REST);

      // Writing from REST with a different charset
      Charset shift_jis = Charset.forName("Shift_JIS");
      String content = "{\"description\": \"現金\",\"rank\": 12222}";

      byte[] encoded = shift_jis.encode(content).array();

      requestExecutor.execute(Request.Post(String.format("http://%s:%d/rest/%s/%s", "localhost", 8080, cacheName, "ZEC"))
            .bodyByteArray(encoded)
            .addHeader("Content-Type", "text/plain; charset=" + shift_jis.displayName()));
      logAction(WRITE, "ZEC", content, REST);


      // Reading as UTF-8 (this is the default charset)
      byte[] bytes = requestExecutor
            .execute(Request.Get(String.format("http://%s:%d/rest/%s/%s", "localhost", 8080, cacheName, "ZEC"))
                  .addHeader("Accept", "application/json; charset=UTF-8"))
            .returnContent().asBytes();
      logAction(READ, "ZEC", new String(bytes, UTF_8), REST);

      // Read with Memcached
      Object btc = memcachedClient.get("BTC");
      logAction(READ, "BTC", btc, MEMCACHED);

      Object zec = memcachedClient.get("ZEC");
      logAction(READ, "ZEC", zec, MEMCACHED);

      // Write with memcached
      String eth = "{\"description\": \"Ethereum\",\"rank\": 2}";
      memcachedClient.set("ETH", -1, eth).get();
      logAction(WRITE, "ETH", eth, MEMCACHED);

      // Read with other endpoints
      logAction(READ, "ETH", cache.get("ETH"), HOT_ROD);

      String res = requestExecutor
            .execute(Request.Get(String.format("http://%s:%d/rest/%s/%s", "localhost", 8080, cacheName, "ETH")))
            .returnContent().asString();
      logAction(READ, "ETH", res, REST);

      remoteCacheManager.close();
      memcachedClient.shutdown();
   }


}
