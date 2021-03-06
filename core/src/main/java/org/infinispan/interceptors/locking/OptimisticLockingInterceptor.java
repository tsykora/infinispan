package org.infinispan.interceptors.locking;

import java.util.Set;

import org.infinispan.InvalidCacheUsageException;
import org.infinispan.commands.AbstractVisitor;
import org.infinispan.commands.DataCommand;
import org.infinispan.commands.FlagAffectedCommand;
import org.infinispan.commands.control.LockControlCommand;
import org.infinispan.commands.read.AbstractDataCommand;
import org.infinispan.commands.tx.PrepareCommand;
import org.infinispan.commands.write.ApplyDeltaCommand;
import org.infinispan.commands.write.ClearCommand;
import org.infinispan.commands.write.DataWriteCommand;
import org.infinispan.commands.write.PutKeyValueCommand;
import org.infinispan.commands.write.PutMapCommand;
import org.infinispan.commands.write.RemoveCommand;
import org.infinispan.commands.write.ReplaceCommand;
import org.infinispan.commands.write.WriteCommand;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.container.entries.CacheEntry;
import org.infinispan.container.entries.RepeatableReadEntry;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContext;
import org.infinispan.context.impl.TxInvocationContext;
import org.infinispan.factories.annotations.Start;
import org.infinispan.util.concurrent.IsolationLevel;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * Locking interceptor to be used by optimistic transactional caches.
 *
 * @author Mircea Markus
 * @since 5.1
 */
public class OptimisticLockingInterceptor extends AbstractTxLockingInterceptor {

   private LockAcquisitionVisitor lockAcquisitionVisitor;
   private boolean needToMarkReads;

   private static final Log log = LogFactory.getLog(OptimisticLockingInterceptor.class);

   @Override
   protected Log getLog() {
      return log;
   }

   @Start
   public void start() {
      if (cacheConfiguration.clustering().cacheMode() == CacheMode.LOCAL &&
            cacheConfiguration.locking().writeSkewCheck() &&
            cacheConfiguration.locking().isolationLevel() == IsolationLevel.REPEATABLE_READ &&
            !cacheConfiguration.unsafe().unreliableReturnValues()) {
         lockAcquisitionVisitor = new LocalWriteSkewCheckingLockAcquisitionVisitor();
         needToMarkReads = true;
      } else {
         lockAcquisitionVisitor = new LockAcquisitionVisitor();
         needToMarkReads = false;
      }
   }

   private void markKeyAsRead(InvocationContext ctx, DataCommand command, boolean forceRead) {
      if (needToMarkReads && ctx.isInTxScope() &&
            (forceRead || !command.hasFlag(Flag.IGNORE_RETURN_VALUES))) {
         TxInvocationContext tctx = (TxInvocationContext) ctx;
         tctx.getCacheTransaction().addReadKey(command.getKey());
      }
   }
   
   @Override
   public Object visitPrepareCommand(TxInvocationContext ctx, PrepareCommand command) throws Throwable {
      if (!command.hasModifications() || command.writesToASingleKey()) {
         //optimisation: don't create another LockReorderingVisitor here as it is not needed.
         log.trace("Not using lock reordering as we have a single key.");
         acquireLocksVisitingCommands(ctx, command);
      } else {
         Object[] orderedKeys = command.getAffectedKeysToLock(true);
         boolean hasClear = orderedKeys == null;
         if (hasClear) {
            log.trace("Not using lock reordering as the prepare contains a clear command.");
            acquireLocksVisitingCommands(ctx, command);
         } else {
            log.tracef("Using lock reordering, order is: %s", orderedKeys);
            acquireAllLocks(ctx, orderedKeys);
         }
      }
      return invokeNextAndCommitIf1Pc(ctx, command);
   }

   @Override
   protected Object visitDataReadCommand(InvocationContext ctx, DataCommand command) throws Throwable {
      markKeyAsRead(ctx, command, true);
      try {
         return invokeNextInterceptor(ctx, command);
      } finally {
         //when not invoked in an explicit tx's scope the get is non-transactional(mainly for efficiency).
         //locks need to be released in this situation as they might have been acquired from L1.
         if (!ctx.isInTxScope()) lockManager.unlockAll(ctx);
      }
   }

   @Override
   public Object visitApplyDeltaCommand(InvocationContext ctx, ApplyDeltaCommand command) throws Throwable {
      try {
         return invokeNextInterceptor(ctx, command);
      } catch (Throwable te) {
         throw cleanLocksAndRethrow(ctx, te);
      }
   }

   @Override
   public Object visitPutMapCommand(InvocationContext ctx, PutMapCommand command) throws Throwable {
      try {
         return invokeNextInterceptor(ctx, command);
      } catch (Throwable te) {
         throw cleanLocksAndRethrow(ctx, te);
      }
   }

   @Override
   protected Object visitDataWriteCommand(InvocationContext ctx, DataWriteCommand command) throws Throwable {
      try {
         // Regardless of whether is conditional so that
         // write skews can be detected in both cases.
         markKeyAsRead(ctx, command, command.isConditional());
         return invokeNextInterceptor(ctx, command);
      } catch (Throwable te) {
         throw cleanLocksAndRethrow(ctx, te);
      }
   }

