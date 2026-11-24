package fr.evaloc.perf;

import fr.evaloc.config.ApplicationContext;
import fr.evaloc.dto.SearchCriteria;
import fr.evaloc.dto.SearchResult;
import fr.evaloc.util.TimeBudget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Harness de performance manuel (pas JUnit) :
 * {@code mvn -q exec:java -Dexec.classpathScope=test -Dexec.mainClass=fr.evaloc.perf.PerformanceHarness}
 * <p>
 * Ne fabrique pas de résultats : affiche uniquement les mesures réelles.
 */
public final class PerformanceHarness {

    private PerformanceHarness() {
    }

    public static void main(String[] args) throws Exception {
        int concurrency = args.length > 0 ? Integer.parseInt(args[0]) : 5;
        int iterations = args.length > 1 ? Integer.parseInt(args[1]) : 20;

        ApplicationContext context = ApplicationContext.bootstrap();
        try {
            SearchCriteria criteria = new SearchCriteria();
            criteria.setNumeroPage(1);
            criteria.setTaillePage(50);

            List<Long> durations = Collections.synchronizedList(new ArrayList<Long>());
            AtomicInteger timeouts = new AtomicInteger();
            AtomicInteger errors = new AtomicInteger();

            ExecutorService clients = Executors.newFixedThreadPool(concurrency);
            List<Future<?>> futures = new ArrayList<Future<?>>();
            for (int i = 0; i < iterations; i++) {
                futures.add(clients.submit(new Callable<Void>() {
                    @Override
                    public Void call() {
                        long start = System.nanoTime();
                        try {
                            long budget = context.getProperties().getLong("os.timeout.ms", 2500L);
                            SearchResult result = context.getDeclarationService()
                                    .rechercherDeclarations(criteria, new TimeBudget(budget));
                            long ms = (System.nanoTime() - start) / 1_000_000L;
                            durations.add(Long.valueOf(ms));
                            System.out.println("OK total=" + result.getNombreTotal()
                                    + " pageRows=" + result.getDeclarations().size()
                                    + " ms=" + ms);
                        } catch (fr.evaloc.exception.OsTimeoutException e) {
                            timeouts.incrementAndGet();
                            System.out.println("TIMEOUT ms="
                                    + ((System.nanoTime() - start) / 1_000_000L));
                        } catch (Exception e) {
                            errors.incrementAndGet();
                            System.out.println("ERROR " + e.getClass().getSimpleName()
                                    + ": " + e.getMessage());
                        }
                        return null;
                    }
                }));
            }
            for (Future<?> f : futures) {
                f.get();
            }
            clients.shutdown();

            printStats(durations, timeouts.get(), errors.get(), concurrency, iterations);
        } finally {
            context.close();
        }
    }

    private static void printStats(List<Long> durations,
                                   int timeouts,
                                   int errors,
                                   int concurrency,
                                   int iterations) {
        System.out.println("--- Performance summary ---");
        System.out.println("concurrency=" + concurrency + " iterations=" + iterations);
        System.out.println("success=" + durations.size()
                + " timeouts=" + timeouts + " errors=" + errors);
        if (durations.isEmpty()) {
            System.out.println("Aucune mesure de durée (tous en timeout/erreur).");
            return;
        }
        List<Long> sorted = new ArrayList<Long>(durations);
        Collections.sort(sorted);
        long sum = 0L;
        for (Long v : sorted) {
            sum += v.longValue();
        }
        System.out.println("avg=" + (sum / sorted.size()) + "ms");
        System.out.println("min=" + sorted.get(0) + "ms");
        System.out.println("max=" + sorted.get(sorted.size() - 1) + "ms");
        System.out.println("p50=" + percentile(sorted, 50) + "ms");
        System.out.println("p95=" + percentile(sorted, 95) + "ms");
        System.out.println("p99=" + percentile(sorted, 99) + "ms");
        System.out.println("CPU/mémoire : mesurer via OS (top/pidstat) — non inventés ici.");
    }

    private static long percentile(List<Long> sorted, int pct) {
        if (sorted.isEmpty()) {
            return 0L;
        }
        int index = (int) Math.ceil(pct / 100.0 * sorted.size()) - 1;
        index = Math.max(0, Math.min(sorted.size() - 1, index));
        return sorted.get(index).longValue();
    }
}
