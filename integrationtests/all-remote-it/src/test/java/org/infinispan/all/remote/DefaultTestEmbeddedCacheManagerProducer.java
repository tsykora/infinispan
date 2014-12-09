package org.infinispan.all.remote;

import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

/**
 * <p>The alternative default {@link EmbeddedCacheManager} producer for the test environment.</p>
 *
 * @author Galder Zamarreño
 */

public class DefaultTestEmbeddedCacheManagerProducer {

   /**
    * Produces the default embedded cache manager.
    *
    * @param defaultConfiguration the default configuration produced by the {@link DefaultTestEmbeddedCacheManagerProducer}.
    * @return the default embedded cache manager used by the application.
    */
   @Produces
   @ApplicationScoped
   public EmbeddedCacheManager getDefaultEmbeddedCacheManager(Configuration defaultConfiguration) {
      ConfigurationBuilder builder = new ConfigurationBuilder();
      GlobalConfigurationBuilder globalConfigurationBuilder = new GlobalConfigurationBuilder();
      globalConfigurationBuilder.globalJmxStatistics().allowDuplicateDomains(true);
      builder.read(defaultConfiguration);

      return new DefaultCacheManager(globalConfigurationBuilder.build(), builder.build(), true);
   }

   // TODO: Do we need kill method here? Or can we live without it? Any issues? Check!!
//   /**
//    * Stops the default embedded cache manager when the corresponding instance is released.
//    *
//    * @param defaultEmbeddedCacheManager the default embedded cache manager.
//    */
//   private void stopCacheManager(@Disposes EmbeddedCacheManager defaultEmbeddedCacheManager) {
//      TestingUtil.killCacheManagers(defaultEmbeddedCacheManager);
//   }

}
