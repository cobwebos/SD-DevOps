package hudson.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
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
 * - at most one dependency graph building request queued
 * 
 * @author kutzi
 */
public class DependencyGraphCalculator {
    
    private static final Logger LOGGER = Logger.getLogger(DependencyGraphCalculator.class.getName());
    
    private List<Future<DependencyGraph>> futuresList = new ArrayList<Future<DependencyGraph>>();
    
    private ExecutorService executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<Runnable>(1));
    
    public DependencyGraphCalculator() {
    }
    
    public DependencyGraph rebuildGraph() {
        
        try {
            Future<DependencyGraph> future;
            try {

                // synchronize to prevent race conditions when adding to the list
                // i.e. making sure list contains futures in the same order as they've been submitted.
                synchronized (this) {
                    future = executor.submit(new Callable<DependencyGraph>() {
                        @Override
                        public DependencyGraph call() throws Exception {
                            return new DependencyGraph();
                        }
                    });
                    
                    futuresList.add(future);
                }
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
