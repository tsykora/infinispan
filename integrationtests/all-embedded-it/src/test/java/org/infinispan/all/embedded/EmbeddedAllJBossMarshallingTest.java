package org.infinispan.all.embedded;

import org.jboss.marshalling.ByteInput;
import org.jboss.marshalling.ByteOutput;
import org.jboss.marshalling.ContextClassResolver;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.marshalling.reflect.SunReflectiveCreator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * Test the behaviour of JBoss Marshalling library itself included in infinispan-embedded uber-jar
 */
public class EmbeddedAllJBossMarshallingTest {

   private MarshallerFactory factory;
   private Marshaller marshaller;
   private Unmarshaller unmarshaller;

   @Before
   public void setUp() throws Exception {
      factory = (MarshallerFactory) Thread.currentThread().getContextClassLoader().loadClass("org.jboss.marshalling.river.RiverMarshallerFactory").newInstance();
      MarshallingConfiguration configuration = new MarshallingConfiguration();
      configuration.setCreator(new SunReflectiveCreator());
      configuration.setClassResolver(new ContextClassResolver());

      marshaller = factory.createMarshaller(configuration);
      unmarshaller = factory.createUnmarshaller(configuration);
   }

   @After
   public void tearDown() {
   }

   @Test
   public void testSerObjWithRefToSerObjectWithCustomReadObj() throws Exception {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(10240);
      ByteOutput byteOutput = Marshalling.createByteOutput(baos);
      marshaller.start(byteOutput);
      ObjectThatContainsACustomReadObjectMethod obj = new ObjectThatContainsACustomReadObjectMethod();
      obj.anObjectWithCustomReadObjectMethod = new CustomReadObjectMethod();
      try {
         marshaller.writeObject(obj);
      } finally {
         marshaller.finish();
      }

      byte[] bytes = baos.toByteArray();

      ByteInput byteInput = Marshalling.createByteInput(new ByteArrayInputStream(bytes));
      unmarshaller.start(byteInput);
      try {
         assert obj.equals(unmarshaller.readObject());
      } finally {
         unmarshaller.finish();
      }
   }

   public static class CustomReadObjectMethod implements Serializable {
      private static final long serialVersionUID = 1L;
      String lastName;
      String ssn;
      transient boolean deserialized;

      public CustomReadObjectMethod( ) {
         this("Zamarreno", "234-567-8901");
      }

      public CustomReadObjectMethod(String lastName, String ssn) {
         this.lastName = lastName;
         this.ssn = ssn;
      }

      @Override
      public boolean equals(Object obj) {
         if (obj == this) return true;
         if (!(obj instanceof CustomReadObjectMethod)) return false;
         CustomReadObjectMethod pk = (CustomReadObjectMethod) obj;
         if (!lastName.equals(pk.lastName)) return false;
         if (!ssn.equals(pk.ssn)) return false;
         return true;
      }

      @Override
      public int hashCode( ) {
         int result = 17;
         result = result * 31 + lastName.hashCode();
         result = result * 31 + ssn.hashCode();
         return result;
      }

      private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
         ois.defaultReadObject();
         deserialized = true;
      }
   }

   public static class ObjectThatContainsACustomReadObjectMethod implements Serializable {
      private static final long serialVersionUID = 1L;
      public CustomReadObjectMethod anObjectWithCustomReadObjectMethod;
      Integer balance;

      public boolean equals(Object obj) {
         if (obj == this)
            return true;
         if (!(obj instanceof ObjectThatContainsACustomReadObjectMethod))
            return false;
         ObjectThatContainsACustomReadObjectMethod acct = (ObjectThatContainsACustomReadObjectMethod) obj;
         if (!safeEquals(balance, acct.balance))
            return false;
         if (!safeEquals(anObjectWithCustomReadObjectMethod, acct.anObjectWithCustomReadObjectMethod))
            return false;
         return true;
      }

      public int hashCode() {
         int result = 17;
         result = result * 31 + safeHashCode(balance);
         result = result * 31 + safeHashCode(anObjectWithCustomReadObjectMethod);
         return result;
      }

      private static int safeHashCode(Object obj) {
         return obj == null ? 0 : obj.hashCode();
      }

      private static boolean safeEquals(Object a, Object b) {
         return (a == b || (a != null && a.equals(b)));
      }
   }
}
