package org.sitenetsoft.quarkus.topaz.security.runtime;

import java.util.Optional;

import jakarta.inject.Singleton;

import org.sitenetsoft.quarkus.topaz.security.DecisionCache;

import io.quarkus.arc.DefaultBean;

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