   @Override
   public Object visitClearCommand(InvocationContext ctx, ClearCommand command) throws Throwable {
      try {
         for (Object key : dataContainer.keySet())
            entryFactory.wrapEntryForClear(ctx, key);
         return invokeNextInterceptor(ctx, command);
      } catch (Throwable te) {
         throw cleanLocksAndRethrow(ctx, te);
      }
   }

   @Override
   public Object visitLockControlCommand(TxInvocationContext ctx, LockControlCommand command) throws Throwable {
      throw new InvalidCacheUsageException(
            "Explicit locking is not allowed with optimistic caches!");
   }

   private class LockAcquisitionVisitor extends AbstractVisitor {
      protected void performWriteSkewCheck(TxInvocationContext ctx, Object key) {
         // A no-op
      }
      @Override
      public Object visitClearCommand(InvocationContext ctx, ClearCommand command) throws Throwable {
         return visitMultiKeyCommand(ctx, command, dataContainer.keySet());
      }

      @Override
      public Object visitPutMapCommand(InvocationContext ctx, PutMapCommand command) throws Throwable {
         return visitMultiKeyCommand(ctx, command, command.getMap().keySet());
      }

      private Object visitMultiKeyCommand(InvocationContext ctx, FlagAffectedCommand command, Set<Object> keys) throws Throwable {
         final TxInvocationContext txC = (TxInvocationContext) ctx;
         boolean skipLocking = hasSkipLocking(command);
         long lockTimeout = getLockAcquisitionTimeout(command, skipLocking);
         for (Object key : keys) {
            lockAndRecord(txC, skipLocking, lockTimeout, key);
         }
         return null;
      }

      @Override
      public Object visitRemoveCommand(InvocationContext ctx, RemoveCommand command) throws Throwable {
         return visitSingleKeyCommand(ctx, command);
      }

      @Override
      public Object visitPutKeyValueCommand(InvocationContext ctx, PutKeyValueCommand command) throws Throwable {
         return visitSingleKeyCommand(ctx, command);
      }

      private Object visitSingleKeyCommand(InvocationContext ctx, AbstractDataCommand command) throws InterruptedException {
         final TxInvocationContext txC = (TxInvocationContext) ctx;
         boolean skipLocking = hasSkipLocking(command);
         long lockTimeout = getLockAcquisitionTimeout(command, skipLocking);
         lockAndRecord(txC, skipLocking, lockTimeout, command.getKey());
         return null;
      }

      private void lockAndRecord(TxInvocationContext txC, boolean skipLocking, long lockTimeout, Object key) throws InterruptedException {
         lockAndRegisterBackupLock(txC, key, lockTimeout, skipLocking);
         performWriteSkewCheck(txC, key);
         txC.addAffectedKey(key);
      }

      @Override
      public Object visitApplyDeltaCommand(InvocationContext ctx, ApplyDeltaCommand command) throws Throwable {
         if (cdl.localNodeIsOwner(command.getKey())) {
            Object[] compositeKeys = command.getCompositeKeys();
            TxInvocationContext txC = (TxInvocationContext) ctx;
            boolean skipLocking = hasSkipLocking(command);
            long lockTimeout = getLockAcquisitionTimeout(command, skipLocking);
            for (Object key : compositeKeys) {
               performWriteSkewCheck(txC, key);
               lockAndRegisterBackupLock(txC, key, lockTimeout, skipLocking);
               txC.addAffectedKey(key);
            }
         }
         return null;
      }

      @Override
      public Object visitReplaceCommand(InvocationContext ctx, ReplaceCommand command) throws Throwable {
         return visitSingleKeyCommand(ctx, command);
      }
   }
   
   private class LocalWriteSkewCheckingLockAcquisitionVisitor extends LockAcquisitionVisitor {
      @Override
      protected void performWriteSkewCheck(TxInvocationContext ctx, Object key) {
         performLocalWriteSkewCheck(ctx, key);
      }
   }

   private void performLocalWriteSkewCheck(TxInvocationContext ctx, Object key) {
      CacheEntry ce = ctx.lookupEntry(key);
      if (ce instanceof RepeatableReadEntry && ctx.getCacheTransaction().keyRead(key)) {
         if (log.isTraceEnabled()) {
            log.tracef("Performing local write skew check for key %s", key);
         }
         ((RepeatableReadEntry) ce).performLocalWriteSkewCheck(dataContainer, true);
      } else {
         if (log.isTraceEnabled()) {
            log.tracef("*Not* performing local write skew check for key %s", key);
         }
      }
   }

   private void acquireAllLocks(TxInvocationContext ctx, Object[] orderedKeys) throws InterruptedException {
      long lockTimeout = cacheConfiguration.locking().lockAcquisitionTimeout();
      for (Object key: orderedKeys) {
         lockAndRegisterBackupLock(ctx, key, lockTimeout, false);
         performLocalWriteSkewCheck(ctx, key);
         ctx.addAffectedKey(key);
      }
   }

   private void acquireLocksVisitingCommands(TxInvocationContext ctx, PrepareCommand command) throws Throwable {
      for (WriteCommand wc : command.getModifications()) {
         wc.acceptVisitor(ctx, lockAcquisitionVisitor);
      }
   }
}
