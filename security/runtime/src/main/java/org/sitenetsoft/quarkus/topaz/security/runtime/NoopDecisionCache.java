package org.sitenetsoft.quarkus.topaz.security.runtime;

import java.util.Optional;

import jakarta.inject.Singleton;

import org.sitenetsoft.quarkus.topaz.security.DecisionCache;

import io.quarkus.arc.DefaultBean;

/**
 * The default {@link DecisionCache}, which caches nothing.
 * <p>
 * Every lookup misses and every store is dropped, so each decision is a call to Topaz.
 * <p>
 * It is a {@code @DefaultBean}. To cache decisions, provide your own bean implementing {@link DecisionCache}.
 */
@Singleton
@DefaultBean
public class NoopDecisionCache implements DecisionCache {

    /**
     * Create the NoopDecisionCache.
     */
    public NoopDecisionCache() {
    }

    @Override
    public Optional<Boolean> get(String key) {
        return Optional.empty();
    }

    @Override
    public void put(String key, boolean allowed) {
    }
}
