# Security hardening and conventions design for 0.2.0

Date: 2026-10-26. Status: approved for planning.

The scope comes from the items that four branch reviews parked for later. The branches are client, security,
zanzibar and pre-release. The user approved the scope and asked that the code and the docs follow two sets of Quarkus
rules. Every other choice below is the author's call, and the text marks the important ones.

## 1. Intent

Version 0.1.0 ships with a few gaps in `quarkus-topaz-security`. In each case an enforcement point can behave
differently from what the docs promise.

- If an application provides its own `PolicyPathMapper` through a producer method, the extension does not see it.
  The application then fails at startup with "policy-root is not set".
- Under `@TopazAuthorize`, a mapper that blocks is not bounded by the enforcement timeout.
- A relation name with a non-breaking space passes validation, and Topaz then rejects it.

Version 0.2.0 closes these gaps. It makes the mapper threading the same at every enforcement point. It adds an
opt-in fast path, so the default mappers no longer move every request to a worker thread.

Version 0.2.0 also brings the code comments, the Javadoc and the docs in line with two Quarkus rule sets. Section
5.7 describes them. Readers of the code and the docs get short sentences, one term per concept and Javadoc on
every member.

## 2. Goals

- If an application provides its own `PolicyPathMapper` bean, the policy-root check never fails for it. This holds
  for a producer method, a subclass and a bean whose class is not in the index.
- The four enforcement points apply the same rules to the mappers. The points are the `topaz` route policy,
  `@TopazAuthorize`, `@TopazCheck` and the `@PermissionsAllowed` bridge. A mapper runs on a thread that may block,
  with the Vert.x context of the request. The enforcement timeout bounds it. On a blocking method, the existing
  grace of one second applies on top. A timeout denies the request.
- If every mapper of a request declares that it does not block, the extension evaluates the request without a
  worker hop.
- `TopazRelation` rejects every name that Topaz rejects. The first call fails with a clear message, not with a
  directory error.
- The code, the Javadoc and the docs follow the rules in section 5.7.
- The security spec and the security page describe what the code does after this change.

## 3. Non-goals

- Interrupting a mapper that is still blocked after the timeout. Mutiny cancellation does not interrupt a thread.
  The denial arrives at the deadline, and the worker thread is free again when the mapper returns. The docs
  already say this and keep saying it.
- Behaviour changes in the client, the Dev Service or the zanzibar connector. Their parked items belong to a later
  plan. The conventions pass of section 5.7 does touch their comments and docs.
- New enforcement features. This means no new annotations and no new configuration keys.
- Changes to the public mapper signatures. The only additions are default methods.
- Javadoc on the generated gRPC stubs.

## 4. Anti-goals

- A fast path that runs a blocking custom mapper on the event loop. A custom mapper stays on the worker unless it
  opts in.
- A check for the custom mapper that is looser than today. A class that implements the interface but is not the
  bean in use must not silence the policy-root check.
- A break for an application that compiles against 0.1.0. This version removes or changes no public method.
- A conventions pass that changes behaviour. That pass touches comments, Javadoc and docs only.

## 5. Decisions

### 5.1 Custom `PolicyPathMapper` detection

The build step `TopazSecurityProcessor.validate` scans the Jandex index for implementors of `PolicyPathMapper`. It
skips the default mapper and abstract types. This scan misses a bean from a producer method and a bean whose class
is not in the index. It also counts a class that implements the interface but is not a bean.

Decision: the runtime-init recorder does the detection. `TopazSecurityRecorder.validate` resolves the
`PolicyPathMapper` bean from the Arc container. The mapper is custom when the bean is not an instance of
`DefaultPolicyPathMapper`. The build step no longer scans implementors. It passes one flag, which says whether the
application needs a policy root.

`DefaultPolicyPathMapper` is a `@DefaultBean`. When the application provides any bean of that type, Arc drops the
default, so the check is exact.

Tests:

- A custom mapper from a `@Produces` method starts without `policy-root`.
- A custom mapper that extends an abstract helper starts without `policy-root`.
- A class that implements the interface but is not a bean still fails at startup with the policy-root message,
  when the application binds the `topaz` policy.

### 5.2 One threading rule for the mappers

`DecisionEvaluator.evaluateRequest` already runs the resolver and the three mappers on a worker. It moves them
through `mapperExecutor` after the timeout starts. `TopazAuthorizeInterceptor` and `TopazCheckInterceptor` call
the mappers on the thread that subscribes. For a blocking method, that thread then waits in `await`. The timeout
does not bound a mapper that blocks there. For a `Uni` method on the event loop, the mapper blocks the loop.

