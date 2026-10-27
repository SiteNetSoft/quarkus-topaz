package org.sitenetsoft.quarkus.topaz.security;

import java.util.Optional;

/**
 * A cache of authorization decisions.
 * <p>
 * Keys are opaque 64-character lowercase hex SHA-256 digests of the identity type, identity, policy path, decision and
 * resource context. They never contain the identity, a token or the resource in clear. They still identify a caller's
 * decision, so keep them private to the cache. The default caches nothing. Provide a CDI bean of this type to enable
 * caching. Only successful decisions are stored. Errors are never cached.
 */
public interface DecisionCache {

    /**
     * Get a cached decision.
     *
     * @param key the decision key
     * @return the cached decision, or empty on a miss
     */
    Optional<Boolean> get(String key);

    /**
     * Store a decision.
     *
     * @param key the decision key
     * @param allowed the decision
     */
    void put(String key, boolean allowed);
}
