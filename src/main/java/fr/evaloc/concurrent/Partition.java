package fr.evaloc.concurrent;

/**
 * Partition logique sur des plages de valeurs de {@code idFiche} (VARCHAR).
 * Les bornes sont inclusives/exclusives selon la convention :
 * {@code lowerInclusive <= idFiche < upperExclusive}.
 * Une borne {@code null} signifie une extrémité ouverte.
 */
public final class Partition {

    private final int index;
    private final String lowerInclusive;
    private final String upperExclusive;

    public Partition(int index, String lowerInclusive, String upperExclusive) {
        if (index < 0) {
            throw new IllegalArgumentException("index partition invalide: " + index);
        }
        this.index = index;
        this.lowerInclusive = lowerInclusive;
        this.upperExclusive = upperExclusive;
    }

    public int getIndex() {
        return index;
    }

    /**
     * Borne inférieure inclusive, ou {@code null} si aucune.
     */
    public String getLowerInclusive() {
        return lowerInclusive;
    }

    /**
     * Borne supérieure exclusive, ou {@code null} si aucune.
     */
    public String getUpperExclusive() {
        return upperExclusive;
    }

    public String getId() {
        return "P" + index;
    }

    @Override
    public String toString() {
        return "Partition{"
                + "id=" + getId()
                + ", lower=" + lowerInclusive
                + ", upper=" + upperExclusive
                + '}';
    }
}
