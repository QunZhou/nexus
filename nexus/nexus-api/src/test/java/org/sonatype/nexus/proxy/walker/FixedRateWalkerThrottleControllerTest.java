package org.sonatype.nexus.proxy.walker;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;

import org.junit.Test;
import org.mockito.Mockito;
import org.sonatype.nexus.proxy.walker.WalkerThrottleController.ThrottleInfo;
import org.sonatype.nexus.util.ConstantNumberSequence;
import org.sonatype.nexus.util.FibonacciNumberSequence;
import org.sonatype.nexus.util.NumberSequence;

/**
 * Test for fixed rate walker throttle controller.
 * 
 * @author cstamas
 */
public class FixedRateWalkerThrottleControllerTest
{
    protected FixedRateWalkerThrottleController fixedRateWalkerThrottleController;

    @Test
    public void testDoesItHelpAtAll()
    {
        // set unrealistic TPS and we do "almost nothing" (1 nano) in processItem method
        final int measuredTpsUnreal = performAndMeasureActualTps( 100000000, new ConstantNumberSequence( 1 ) );
        // set 500 TPS and we do "little" (1 nano) in processItem method
        final int measuredTps500 = performAndMeasureActualTps( 500, new ConstantNumberSequence( 1 ) );
        // set 200 TPS and we do "little" (1 nano) in processItem method
        final int measuredTps200 = performAndMeasureActualTps( 200, new ConstantNumberSequence( 1 ) );

        assertThat( "TPS500 should less than Unreal one", measuredTps500, lessThan( measuredTpsUnreal ) );
        assertThat( "TPS200 should less than TPS500 one", measuredTps200, lessThan( measuredTps500 ) );
    }

    // ==

    protected int performAndMeasureActualTps( final int wantedTps, final NumberSequence loadChange )
    {
        fixedRateWalkerThrottleController =
            new FixedRateWalkerThrottleController( wantedTps, new FibonacciNumberSequence( 1 ) );
        fixedRateWalkerThrottleController.setSliceSize( 1 );

        final TestThrottleInfo info = new TestThrottleInfo();

        final WalkerContext context = Mockito.mock( WalkerContext.class );

        final int iterationCount = 10000;

        final long startTime = System.currentTimeMillis();
        fixedRateWalkerThrottleController.walkStarted( context );
        for ( int i = 0; i < iterationCount; i++ )
        {
            info.simulateInvocation( loadChange.next() );
            long sleepTime = fixedRateWalkerThrottleController.throttleTime( info );
            sleep( sleepTime ); // sleep as much as throttle controller says to sleep
        }
        fixedRateWalkerThrottleController.walkEnded( context, info );

        final int measuredTps =
            fixedRateWalkerThrottleController.calculateCPS( iterationCount, System.currentTimeMillis() - startTime );

        System.out.println( "Measured=" + measuredTps );
        System.out.println( "GlobalAvg=" + fixedRateWalkerThrottleController.getGlobalAverageTps() );
        System.out.println( "GlobalMax=" + fixedRateWalkerThrottleController.getGlobalMaximumTps() );

        return measuredTps;
    }

    // ==

    protected static void sleep( long millis )
    {
        try
        {
            Thread.sleep( millis );
        }
        catch ( InterruptedException e )
        {
            // need to kill test too
            throw new RuntimeException( e );
        }
    }

    protected static void sleepNanos( long nanos )
    {
        try
        {
            Thread.sleep( 0, (int) nanos );
        }
        catch ( InterruptedException e )
        {
            // need to kill test too
            throw new RuntimeException( e );
        }
    }

    protected static class TestThrottleInfo
        implements ThrottleInfo
    {
        private final long started;

        private long totalProcessItemSpentMillis;

        private long totalProcessItemInvocationCount;

        public TestThrottleInfo()
        {
            this.started = System.currentTimeMillis();
            this.totalProcessItemSpentMillis = 0;
            this.totalProcessItemInvocationCount = 0;
        }

        public void simulateInvocation( final long spentTimeInProcessItem )
        {
            // we need to sleep to keep getTotalTimeWalking() and totalProcessItemSpentMillis aligned
            sleepNanos( spentTimeInProcessItem );
            totalProcessItemSpentMillis = totalProcessItemSpentMillis + ( spentTimeInProcessItem * 1000 );
            totalProcessItemInvocationCount++;
        }

        @Override
        public long getTotalProcessItemSpentMillis()
        {
            return totalProcessItemSpentMillis;
        }

        @Override
        public long getTotalProcessItemInvocationCount()
        {
            return totalProcessItemInvocationCount;
        }

        @Override
        public long getTotalTimeWalking()
        {
            return System.currentTimeMillis() - started;
        }
    }
}