Decision: one shared operation does the worker hop and the timeout guard. Its shape is close to
`Uni<T> mapped(Supplier<Uni<T>> assembly, boolean callerMayBlock)`. It moves to `mapperExecutor`, runs the
assembly there and applies `ifNoItem().after(timeout).fail()`. When the caller has a Vert.x context, the result
comes back on that context.

`evaluateRequest`, the two interceptors and `TopazPermissionAugmentor` call their mappers through this operation.
`InterceptorSupport.failClosed` keeps its outer timeout as the last guard. The inner guard makes a blocking mapper
deny on time.

Blocking dispatch, author's call: a blocking method runs `dispatch` on a worker that then waits in `await`. A hop
to a second worker would hold two threads of the same pool for each request. Under load, the mapper tasks would
queue behind the waiting threads, and every request would deny at the timeout.

So when the caller may block, the operation runs the assembly on the current thread. The bound is then the outer
`await().atMost(timeout + 1 s)`. The operation takes the hop only when the caller cannot block. That covers the
event loop, a `Uni` or `Multi` method and the route policy.

`@RequestScoped` beans stay out of reach inside a mapper. The security page says so today.

Tests:

- A blocking `IdentityMapper` under `@TopazAuthorize` on a `Uni` method denies within the timeout. The mapper runs
  on a worker thread, not on the event loop.
- The same mapper on a blocking method denies within the timeout plus one second, and runs on the request thread.
- A blocking `SubjectMapper` under `@TopazCheck` and under the `@PermissionsAllowed` bridge gets the same two
  checks.
- `RoutePolicySlowMapperTest` asserts the Mutiny `TimeoutException` in the denial log. It no longer relies on the
  latch and the sleep.

### 5.3 Fast path for mappers that do not block

The worker hop costs two thread switches per request. This holds even when every mapper is a default mapper that
does not block.

Decision: `IdentityMapper`, `PolicyPathMapper`, `ResourceContextMapper` and `SubjectMapper` each get a default
method, `default boolean blocking() { return true; }`. The four default mappers return `false`. Suppose every
mapper of an enforcement point returns `false`. The shared operation from 5.2 then runs the assembly on the
calling thread. The timeout guard still applies.

The default value is `true`, so a custom mapper from 0.1.0 keeps its behaviour. An application opts its mapper in
by overriding the method. The Javadoc and the security page give the contract. A mapper that returns `false` must
not block and must not use `@RequestScoped` beans.

Cache and resolver, author's call: `DecisionCache.get`, `DecisionCache.put` and `ResourceMethodInfoResolver.resolve`
run inside the same assembly. Their interfaces say nothing about blocking. A Redis or JDBC cache would then run on
the event loop.

So the fast path needs two more conditions. The `DecisionCache` bean is the built-in `NoopDecisionCache`. The
`ResourceMethodInfoResolver` bean is one of the two built-in resolvers. The recorder checks both with `instanceof`
at runtime init, as in 5.1. These two interfaces get no `blocking()` method, so the limit on new public API in
section 6 holds.

Tests:

- With the default mappers, the extension evaluates a route-policy request on the event-loop thread. A test
  `ResourceContextMapper` that returns `false` records the thread name.
- A custom mapper that keeps the default `true` still runs on a worker.
- A custom `DecisionCache` with all default mappers still takes the hop.

### 5.4 Grammar of `TopazRelation` names

`TopazRelation` rejects the characters for which `Character.isWhitespace` is true. That test misses U+00A0 and
U+2007. Topaz identifiers are ASCII.

Decision: the name splits at `#` and `@` into the object type, the relation and the subject type. Each part must
match `[A-Za-z0-9_.-]+`. The constructor rejects any other name. The existing message stays and names the part
that fails.

The builder checks the identifier grammar of Topaz 0.33.22 for manifest names. If Topaz accepts more, the pattern
grows to match. The pattern is never wider than what Topaz accepts.

Tests: the constructor rejects U+00A0, U+2007, a tab and an empty part. `todo#can_read@user` and `doc-v2#viewer` pass.

### 5.5 Docs and spec

- The "Mapper threading" section of `security.adoc` describes the single rule, the `blocking()` contract and the
  fast path.
- Section 5 of the design spec of 2026-10-18 gets an "As built" note. The note records decisions 5.1 to 5.4. It
  also lists the drift that is already in the code:
  - The mappers are `@DefaultBean`s, and the extension validates at startup.
  - `IdentityMapper` takes a `SecurityIdentity` and an `Optional<RoutingContext>`.
  - `TopazRelation` takes a name and an id, and `ResourceMethodInfo` is a record.
  - `@TopazCheck` always answers 401 to an anonymous caller.
  - A `CompletionStage` method counts as blocking, and cache keys are hashes.
