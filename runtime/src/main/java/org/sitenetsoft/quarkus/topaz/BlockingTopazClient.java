package org.sitenetsoft.quarkus.topaz;

import java.time.Duration;
import java.util.Optional;

import io.grpc.Status;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;

/**
 * Blocking view of a {@link TopazClient}.
 * It shares the channels of that client.
 * Close the owning {@link TopazClient} to release them.
 */
public final class BlockingTopazClient {

    /** Wait applied when the call has no deadline. */
    static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DEADLINE_GRACE = Duration.ofSeconds(1);

    /**
     * Compute the blocking wait for a call with the given gRPC deadline.
     * The wait is the deadline plus one second, so the gRPC deadline fires first.
     * It is {@link #DEFAULT_TIMEOUT} when no deadline is set.
     *
     * @param deadline the gRPC deadline of the call, if any
     * @return the time to wait for the call
     */
    static Duration waitFor(Optional<Duration> deadline) {
        return deadline.map(d -> d.plus(DEADLINE_GRACE)).orElse(DEFAULT_TIMEOUT);
    }

    /**
     * Wait for {@code uni}.
     * Call failures surface only as {@link io.grpc.StatusRuntimeException}.
     *
     * @param <T> the item type
     * @param uni the call to wait for
     * @param wait the longest time to wait
     * @return the item of the call
     */
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

    /**
     * Create a blocking view of both clients.
     *
     * @param authorizer the reactive authorizer client
     * @param directory the reactive directory client
     */
    BlockingTopazClient(AuthorizerClient authorizer, DirectoryClient directory) {
        this.authorizer = new BlockingAuthorizerClient(authorizer);
        this.directory = new BlockingDirectoryClient(directory);
    }

    /**
     * Get the blocking authorizer client.
     *
     * @return the blocking authorizer client
     */
    public BlockingAuthorizerClient authorizer() {
        return authorizer;
    }

    /**
     * Get the blocking directory client.
     *
     * @return the blocking directory client
     */
    public BlockingDirectoryClient directory() {
        return directory;
    }
}
