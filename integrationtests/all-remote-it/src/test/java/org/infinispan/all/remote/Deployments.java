package org.infinispan.all.remote;

import org.infinispan.cdi.ConfigureCache;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import java.io.File;

/**
 * Arquillian deployment utility class.
 *
 * @author Kevin Pollet <kevin.pollet@serli.com> (C) 2011 SERLI
 */
public final class Deployments {

   /**
    * The base deployment web archive. The CDI extension is packaged as an individual jar.
    */
   public static WebArchive baseDeployment() {

      return ShrinkWrap.create(WebArchive.class, "test.war")
            .addAsWebInfResource(Deployments.class.getResource("/beans.xml"), "beans.xml")
            .addAsWebInfResource(new File("target/test-classes/jboss-deployment-structure.xml"))
            .addAsLibrary(ShrinkWrap.create(JavaArchive.class, "infinispan-hotrod-exception.jar")
                  // TODO: is this import ok??????????
                                .addPackage(org.infinispan.client.hotrod.exceptions.HotRodClientException.class.getPackage()))
            .addAsLibraries(

                  // I have copied those files manually into all-remote-it/uber-jars/ folder
                  // note that infinispan-embedded brings CDI and also infinispan-remote brings CDI as well!

                  // You might also meet some problems with incompatible Log classes!...
                  // ...when trying to run with ...redhat-1.jars as some classes changed

//                  new File("uber-jars/infinispan-embedded-query-6.2.0.ER7-redhat-1.jar"),
//                  new File("uber-jars/infinispan-remote-6.2.0.ER7-redhat-1.jar"),
//                  new File("uber-jars/infinispan-embedded-6.2.0.ER7-redhat-1.jar"),
                  new File("uber-jars/infinispan-embedded-7.1.0-SNAPSHOT.jar"),
                  new File("uber-jars/infinispan-embedded-query-7.1.0-SNAPSHOT.jar"),
                  new File("uber-jars/infinispan-remote-7.1.0-SNAPSHOT.jar"),
//                  new File("uber-jars/cache-api-1.0.0-PFD.jar"), does not have EntryProcessorResult.java
                  new File("uber-jars/cache-api-1.0.0.jar"),
                  new File("uber-jars/jsf-api-1.2_15-b01-redhat-11.jar"),
                  new File("uber-jars/cdi-api-1.1.jar"),

                  //TODO: add javax facets + others

                  new File("uber-jars/hibernate-core-3.6.1.Final.jar"),
                  new File("uber-jars/weld-servlet-2.1.1.Final.jar"))

            .addAsManifestResource(ConfigureCache.class.getResource("/META-INF/beans.xml"), "beans.xml")
            .addAsManifestResource(ConfigureCache.class.getResource("/META-INF/services/javax.enterprise.inject.spi.Extension"),
                                   "services/javax.enterprise.inject.spi.Extension");



//            .addAsLibrary(
//                  ShrinkWrap.create(JavaArchive.class, "infinispan-remote.jar")
//
////                        .addPackage(org.infinispan.cdi.util.logging.Log.class.getPackage())
////                        .addPackage(org.infinispan.util.logging.LogFactory.class.getPackage())
////                        .addPackage(infinispanembedded.org.jboss.logging.BasicLogger.class.getPackage())
////                        .addPackage(org.infinispan.persistence.spi.PersistenceException.class.getPackage())
//
//                        // add file remote + embedded uber-jar from test-libs
//
//
//                                          .addPackage(ConfigureCache.class.getPackage())
//                                          .addPackage(AbstractEventBridge.class.getPackage())
//                                          .addPackage(CacheEventBridge.class.getPackage())
//                                          .addPackage(CacheManagerEventBridge.class.getPackage())
//                                          .addAsManifestResource(ConfigureCache.class.getResource("/META-INF/beans.xml"), "beans.xml")
//                                          .addAsManifestResource(ConfigureCache.class.getResource("/META-INF/services/javax.enterprise.inject.spi.Extension"), "services/javax.enterprise.inject.spi.Extension")
//                        );


   }
}
