package fr.evaloc.config;

import fr.evaloc.util.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Configuration du {@link ThreadPoolExecutor} partagé (jamais ForkJoinPool.commonPool).
 * Le pool n'est pas recréé à chaque appel SOAP.
 */
public final class ExecutorConfig {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutorConfig.class);

    private ExecutorConfig() {
    }

    /**
     * Crée un ThreadPoolExecutor dimensionné pour le parallélisme des partitions.
     */
    public static ThreadPoolExecutor createExecutor(AppProperties props) {
        int core = props.getInt("parallel.corePoolSize", 4);
        int max = props.getInt("parallel.maximumPoolSize", 8);
        int queueCapacity = props.getInt("parallel.queueCapacity", 8);

        if (core < 1 || max < core || queueCapacity < 1) {
            throw new IllegalArgumentException(
                    "Paramètres pool invalides: core=" + core + ", max=" + max
                            + ", queue=" + queueCapacity);
        }

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                core,
                max,
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(queueCapacity),
                new NamedThreadFactory("evaloc-partition"),
                new LoggingAbortPolicy());

        // Pré-démarrage des threads core pour réduire le temps d'attente au premier appel
        executor.prestartAllCoreThreads();

        LOG.info("ThreadPoolExecutor créé: core={}, max={}, queueCapacity={}",
                core, max, queueCapacity);
        return executor;
    }

    public static void shutdownQuietly(ExecutorService executor) {
        if (executor == null) {
            return;
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static final class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(1);
        private final String prefix;

        private NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, prefix + "-" + counter.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }

    /**
     * Politique Abort : les rejets sont journalisés puis relancés.
     */
    public static final class LoggingAbortPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            LOG.warn("Rejet ThreadPoolExecutor: active={}, poolSize={}, queue={}, completed={}",
                    executor.getActiveCount(),
                    executor.getPoolSize(),
                    executor.getQueue().size(),
                    executor.getCompletedTaskCount());
            throw new RejectedExecutionException(
                    "Saturation du pool de threads EVALOC (active="
                            + executor.getActiveCount() + ", queue="
                            + executor.getQueue().size() + ")");
        }
    }
}
