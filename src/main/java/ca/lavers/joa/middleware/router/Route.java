package ca.lavers.joa.middleware.router;

import ca.lavers.joa.core.MiddlewareChain;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Route {
    private final String path;
    private final Pattern pattern;
    private final List<String> paramNames = new ArrayList<>();

    private final boolean isPrefix;

    private final MiddlewareChain middleware;

    // TODO -- Make route abstract and split into LiteralRoute, ParameterizedRoute, PrefixRoute

    Route(String path, MiddlewareChain middleware) {
        this(path, middleware, false);
    }

    Route(String path, MiddlewareChain middleware, boolean isPrefix) {
        this.path = path;
        this.pattern = toRegex(path, isPrefix);
        this.middleware = middleware;
        this.isPrefix = isPrefix;
    }

    private Pattern toRegex(String path, boolean isPrefix) {
        if(path.startsWith("/")) {
            path = path.substring(1);
        }

        StringJoiner sj = new StringJoiner("/");

        for(String part : path.split("/")) {
            if(part.startsWith(":")) {
                if(isPrefix) {
                    throw new IllegalStateException("Path parameters not permitted in prefix routes: " + path);
                }
                sj.add("([^/]*)");
                paramNames.add(part.substring(1));
            }
            else {
                // TODO - Escape so raw regexes can't be passed (or is that a feature?)
                sj.add(part);
            }
        }

        String regex = sj.toString();

        if(isPrefix) {
            regex += "(|/.*)";
        }

        return Pattern.compile("/" + regex);
    }

    public Map<String, String> match(String path) {
        Matcher m = pattern.matcher(path);
        if(m.matches()) {
            Map<String, String> results = new HashMap<>();
            for(int i=0; i<paramNames.size(); i++) {
                results.put(paramNames.get(i), m.group(i+1));
            }
            return results;
        }
        else {
            return null;
        }
    }

    public String getPath() {
        return path;
    }

    public boolean isPrefix() {
        return isPrefix;
    }

    public MiddlewareChain getMiddleware() {
        return middleware;
    }
}
