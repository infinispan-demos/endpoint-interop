package org.infinispan.interop;

import java.io.Serializable;

import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoMessage;

@ProtoMessage(name = "CryptoCurrency")
@ProtoDoc("@Indexed")
public class CryptoCurrency implements Serializable {

   @ProtoField(number = 1)
   @ProtoDoc("@Field(store = Store.YES, analyze = Analyze.YES, analyzer = @Analyzer(definition = \"standard\"))")
   public String description;

   @ProtoField(number = 2)
   @ProtoDoc("@Field(index = Index.YES, store = Store.NO)")
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
