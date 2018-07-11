package org.infinispan.interop;

import java.io.Serializable;

public class CryptoCurrency implements Serializable {

   public String description;

   public Integer rank;

   public CryptoCurrency() {
   }

   public CryptoCurrency(String description, Integer rank) {
      this.description = description;
      this.rank = rank;
   }

   public String getDescription() {
      return description;
   }

   public Integer getRank() {
      return rank;
   }

   @Override
   public String toString() {
      return "CryptoCurrency{" +
            "description='" + description + '\'' +
            ", rank=" + rank +
            '}';
   }
}
