package org.infinispan.interop;

final class Utils {

   enum Endpoint {HOT_ROD, REST, MEMCACHED}

   enum Action {READ, WRITE, QUERY}

   static void logAction(Action action, Object input, Object content, Endpoint endpoint) {
      switch (action) {
         case WRITE:
            System.out.println("Wrote '" + input + "'='" + content + "' from " + endpoint);
            break;
         case READ:
            System.out.println("Read '" + input + "' from " + endpoint + ": " + content);
            break;
         case QUERY:
            System.out.println("Query " + input + " from " + endpoint + ": " + content);
      }
   }

}
