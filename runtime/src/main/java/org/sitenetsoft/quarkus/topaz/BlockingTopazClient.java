package org.sitenetsoft.quarkus.topaz;

import java.time.Duration;

/**
 * Blocking view of a {@link TopazClient}; shares its channels. Close the owning {@link TopazClient}.
 */
public final class BlockingTopazClient {

    static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    private final BlockingAuthorizerClient authorizer;
    private final BlockingDirectoryClient directory;

    BlockingTopazClient(AuthorizerClient authorizer, DirectoryClient directory) {
        this.authorizer = new BlockingAuthorizerClient(authorizer);
        this.directory = new BlockingDirectoryClient(directory);
    }

    public BlockingAuthorizerClient authorizer() {
        return authorizer;
    }

    public BlockingDirectoryClient directory() {
        return directory;
    }
}
