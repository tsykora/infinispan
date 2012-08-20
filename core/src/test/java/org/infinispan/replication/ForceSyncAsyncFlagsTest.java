/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009 Red Hat Inc. and/or its affiliates and other
 * contributors as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.infinispan.replication;

import org.infinispan.AdvancedCache;
import org.infinispan.commands.write.PutKeyValueCommand;
import org.infinispan.config.Configuration;
import org.infinispan.context.Flag;
import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.test.MultipleCacheManagersTest;
import org.infinispan.test.fwk.CleanupAfterMethod;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

/**
 * Tests for FORCE_ASYNCHRONOUS and FORCE_SYNCHRONOUS flags.
 *
 * @author Anna Manukyan
 */
@Test(testName = "loaders.ForceSyncAsyncFlagsTest", groups = "functional")
@CleanupAfterMethod
public class ForceSyncAsyncFlagsTest extends MultipleCacheManagersTest {

   protected void createCacheManagers() throws Throwable {
   }

   public void testForceAsynchronousFlagUsage() throws CacheLoaderException, InterruptedException {
      Configuration c = getDefaultClusteredConfig(Configuration.CacheMode.REPL_SYNC, true);
      createClusteredCaches(2, "replication", c);

      AdvancedCache cache1 = cache(0,"replication").getAdvancedCache();
      AdvancedCache cache2 = cache(1,"replication").getAdvancedCache();
      // test a simple put!
      assert cache1.get("key") == null;
      assert cache2.get("key") == null;

      cache1.put("key", "value");

      assert cache1.get("key").equals("value");
      assert cache2.get("key").equals("value");

      //This case even works if the expectation and replication wait are not in place.
      replListener(cache2).expect(PutKeyValueCommand.class);

      cache1.withFlags(Flag.FORCE_ASYNCHRONOUS).put("key1", "value1");
      replListener(cache2).waitForRpc(1, TimeUnit.SECONDS);

      assert cache1.get("key1").equals("value1");
      assert cache2.get("key1").equals("value1");
   }

   public void testForceSynchronousFlagUsage() throws CacheLoaderException, InterruptedException {
      Configuration c = getDefaultClusteredConfig(Configuration.CacheMode.REPL_ASYNC, true);
      createClusteredCaches(2, "replication", c);

      AdvancedCache cache1 = cache(0,"replication").getAdvancedCache();
      AdvancedCache cache2 = cache(1,"replication").getAdvancedCache();
      // test a simple put!
      assert cache1.get("key") == null;
      assert cache2.get("key") == null;

      replListener(cache2).expect(PutKeyValueCommand.class);
      cache1.put("key", "value");
      replListener(cache2).waitForRpc();

      assert cache1.get("key").equals("value");
      assert cache2.get("key").equals("value");

      cache1.withFlags(Flag.FORCE_SYNCHRONOUS).put("key1", "value1");

      //Here it fails as the replication is in any case asynchrone
      assert cache1.get("key1").equals( "value1" );
      assert cache2.get("key1").equals( "value1" );
   }

}

