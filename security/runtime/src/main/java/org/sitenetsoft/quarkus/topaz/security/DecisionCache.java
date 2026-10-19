package org.sitenetsoft.quarkus.topaz.security;

import java.util.Optional;

/**
 * Caches authorization decisions.
 * <p>
 * Keys are opaque 64-character lowercase hex SHA-256 digests of the identity type, identity, policy path, decision and
 * resource context. They never contain the identity, a token or the resource in clear, but they still identify a
 * caller's decision, so keep them private to the cache. The default caches nothing; provide a CDI bean of this type to
 * enable caching. Only successful decisions are stored; errors are never cached.
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
