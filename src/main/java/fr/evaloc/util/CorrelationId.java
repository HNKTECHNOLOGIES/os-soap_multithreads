package fr.evaloc.util;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * Identifiant de corrélation propagé via MDC SLF4J dans les threads parallèles.
 */
public final class CorrelationId {

    public static final String MDC_KEY = "correlationId";

    private CorrelationId() {
    }

    public static String generate() {
        return UUID.randomUUID().toString();
    }

    public static void set(String correlationId) {
        if (correlationId == null || correlationId.isEmpty()) {
            MDC.remove(MDC_KEY);
        } else {
            MDC.put(MDC_KEY, correlationId);
        }
    }

    public static String get() {
        return MDC.get(MDC_KEY);
    }

    public static void clear() {
        MDC.remove(MDC_KEY);
    }

    /**
     * Exécute une tâche en restaurant le MDC de corrélation dans le thread courant.
     */
    public static Runnable wrap(final String correlationId, final Runnable runnable) {
        return new Runnable() {
            @Override
            public void run() {
                String previous = get();
                try {
                    set(correlationId);
                    runnable.run();
                } finally {
                    if (previous == null) {
                        clear();
                    } else {
                        set(previous);
                    }
                }
            }
        };
    }

    public static <T> java.util.concurrent.Callable<T> wrapCallable(
            final String correlationId,
            final java.util.concurrent.Callable<T> callable) {
        return new java.util.concurrent.Callable<T>() {
            @Override
            public T call() throws Exception {
                String previous = get();
                try {
                    set(correlationId);
                    return callable.call();
                } finally {
                    if (previous == null) {
                        clear();
                    } else {
                        set(previous);
                    }
                }
            }
        };
    }
}
