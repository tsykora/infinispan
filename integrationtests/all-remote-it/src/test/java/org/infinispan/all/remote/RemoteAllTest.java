package org.infinispan.all.remote;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.impl.ConfigurationProperties;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(Arquillian.class)
public class RemoteAllTest {

   private static RemoteCacheManager rcm = null;

   @BeforeClass
   public static void beforeTest() {
      ConfigurationBuilder builder = new ConfigurationBuilder();
      builder.addServer()
            .host("127.0.0.1")
            .port(11222)
            .protocolVersion(ConfigurationProperties.PROTOCOL_VERSION_20);
      rcm = new RemoteCacheManager(builder.build());
   }

   @AfterClass
   public static void cleanUp() {
      if (rcm != null)
         rcm.stop();
   }

   @Test
   public void testBasicHotRodPutGetRemoteAll() {
      RemoteCache<String, String> c1 = rcm.getCache("default");
      c1.put("key1", "value1");
      assertEquals("value1", c1.get("key1"));
   }
}
