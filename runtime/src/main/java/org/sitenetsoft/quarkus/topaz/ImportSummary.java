package org.sitenetsoft.quarkus.topaz;

import java.util.List;

/**
 * Totals reported by the Topaz importer for one {@code importData} call.
 *
 * @param objectsSet the number of objects set
 * @param objectsDeleted the number of objects deleted
 * @param relationsSet the number of relations set
 * @param relationsDeleted the number of relations deleted
 * @param errors the messages of the non-zero {@code ImportStatus} entries
 */
public record ImportSummary(long objectsSet, long objectsDeleted, long relationsSet, long relationsDeleted,
        List<String> errors) {

    /**
     * Create a summary.
     * The constructor takes an immutable copy of {@code errors}.
     */
    public ImportSummary {
        errors = List.copyOf(errors);
    }
}
