package org.sitenetsoft.quarkus.topaz.security.runtime;

import java.util.Optional;

import jakarta.inject.Singleton;

import org.sitenetsoft.quarkus.topaz.security.DecisionCache;

import io.quarkus.arc.DefaultBean;

/**
 * Caches nothing: every lookup misses and every store is dropped, so each decision is a call to Topaz. It is a {@code
 * @DefaultBean}: to cache decisions, provide your own bean implementing {@link DecisionCache}.
 */
@Singleton
@DefaultBean
public class NoopDecisionCache implements DecisionCache {

    @Override
    public Optional<Boolean> get(String key) {
        return Optional.empty();
    }

    @Override
    public void put(String key, boolean allowed) {
    }
}
