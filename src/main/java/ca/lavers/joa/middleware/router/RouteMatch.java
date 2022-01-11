package ca.lavers.joa.middleware.router;

import java.util.Map;

public class RouteMatch {

    Route route;
    Map<String, String> match;

    RouteMatch(Route route, Map<String, String> match) {
        this.route = route;
        this.match = match;
    }

    public String matchedPath() {
        return route.getPath();
    }

    public String getParam(String name) {
        return match.get(name);
    }
}
