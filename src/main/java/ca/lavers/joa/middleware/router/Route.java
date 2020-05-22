package ca.lavers.joa.middleware.router;

import ca.lavers.joa.core.MiddlewareChain;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Route {
    private final String path;
    private final Pattern pattern;
    private final List<String> paramNames = new ArrayList<>();

    private final MiddlewareChain middleware;

    Route(String path, MiddlewareChain middleware) {
        this.path = path;
        this.pattern = toRegex(path);
        this.middleware = middleware;
    }

    private Pattern toRegex(String path) {
        if(path.startsWith("/")) {
            path = path.substring(1);
        }

        StringJoiner sj = new StringJoiner("/");

        for(String part : path.split("/")) {
            if(part.startsWith(":")) {
                sj.add("([^/]*)");
                paramNames.add(part.substring(1));
            }
            else {
                sj.add(part);
            }
        }

        String regex = sj.toString();
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

    public MiddlewareChain getMiddleware() {
        return middleware;
    }
}
