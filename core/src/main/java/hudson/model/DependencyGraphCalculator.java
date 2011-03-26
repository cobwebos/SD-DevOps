package hudson.model;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Helper class to calculate {@link DependencyGraph}s.
 * 
 * This class ensures that at any time at most 1 thread rebuilds the dependency graph.
 * When one rebuild is already in progress any new requests will wait for the *next*
 * (that is the single request after the currently running one) rebuild to finish.
 * 
 * So there are always:
 * - at most one dependency graph in the process of being build
 * - at most one dependency grpah building request queued
 * 
 * @author kutzi
 */
public class DependencyGraphCalculator {
    
    private static final Logger LOGGER = Logger.getLogger(DependencyGraphCalculator.class.getName());
    
    private CopyOnWriteArrayList<Future<DependencyGraph>> futuresList = new CopyOnWriteArrayList<Future<DependencyGraph>>();
    
    private ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<Runnable>(1));
    
    public DependencyGraphCalculator() {
        //this.executor.setRejectedExecutionHandler(handler)
    }
    
    public DependencyGraph rebuildGraph() {
        
        // TODO: to be absolutely safe, we must add some synchronisation around
        // futureList.add and executor.submit resp. futureList.get
        // However, for our use case it should it should be safe enough
        // In case of an exception we will fall back to the old behaviour anyway
        try {
            Future<DependencyGraph> future;
            try {
                future = executor.submit(new Callable<DependencyGraph>() {
    
                    @Override
                    public DependencyGraph call() throws Exception {
                        return new DependencyGraph();
                    }
                });
                
                futuresList.add(future);
            } catch (RejectedExecutionException e) {
                //already 2 callables submitted. Wait for the last submitted one to complete
                future = futuresList.get(futuresList.size() - 1);
            }
        
        
            DependencyGraph graph = future.get();
            futuresList.remove(future);
            return graph;
        } catch (Exception e) {
            LOGGER.warning(e.toString());
            
            // fall back to old behaviour
            return new DependencyGraph();
        }
    }
    
}
