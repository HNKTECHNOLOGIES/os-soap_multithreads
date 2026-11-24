package fr.evaloc.dao;

import fr.evaloc.exception.OsValidationException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Liste blanche des colonnes de tri autorisées (évite l'injection SQL).
 */
public final class SortColumnWhitelist {

    private static final Map<String, String> ALLOWED;

    static {
        Map<String, String> map = new HashMap<String, String>();
        map.put("idfiche", "f.idFiche");
        map.put("idetat", "f.etat");
        map.put("etat", "f.etat");
        map.put("dtcreation", "f.dtCreation");
        map.put("iddeclafonciere", "l.idDeclaFonciere");
        map.put("cddept", "l.cdDept");
        map.put("cdcommune", "l.cdCommune");
        map.put("invariant", "l.invariant");
        map.put("idlocal", "l.idLocal");
        map.put("dtchangement", "l.dtChangement");
        map.put("typelocal", "l.typeLocal");
        ALLOWED = Collections.unmodifiableMap(map);
    }

    private SortColumnWhitelist() {
    }

    /**
     * Résout le critère de tri métier vers une expression SQL sûre.
     *
     * @param critereTri nom logique (ex. idFiche) ; défaut {@code f.idFiche}
     */
    public static String resolveColumn(String critereTri) {
        if (critereTri == null || critereTri.trim().isEmpty()) {
            return "f.idFiche";
        }
        String key = critereTri.trim().toLowerCase(Locale.ROOT).replace("_", "");
        String column = ALLOWED.get(key);
        if (column == null) {
            throw new OsValidationException("Critère de tri non autorisé: " + critereTri);
        }
        return column;
    }

    /**
     * Résout le sens de tri ({@code ASC} ou {@code DESC}).
     */
    public static String resolveDirection(String sensTri) {
        if (sensTri == null || sensTri.trim().isEmpty()) {
            return "ASC";
        }
        String s = sensTri.trim().toUpperCase(Locale.ROOT);
        if ("ASC".equals(s) || "DESC".equals(s)) {
            return s;
        }
        throw new OsValidationException("Sens de tri invalide: " + sensTri + " (ASC|DESC)");
    }
}
