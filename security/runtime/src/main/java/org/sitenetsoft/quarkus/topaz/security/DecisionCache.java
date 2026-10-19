package org.sitenetsoft.quarkus.topaz.security;

import java.util.Optional;

/**
 * Caches authorization decisions.
 * <p>
 * Keys have the form {@code identity|policyPath|decision|resourceJson}. The default caches nothing; provide a CDI bean
 * of this type to enable caching. Only successful decisions are stored; errors are never cached.
 */
public interface DecisionCache {

    /**
     * @param key the decision key
     * @return the cached decision, or empty on a miss
     */
    Optional<Boolean> get(String key);

    /**
     * @param key the decision key
     * @param allowed the decision
     */
    void put(String key, boolean allowed);
}
