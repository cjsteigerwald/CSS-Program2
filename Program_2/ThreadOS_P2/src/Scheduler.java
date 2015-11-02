import java.util.*;

public class Scheduler extends Thread
{
    private Vector [] queue;
    private int timeSlice;
    private static final int DEFAULT_TIME_SLICE = 1000;

    // New data added to p161
    private boolean[] tids; // Indicate which ids have been used
    private static final int DEFAULT_MAX_THREADS = 10000;

    // A new feature added to p161
    // Allocate an ID array, each element indicating if that id has been used
    private int nextId = 0;
    private void initTid( int maxThreads ) {
        tids = new boolean[maxThreads];
        for ( int i = 0; i < maxThreads; i++ )
            tids[i] = false;
    }

    // A new feature added to p161
    // Search an available thread ID and provide a new thread with this ID
    private int getNewTid( ) {
        for ( int i = 0; i < tids.length; i++ ) {
            int tentative = ( nextId + i ) % tids.length;
            if ( !tids[tentative] ) {
                tids[tentative] = true;
                nextId = ( tentative + 1 ) % tids.length;
                return tentative;
            }
        }
        return -1;
    }

    // A new feature added to p161
    // Return the thread ID and set the corresponding tid element to be unused
    private boolean returnTid( int tid ) {
        if ( tid >= 0 && tid < tids.length && tids[tid] == true ) {
            tids[tid] = false;
            return true;
        }
        return false;
    }

    // A new feature added to p161
    // Retrieve the current thread's TCB from the queue
    public TCB getMyTcb( ) {
        Thread myThread = Thread.currentThread( ); // Get my thread object
        synchronized( queue[0] ) {
            for ( int i = 0; i < queue[0].size( ); i++ ) {
                TCB tcb = ( TCB )queue[0].elementAt( i );
                Thread thread = tcb.getThread( );
                if ( thread == myThread ) // if this is my TCB, return it
                    return tcb;
            }
        }
        return null;
    }

    // A new feature added to p161
    // Return the maximal number of threads to be spawned in the system
    public int getMaxThreads( ) {
        return tids.length;
    }

    public Scheduler( ) {
        timeSlice = DEFAULT_TIME_SLICE;
        queue = new Vector[3];
        queue[0] = new Vector();
        queue[1] = new Vector();
        queue[2] = new Vector();
        initTid( DEFAULT_MAX_THREADS );
    }

    public Scheduler( int quantum ) {
        timeSlice = quantum;
        queue[0] = new Vector();
        queue[1] = new Vector();
        queue[2] = new Vector();
        queue = new Vector[3];
        initTid( DEFAULT_MAX_THREADS );
    }

    // A new feature added to p161
    // A constructor to receive the max number of threads to be spawned
    public Scheduler( int quantum, int maxThreads ) {
        timeSlice = quantum;
        queue = new Vector[3];
        queue[0] = new Vector();
        queue[1] = new Vector();
        queue[2] = new Vector();
        initTid( maxThreads );
    }

    private void schedulerSleep( ) {
        try {
            Thread.sleep( timeSlice / 2 );
        } catch ( InterruptedException e ) {
        }
    }

    // A modified addThread of p161 example
    public TCB addThread( Thread t ) {
        // num = 0 is highest priority queue that holds newly created threads
        int num = 0;
        //t.setPriority( 2 );
        TCB parentTcb = getMyTcb( ); // get my TCB and find my TID
        int pid = ( parentTcb != null ) ? parentTcb.getTid( ) : -1;
        int tid = getNewTid( ); // get a new TID
        if ( tid == -1)
            return null;
        TCB tcb = new TCB( t, tid, pid ); // create a new TCB
        queue[num].add(tcb);
        return tcb;
    }

    // method for moving threads between queues
    public void moveThread( Thread t, int num)
    {
        queue[num].add(t);
        queue[num - 1].remove(t);
    }
    // A new feature added to p161
    // Removing the TCB of a terminating thread
    public boolean deleteThread( ) {
        TCB tcb = getMyTcb( );
        if ( tcb!= null )
            return tcb.setTerminated( );
        else
            return false;
    }

    public void sleepThread( int milliseconds ) {
        try {
            sleep( milliseconds );
        } catch ( InterruptedException e ) { }
    }

    // A modified run of p161
    @Override
    public void run( ) {
        Thread current = null;
        int num = 0;
        int queueCount2 = 0;
        int queueCount1 = 0;
        //this.setPriority( 6 );

        while ( true ) {
            try {
                // get the next TCB and its thread
                if ( queue[num].size() == 0 )
                {
                    num++;
                    if (num == 2)
                    {
                        num = 0;
                    }
                    continue;
                }
                TCB currentTCB = (TCB)queue[num].firstElement();
                current = currentTCB.getThread( );
                if ( current != null )
                {
                    if ( current.isAlive( ) )
                        current.resume();
                    else
                    {
                        // Spawn must be controlled by Scheduler
                        // Scheduler must start a new thread
                        current.start();
                        //current.setPriority( 4 );
                    }
                } // if ( current != null )

                synchronized ( queue[num] ) {
                   /* if ( current != null && current.isAlive( ) )
                        current.suspend();*/
                    if (num == 0)
                    {
                        schedulerSleep();
                        if (currentTCB.getTerminated())
                        {
                            queue[num].remove(currentTCB);
                            returnTid(currentTCB.getTid());
                            continue;
                        }
                        else
                        {
                            queue[num + 1].add(currentTCB);
                            queue[num].remove(currentTCB);
                            continue;
                        }
                    }
                    else if  (num == 1)
                    {
                        schedulerSleep();
                        if (currentTCB.getTerminated())
                        {
                            queue[num].remove(currentTCB);
                            returnTid(currentTCB.getTid());
                            queueCount1 = 0;
                            num = 0;
                            continue;
                        }
                        else if (current != null && current.isAlive() && queueCount1 == 1)
                        {
                            queue[num + 1].add(currentTCB);
                            queue[num ].remove(currentTCB);
                            queueCount1 = 0;
                            num = 0;
                            continue;
                        }
                        else
                        {
                            queueCount1++;
                            num = 0;
                            continue;
                        }
                    }
                    else
                    {
                        schedulerSleep();
                        if(currentTCB.getTerminated())
                        {
                            queue[num].remove(currentTCB);
                            returnTid(currentTCB.getTid());
                            queueCount2 = 0;
                            num = 0;
                            continue;
                        }
                        else if (current != null && current.isAlive() && queueCount2 < 3)
                        {
                            queueCount2++;
                            num = 0;
                            continue;
                        }
                        else
                        {
                            queue[num].add(currentTCB);
                            queue[num].remove(currentTCB);
                            queueCount2 = 0;
                            num = 0;
                            continue;
                        }
                    }
                }
            } catch ( NullPointerException e3 ) {}
        }
    }
}