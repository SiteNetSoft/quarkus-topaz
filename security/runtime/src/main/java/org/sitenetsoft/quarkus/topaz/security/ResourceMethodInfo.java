package org.sitenetsoft.quarkus.topaz.security;

import java.util.Map;

/**
 * The resource method matched for an HTTP request.
 *
 * @param httpMethod the HTTP method, e.g. {@code GET}
 * @param pathTemplate the class path template joined with the method path template, starting with {@code /},
 *        e.g. {@code /todos/{id}}
 * @param pathParams the path parameter values of the request, keyed by template parameter name
 */
public record ResourceMethodInfo(String httpMethod, String pathTemplate, Map<String, String> pathParams) {

    public ResourceMethodInfo {
        pathParams = Map.copyOf(pathParams);
    }
}
