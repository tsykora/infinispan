package org.infinispan.all.remote;

import org.infinispan.cdi.util.logging.Log;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.impl.ConfigurationProperties;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.infinispan.cdi.Remote;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import static org.infinispan.all.remote.Deployments.baseDeployment;
import static org.junit.Assert.assertEquals;


/**
 * Tests the named cache injection.
 *
 * @author Kevin Pollet <kevin.pollet@serli.com> (C) 2011 SERLI
 */
@RunWith(Arquillian.class)
public class RemoteAllCdiTest {

   private static final String SERVER_LIST_KEY = "infinispan.client.hotrod.server_list";

   // wildfly
   @Deployment(name = "remote-client")
   @TargetsContainer("jboss")

   // jdg
//   @Deployment(name = "node0")
//   @TargetsContainer("remote-query")
   public static Archive<?> deployment() {
      return baseDeployment()
            .addClass(RemoteAllCdiTest.class)
            .addClass(Small.class);
   }

   @Inject
   @Remote("default")
   private RemoteCache<String, String> cache;

   @Inject
   @Small
   private RemoteCache<String, String> cacheWithQualifier;

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
   public void testNamedCache() {
//      cache.put("pete", "British");
//      cache.put("manik", "Sri Lankan");
//
//      assertEquals(cache.getName(), "small");
//      assertEquals(cache.get("pete"), "British");
//      assertEquals(cache.get("manik"), "Sri Lankan");

      // here we check that the cache injection with the @Small qualifier works
      // like the injection with the @Remote qualifier

      assertEquals(cacheWithQualifier.getName(), "small");
      assertEquals(cacheWithQualifier.get("pete"), "British");
      assertEquals(cacheWithQualifier.get("manik"), "Sri Lankan");
   }

   /**
    * Overrides the default remote cache manager.
    *
    */
   @Produces
   @ApplicationScoped
   public static RemoteCacheManager defaultRemoteCacheManager() {
      return new RemoteCacheManager(
            new org.infinispan.client.hotrod.configuration.ConfigurationBuilder()
                  .addServers("127.0.0.1:11222").build());
   }
}