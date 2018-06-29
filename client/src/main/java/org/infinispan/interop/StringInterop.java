package org.infinispan.interop;

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.commons.marshall.UTF8StringMarshaller;

/**
 * Interop with REST and Hot Rod storing text.
 * <p>
 * The Hot Rod Client uses the UTF8StringMarshaller to write keys and values, and REST gets the values as text/plain
 */
public class StringInterop {

   public static void main(String[] args) throws IOException {
      Configuration configuration = new ConfigurationBuilder().marshaller(new UTF8StringMarshaller()).build();
      RemoteCacheManager remoteCacheManager = new RemoteCacheManager(configuration);

      // The 'string-cache' is configured with text/plain for keys and values
      String cacheName = "string-cache";

      RemoteCache<String, String> cache = remoteCacheManager.getCache(cacheName);

      // Write from Hot Rod
      cache.put("BTC", "Bitcoin");
      cache.put("LTC", "Litecoin");
      cache.put("DOG", "Dogecoin");

      System.out.println("Cache size after insertion from Hot Rod: " + cache.size());

      // Read from REST
      Executor requestExecutor = Executor.newInstance().auth("dev", "dev");

      String response = requestExecutor
            .execute(Request.Get(String.format("http://%s:%d/rest/%s/%s", "localhost", 8080, cacheName, "BTC")))
            .returnContent().asString();

      System.out.println("/GET 'BTC' from REST: " + response);

      // Writing from REST with a different charset
      Charset shift_jis = Charset.forName("Shift_JIS");
      String content = "現金";

      byte[] encoded = shift_jis.encode(content).array();

      requestExecutor.execute(Request.Post(String.format("http://%s:%d/rest/%s/%s", "localhost", 8080, cacheName, "ZEC"))
            .bodyByteArray(encoded)
            .addHeader("Content-Type", "text/plain; charset=" + shift_jis.displayName()));

      // Reading as UTF-8 (this is the default charset)
      byte[] bytes = requestExecutor
            .execute(Request.Get(String.format("http://%s:%d/rest/%s/%s", "localhost", 8080, cacheName, "ZEC")))
            .returnContent().asBytes();

      System.out.println("/GET 'ZEC' as UTF-8: " + new String(bytes, "UTF-8"));

      // Reading as Shift_JIS
      bytes = requestExecutor
            .execute(Request.Get(String.format("http://%s:%d/rest/%s/%s", "localhost", 8080, cacheName, "ZEC"))
                  .addHeader("Accept", "text/plain; charset=Shift_JIS"))
            .returnContent().asBytes();

      System.out.println("/GET 'ZEC' as Shift_JIS: " + new String(bytes, shift_jis.displayName()));

   }


}
