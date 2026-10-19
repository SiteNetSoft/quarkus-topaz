package org.sitenetsoft.quarkus.topaz;

import java.util.List;

/**
 * Totals reported by the Topaz importer for one {@code importData} call. {@code errors} holds the messages of
 * non-zero {@code ImportStatus} entries.
 */
public record ImportSummary(long objectsSet, long objectsDeleted, long relationsSet, long relationsDeleted,
        List<String> errors) {

    public ImportSummary {
        errors = List.copyOf(errors);
    }
}