- The three `quarkus-extension.yaml` files point `guide:` to `https://sitenetsoft.org/quarkus-topaz/`. GitHub Pages
  serves the site there, and the old host redirects to it.

### 5.6 Smaller code items

- `TopazHealthCheck` defines `HEALTH_TIMEOUT`, and `TopazJsonRpcService` uses it. The message derives "10s" from
  the constant.
- The tests of three modules each copy `silentListener()`. Those modules are `deployment`, `security/deployment`
  and `zanzibar/deployment`. Author's call: each module gets one helper class with one copy. A shared test jar or a
  test-support module would add an artifact. The release workflow and `bin/check-staging.sh` would then have to
  leave it out.

### 5.7 Code and prose conventions

The rules come from two Quarkus pull requests:

- quarkusio/quarkus#57819 has the rules for comments and Javadoc.
- quarkusio/quarkus#57821 has the rules for sentences, and a prose checker, `prose-lint.java`.

Both apply to this repository where they make sense.

Comments and Javadoc, from #57819:

- Every type, constructor, public or package-private method, record, enum and enum constant in `main` code gets a
  Javadoc block. A record gets one `@param` per component. One sentence plus `@param` and `@return` is enough.
  A trivial accessor still gets a block.
- In a long method, one `//` comment per logical block names what the block does.
- No comment restates a single statement. A comment does not explain why a one-line fix exists. The commit message
  carries that.
- A `// TODO` names the issue that tracks it.
- Test classes get no Javadoc.
- A recorder captures one `List` of records, not parallel lists. A captured collection holds no `null`.

Sentences, from #57821:

- One idea per sentence, about 20 words at most. Active voice, present tense. The verb, not a noun made from it.
- No semicolon or dash joins two clauses. No essential fact sits in parentheses. No filler words.
- One term per concept, in the text and in the code.
- A paragraph has one topic and at most six sentences.
- A method's Javadoc starts with one sentence in the imperative, such as "Get the bean name.". A type's Javadoc
  starts with a sentence that says what the type is.
- A commit message has a one-sentence subject, a blank line and a body in the same style.

Scope, author's call:

- All code, Javadoc and docs that this plan writes or rewrites follow the rules from the start.
- One conventions pass covers the rest of the hand-written `main` code in every module. It adds the missing
  Javadoc and turns the first sentence of each method's Javadoc into the imperative. It also removes comments
  that restate a statement. The generated stubs in `grpc/stubs` are out of scope.
- The same pass rewrites every page under `docs/modules/ROOT/pages` and the README. The checker report guides the
  pass. A passive-voice finding is a prompt to re-read, not an error. AsciiDoc listings, tables and attribute
  lines are not prose.
- The pass changes no code, no configuration key and no documented behaviour. Each module and the docs get their
  own commit, so a reviewer can read each diff alone.

## 6. Constraints

- Branching: the work starts from `master` on a feature branch. It starts after the `v0.1.0` release run has
  passed and the `0.2.0-SNAPSHOT` bump is on `master`. The branch must not merge before that bump.
  `bin/release-check.sh` refuses snapshots, and the tag must keep the behaviour of 0.1.0.
- Quarkus 3.40.1, release target Java 21, no new dependencies.
- The `security.runtime` package stays `@Internal`. The four `blocking()` default methods are the only new public
  API.
- Fail closed everywhere. No path in this plan may permit a request on an error or a timeout.
- Every behaviour change comes with a `QuarkusUnitTest` in `security/deployment` against the Topaz Dev Service.

## 7. Left to the builder

- The name of the shared operation in 5.2, and whether it lives on `DecisionEvaluator` or in a new package-private
  helper.
- How the test mappers record the thread that runs them. A captured thread name is the suggestion.
- The exact Topaz identifier grammar for 5.4.
- Whether 5.6 is one task or part of the test-helper work in 5.2.
- The order of the module commits in the conventions pass.

## 8. As built

- The policy-root check compares the exact bean class with `DefaultPolicyPathMapper`. A subclass of
  `DefaultPolicyPathMapper` therefore counts as custom, and startup does not check `policy-root` for it. A subclass
  that calls `super.map` still needs `policy-root`. Without it, the subclass denies every request and fails closed.
- A default mapper returns `false` from `blocking()` only for its exact built-in class. A subclass counts as blocking
  unless it overrides `blocking()`, so its enforcement points take the hop to a worker thread.
- The fast path treats the cache and the resolver as built-in only for the exact classes `NoopDecisionCache` and
  `NoopResourceMethodInfoResolver`, or the REST resolver by name. A subclass of either counts as custom.
