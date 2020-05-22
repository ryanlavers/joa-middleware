package ca.lavers.joa.middleware;

import ca.lavers.joa.core.Context;
import ca.lavers.joa.core.Middleware;
import ca.lavers.joa.core.NextMiddleware;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Serves the contents of files from the specified root directory
 *
 * This middleware uses the full request path to locate files within the root directory, so
 * if you only want to serve files under a sub-path (e.g. http://example.com/static) then you'll
 * either need to use a prefix-stripping middleware before it in the chain (see {@link ca.lavers.joa.middleware.prefixrouter.PrefixRouter})
 * or just ensure your files are in a similarly-named directory in your root directory (e.g.
 * configure your root directory as /var/www and put your files under /var/www/static)
 *
 * Ex.
 * prefixRouter.prefix("/static", new FileServer("/var/www"));
 *
 */
public class FileServer implements Middleware {

    private final String root;

    public FileServer(String root) {
        if(!root.endsWith(File.separator)) {
            root += File.separator;
        }
        this.root = root;
    }

    @Override
    public void call(Context ctx, NextMiddleware next) {
        try {
            handle(ctx);
        } catch (Exception e) {
            e.printStackTrace();
            ctx.response().status(500);
            ctx.response().body("Error processing request");
        }

    }

    private void handle(Context ctx) throws IOException {
        // TODO - Only respond to GET requests
        final File file = new File(this.root, ctx.request().path());

        // Verify the resolved file path is actually under our root path
        if(file.getCanonicalPath().startsWith(this.root)) {
            if(file.isFile()) {
                ctx.response().status(200);
                ctx.response().body(new FileInputStream(file));
                ctx.response().bodySize(file.length());
                String mime = Files.probeContentType(file.toPath());
                if(mime != null) {
                    ctx.response().header("Content-Type", mime);
                }
            }
            else {
                ctx.response().status(404);
                ctx.response().body("Not Found");
            }
        }
        else {
            ctx.response().status(400);
        }

    }
}
