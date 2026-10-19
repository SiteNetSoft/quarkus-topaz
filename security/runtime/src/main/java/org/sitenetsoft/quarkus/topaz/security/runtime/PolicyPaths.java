package org.sitenetsoft.quarkus.topaz.security.runtime;

import java.util.Locale;
import java.util.StringJoiner;

/**
 * Builds policy paths following the {@code <root>.<METHOD>.<path>} convention.
 */
public final class PolicyPaths {

    private PolicyPaths() {
    }

    /**
     * Builds a policy path. {@code /} separators become {@code .}; template segments {@code {name}} and
     * {@code {name:regex}} become {@code __name}; leading, trailing and empty segments are dropped.
     *
     * @param root the policy root, e.g. {@code todoApp}
     * @param httpMethod the HTTP method, e.g. {@code GET}; upper-cased
     * @param path a path or path template, e.g. {@code /todos/{id}}
     * @return the policy path, e.g. {@code todoApp.GET.todos.__id}
     */
    public static String of(String root, String httpMethod, String path) {
        StringJoiner joined = new StringJoiner(".").add(root).add(httpMethod.toUpperCase(Locale.ROOT));
        for (String segment : path.split("/")) {
            String s = segment.trim();
            if (s.isEmpty()) {
                continue;
            }
            if (s.startsWith("{") && s.endsWith("}")) {
                String name = s.substring(1, s.length() - 1);
                int colon = name.indexOf(':');
                if (colon >= 0) {
                    name = name.substring(0, colon);
                }
                s = "__" + name.trim();
            }
            joined.add(s);
        }
        return joined.toString();
    }
}
