package org.sitenetsoft.quarkus.topaz;

import java.time.Duration;
import java.util.Optional;

import io.grpc.Status;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;

/**
 * Blocking view of a {@link TopazClient}; shares its channels. Close the owning {@link TopazClient}.
 */
public final class BlockingTopazClient {

    static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DEADLINE_GRACE = Duration.ofSeconds(1);

    /**
     * Blocking wait for a call with the given gRPC deadline: deadline plus one second, so the gRPC deadline fires
     * first; {@link #DEFAULT_TIMEOUT} when no deadline is set.
     */
    static Duration waitFor(Optional<Duration> deadline) {
        return deadline.map(d -> d.plus(DEADLINE_GRACE)).orElse(DEFAULT_TIMEOUT);
    }

    /** Awaits {@code uni}; only ever throws {@link io.grpc.StatusRuntimeException} for call failures. */
    static <T> T await(Uni<T> uni, Duration wait) {
        try {
            return uni.await().atMost(wait);
        } catch (TimeoutException e) {
            throw Status.DEADLINE_EXCEEDED.withDescription("blocking wait exceeded").withCause(e)
                    .asRuntimeException();
        }
    }

    private final BlockingAuthorizerClient authorizer;
    private final BlockingDirectoryClient directory;

    BlockingTopazClient(AuthorizerClient authorizer, DirectoryClient directory) {
        this.authorizer = new BlockingAuthorizerClient(authorizer);
        this.directory = new BlockingDirectoryClient(directory);
    }

    /** The blocking authorizer client. */
    public BlockingAuthorizerClient authorizer() {
        return authorizer;
    }

    /** The blocking directory client. */
    public BlockingDirectoryClient directory() {
        return directory;
    }
}
