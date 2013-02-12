/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2012 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.util.task.executor;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import org.sonatype.nexus.util.task.CancelableRunnable;
import org.sonatype.nexus.util.task.CancelableSupport;

/**
 * Default implementation of Executor that adds a thin layer around {@link java.util.concurrent.Executor} that is passed
 * in from constructor.
 * 
 * @author cstamas
 * @since 2.4
 */
public class ConstrainedExecutorImpl
    implements ConstrainedExecutor
{
    /**
     * Plain executor for background batch-updates. This executor runs 1 periodic thread (see constructor) that performs
     * periodic remote WL update, but also executes background "force" updates (initiated by user over REST or when
     * repository is added). But, as background threads are bounded by presence of proxy repositories, and introduce
     * hard limit of possible max executions, it protects this instance that is basically unbounded.
     */
    private final java.util.concurrent.Executor executor;

    /**
     * Plain map holding the repository IDs of repositories being batch-updated in background as keys. All read-write
     * access to this set must happen within synchronized block, using this instance as monitor object.
     */
    private final HashMap<String, CancelableRunnableWrapper> currentlyRunningCanncelableRunnables;

    /**
     * Plain map holding the repository IDs of repositories being batch-updated in background as keys. All read-write
     * access to this set must happen within synchronized block, using this instance as monitor object.
     */
    private final ConcurrentHashMap<String, Semaphore> currentlyRunningSemamphores;

    /**
     * Constructor.
     * 
     * @param executor
     */
    public ConstrainedExecutorImpl( final java.util.concurrent.Executor executor )
    {
        this.executor = checkNotNull( executor );
        this.currentlyRunningCanncelableRunnables = new HashMap<String, CancelableRunnableWrapper>();
        this.currentlyRunningSemamphores = new ConcurrentHashMap<String, Semaphore>();
    }

    @Override
    public synchronized Statistics getStatistics()
    {
        return new Statistics( new HashSet<String>( currentlyRunningCanncelableRunnables.keySet() ) );
    }

    @Override
    public synchronized boolean mayExecute( final String key, final CancelableRunnable command )
    {
        checkNotNull( key );
        checkNotNull( command );
        final CancelableRunnableWrapper oldCommand = currentlyRunningCanncelableRunnables.get( key );
        if ( oldCommand != null )
        {
            return false;
        }
        final CancelableRunnableWrapper wrappedCommand = new CancelableRunnableWrapper( this, key, command );
        currentlyRunningCanncelableRunnables.put( key, wrappedCommand );
        executor.execute( wrappedCommand );
        return true;
    }

    @Override
    public synchronized boolean mustExecute( final String key, final CancelableRunnable command )
    {
        checkNotNull( key );
        checkNotNull( command );
        final CancelableRunnableWrapper oldCommand = currentlyRunningCanncelableRunnables.get( key );
        if ( oldCommand != null )
        {
            oldCommand.cancel();
        }
        final CancelableRunnableWrapper wrappedCommand = new CancelableRunnableWrapper( this, key, command );
        currentlyRunningCanncelableRunnables.put( key, wrappedCommand );
        executor.execute( wrappedCommand );
        return oldCommand != null;
    }

    // ==

    protected Semaphore getSemaphore( final String key )
    {
        Semaphore semaphore = currentlyRunningSemamphores.get( key );
        if ( semaphore == null )
        {
            final Semaphore newSemaphore = new Semaphore( 1 );
            semaphore = currentlyRunningSemamphores.putIfAbsent( key, newSemaphore );
            if ( semaphore == null )
            {
                semaphore = newSemaphore;
            }
        }
        return semaphore;
    }

    protected void cancelableStarting( final CancelableRunnableWrapper wrappedCommand )
        throws InterruptedException
    {
        final Semaphore actualSemaphore = getSemaphore( wrappedCommand.getKey() );
        actualSemaphore.acquire();
    }

    protected synchronized void cancelableStopping( final CancelableRunnableWrapper wrappedCommand )
    {
        if ( !wrappedCommand.isCanceled() )
        {
            currentlyRunningCanncelableRunnables.remove( wrappedCommand.getKey() );
        }
        final Semaphore actualSemaphore = getSemaphore( wrappedCommand.getKey() );
        actualSemaphore.release();
    }

    // ==

    protected static class CancelableRunnableWrapper
        implements CancelableRunnable
    {
        private final ConstrainedExecutorImpl host;

        private final String key;

        private final CancelableRunnable runnable;

        private final CancelableSupport cancelableSupport;

        public CancelableRunnableWrapper( final ConstrainedExecutorImpl host, final String key,
                                          final CancelableRunnable runnable )
        {
            this.host = checkNotNull( host );
            this.key = checkNotNull( key );
            this.runnable = checkNotNull( runnable );
            this.cancelableSupport = new CancelableSupport();
        }

        public String getKey()
        {
            return key;
        }

        public CancelableRunnable getRunnable()
        {
            return runnable;
        }

        @Override
        public void run()
        {
            try
            {
                host.cancelableStarting( this );
                try
                {
                    runnable.run();
                }
                finally
                {
                    host.cancelableStopping( this );
                }
            }
            catch ( InterruptedException e )
            {
                //
            }
        }

        @Override
        public boolean isCanceled()
        {
            return cancelableSupport.isCanceled();
        }

        @Override
        public void cancel()
        {
            runnable.cancel();
            cancelableSupport.cancel();
        }
    }
}
