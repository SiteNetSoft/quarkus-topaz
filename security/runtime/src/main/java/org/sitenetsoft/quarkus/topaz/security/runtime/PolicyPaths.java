package org.sitenetsoft.quarkus.topaz.security.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

/**
 * Builds policy paths following the {@code <root>.<METHOD>.<path>} convention.
 */
public final class PolicyPaths {

    private PolicyPaths() {
    }

    /**
     * Build a policy path.
     * <p>
     * Each {@code /} separator becomes {@code .}. Template segments {@code {name}} and {@code {name:regex}} become
     * {@code __name}. Leading, trailing and empty segments are dropped.
     *
     * @param root the policy root, for example {@code todoApp}
     * @param httpMethod the HTTP method, for example {@code GET}, which is upper-cased
     * @param path a path or path template, for example {@code /todos/{id}}
     * @return the policy path, for example {@code todoApp.GET.todos.__id}
     */
    public static String of(String root, String httpMethod, String path) {
        StringJoiner joined = new StringJoiner(".").add(root).add(httpMethod.toUpperCase(Locale.ROOT));
        for (String segment : segments(path)) {
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

    // Split the path at '/' only at brace depth zero, because a '/' inside {...} belongs to a regex.
    private static List<String> segments(String path) {
        List<String> segments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}' && depth > 0) {
                depth--;
            }
            if (c == '/' && depth == 0) {
                segments.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        segments.add(current.toString());
        return segments;
    }
}
