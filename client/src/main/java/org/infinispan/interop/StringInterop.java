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
 * Interop with REST and Hot Rod storing text.
 * <p>
 * The Hot Rod Client uses the UTF8StringMarshaller to write keys and values, and REST gets the values as text/plain
 */
public class StringInterop {

   public static void main(String[] args) throws Exception {
      Configuration configuration = new ConfigurationBuilder().marshaller(new UTF8StringMarshaller()).build();
      RemoteCacheManager remoteCacheManager = new RemoteCacheManager(configuration);
      MemcachedClient memcachedClient = new MemcachedClient(new InetSocketAddress("localhost", 11211));

      // The 'string-cache' is configured with text/plain for keys and values
      String cacheName = "string-cache";

      RemoteCache<String, String> cache = remoteCacheManager.getCache(cacheName);

      // Write from Hot Rod
      cache.put("BTC", "Bitcoin");
      cache.put("LTC", "Litecoin");
      cache.put("DOG", "Dogecoin");
      logAction(WRITE, "BTC", "Bitcoin", HOT_ROD);
      logAction(WRITE, "LTC", "Litecoin", HOT_ROD);
      logAction(WRITE, "DOG", "Dogecoin", HOT_ROD);

      System.out.println("Cache size after insertion from Hot Rod: " + cache.size());

      // Read from REST
      Executor requestExecutor = Executor.newInstance().auth("dev", "dev");

      String response = requestExecutor
            .execute(Request.Get(String.format("http://%s:%d/rest/%s/%s", "localhost", 8080, cacheName, "BTC")))
            .returnContent().asString();

      logAction(READ, "BTC", response, REST);

      // Writing from REST with a different charset
      Charset shift_jis = Charset.forName("Shift_JIS");
      String content = "現金";

      byte[] encoded = shift_jis.encode(content).array();

      requestExecutor.execute(Request.Post(String.format("http://%s:%d/rest/%s/%s", "localhost", 8080, cacheName, "ZEC"))
            .bodyByteArray(encoded)
            .addHeader("Content-Type", "text/plain; charset=" + shift_jis.displayName()));
      logAction(WRITE, "ZEC", content, REST);


      // Reading as UTF-8 (this is the default charset)
      byte[] bytes = requestExecutor
            .execute(Request.Get(String.format("http://%s:%d/rest/%s/%s", "localhost", 8080, cacheName, "ZEC")))
            .returnContent().asBytes();
      logAction(READ, "ZEC", new String(bytes, UTF_8), REST);

      // Reading as Shift_JIS
      bytes = requestExecutor
            .execute(Request.Get(String.format("http://%s:%d/rest/%s/%s", "localhost", 8080, cacheName, "ZEC"))
                  .addHeader("Accept", "text/plain; charset=Shift_JIS"))
            .returnContent().asBytes();
      logAction(READ, " `ZEC` as Shift_JIS", new String(bytes, shift_jis.displayName()), REST);

      // Read with Memcached
      Object btc = memcachedClient.get("BTC");
      logAction(READ, "BTC", btc, MEMCACHED);

      Object zec = memcachedClient.get("ZEC");
      logAction(READ, "ZEC", zec, MEMCACHED);

      // Write with memcached
      memcachedClient.set("ETH", -1, "Ethereum").get();
      logAction(WRITE, "ETH", "Ethereum", MEMCACHED);

      // Read with other endpoints:
      logAction(READ, "ETH", cache.get("ETH"), HOT_ROD);

      String res = requestExecutor
            .execute(Request.Get(String.format("http://%s:%d/rest/%s/%s", "localhost", 8080, cacheName, "ETH")))
            .returnContent().asString();
      logAction(READ, "ETH", res, REST);

      remoteCacheManager.close();
      memcachedClient.shutdown();
   }


}
