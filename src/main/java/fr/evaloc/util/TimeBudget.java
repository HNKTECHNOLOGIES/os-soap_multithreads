package fr.evaloc.util;

import fr.evaloc.exception.OsTimeoutException;

import java.util.concurrent.TimeUnit;

/**
 * Budget temporel mesuré avec {@link System#nanoTime()} depuis l'entrée du service SOAP.
 */
public final class TimeBudget {

    private final long startNanos;
    private final long budgetMillis;

    public TimeBudget(long budgetMillis) {
        if (budgetMillis <= 0) {
            throw new IllegalArgumentException("budgetMillis doit être > 0");
        }
        this.startNanos = System.nanoTime();
        this.budgetMillis = budgetMillis;
    }

    public long getBudgetMillis() {
        return budgetMillis;
    }

    public long elapsedMillis() {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    public long remainingMillis() {
        return budgetMillis - elapsedMillis();
    }

    public boolean isExpired() {
        return remainingMillis() <= 0;
    }

    /**
     * Vérifie qu'il reste du budget, sinon lève {@link OsTimeoutException}.
     */
    public void checkNotExpired(String phase) {
        if (isExpired()) {
            throw new OsTimeoutException(
                    "Budget OS dépassé pendant: " + phase
                            + " (écoulé=" + elapsedMillis() + "ms, budget=" + budgetMillis + "ms)");
        }
    }

    /**
     * Retourne le temps restant ou lève timeout si le budget est épuisé.
     */
    public long requireRemainingMillis(String phase) {
        long remaining = remainingMillis();
        if (remaining <= 0) {
            throw new OsTimeoutException(
                    "Budget OS dépassé avant: " + phase
                            + " (écoulé=" + elapsedMillis() + "ms, budget=" + budgetMillis + "ms)");
        }
        return remaining;
    }
}
