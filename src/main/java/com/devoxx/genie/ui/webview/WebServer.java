package com.devoxx.genie.ui.webview;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static com.devoxx.genie.ui.webview.template.ResourceLoader.loadResource;

@Slf4j
public class WebServer {
    private static WebServer instance;
    private int port = -1;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private final Map<String, String> resources = new ConcurrentHashMap<>();
    private final Map<String, String> scripts = new ConcurrentHashMap<>();
    public static final String PRISM_CSS_RESOURCE = "/prism.css";
    public static final String PRISM_JS_RESOURCE = "/prism.js";
    public static final String BASE_CSS_RESOURCE = "/base.css";
    public static final String BASE_JS_RESOURCE = "/base.js";
    public static final String MCP_LOG_HANDLER_JS_RESOURCE = "/mcpLogHandler.js";
    public static final String BASE_HTML_RESOURCE = "/base.html";

    private WebServer() {
        initializeEmbeddedResources();
    }

    public static synchronized WebServer getInstance() {
        if (instance == null) {
            instance = new WebServer();
        }
        return instance;
    }

    public void start() {
        if (isRunning()) return;

        try {
            port = findAvailablePort();
            bossGroup = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
            workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(
                                    new HttpServerCodec(),
                                    new HttpObjectAggregator(65536),
                                    new WebServerHandler()
                            );
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();
            log.info("Web server started on port {}", port);
        } catch (Exception e) {
            log.error("Failed to start web server", e);
            stop();
        }
    }

    public void stop() {
        if (serverChannel != null) {
            serverChannel.close();
            serverChannel = null;
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
            bossGroup = null;
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            workerGroup = null;
        }
        port = -1;
    }

    public boolean isRunning() {
        return serverChannel != null && serverChannel.isOpen();
    }

    public String getServerUrl() {
        return "http://localhost:" + port;
    }

    public String addDynamicResource(@NotNull String content) {
        String resourceId = "/dynamic/" + System.currentTimeMillis() + "-" + ThreadLocalRandom.current().nextInt(1000, 9999);
        log.info("Adding dynamic resource: {} - content length: {}", resourceId, content.length());
        resources.put(resourceId, content);
        return resourceId;
    }

    public String getResourceUrl(String resourcePath) {
        return getServerUrl() + resourcePath;
    }
    
    /**
     * Add a dynamic JavaScript script that can be injected into web views.
     *
     * @param scriptId unique identifier for the script
     * @param content  JavaScript content
     */
    public void addDynamicScript(@NotNull String scriptId, @NotNull String content) {
        String resourcePath = "/scripts/" + scriptId + ".js";
        log.info("Adding dynamic script: {}, content length: {}", resourcePath, content.length());
        resources.put(resourcePath, content);
        scripts.put(scriptId, resourcePath);
    }
    
    /**
     * Get the URL for a previously registered script.
     * 
     * @param scriptId the script identifier
     * @return URL to the script, or null if not found
     */
    public String getScriptUrl(String scriptId) {
        String path = scripts.get(scriptId);
        return path != null ? getServerUrl() + path : null;
    }

    private int findAvailablePort() {
        try {
            java.net.ServerSocket socket = new java.net.ServerSocket(0);
            int port = socket.getLocalPort();
            socket.close();
            return port;
        } catch (Exception e) {
            log.error("Failed to find available port", e);
            return 8090; // Fallback port
        }
    }

    public void initializeEmbeddedResources() {
        String baseHTML = loadResource("webview/html/base.html")
                .replace("${prismCssUrl}", getPrismCssUrl())
                .replace("${baseCssUrl}", getBaseCssUrl())
                .replace("${prismJsUrl}", getPrismJsUrl())
                .replace("${baseJsUrl}", getBaseJsUrl());
        resources.put(BASE_HTML_RESOURCE, baseHTML);
        
        // Add static icon resources
        addStaticResource("/icons/copy.svg", "icons/copy.svg");
        addStaticResource("/icons/copy_dark.svg", "icons/copy_dark.svg");
    }
    
    /**
     * Add a static resource from the resources directory to be served by the web server.
     *
     * @param uriPath the URI path to serve the resource at
     * @param resourcePath the path to the resource in the resources directory
     */
    public void addStaticResource(String uriPath, String resourcePath) {
        try {
            String content = loadResource(resourcePath);
            if (!content.isEmpty()) {
                resources.put(uriPath, content);
                log.info("Added static resource: {} from {}", uriPath, resourcePath);
            } else {
                log.warn("Failed to load static resource: {}", resourcePath);
            }
        } catch (Exception e) {
            log.error("Error loading static resource: " + resourcePath, e);
        }
    }

    public String getPrismCssUrl() {
        resources.put(PRISM_CSS_RESOURCE, loadResource("webview/prism/prism.css"));
        return getServerUrl() + PRISM_CSS_RESOURCE;
    }

    public String getPrismJsUrl() {
        resources.put(PRISM_JS_RESOURCE, loadResource("webview/prism/prism.js"));
        return getServerUrl() + PRISM_JS_RESOURCE;
    }
    
    public String getBaseCssUrl() {
        resources.put(BASE_CSS_RESOURCE, loadResource("webview/css/base.css"));
        return getServerUrl() + BASE_CSS_RESOURCE;
    }

    public String getBaseJsUrl() {
        resources.put(BASE_JS_RESOURCE, loadResource("webview/js/base.js"));
        return getServerUrl() + BASE_JS_RESOURCE;
    }

    private class WebServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
            String uri = request.uri();
            
            // Normalize URI by removing query string if present
            if (uri.contains("?")) {
                uri = uri.substring(0, uri.indexOf("?"));
            }

            log.info("Handling request for: " + uri);
            
            // Handle health check endpoint
            if ("/health-check".equals(uri)) {
                handleHealthCheckRequest(ctx);
                return;
            }
            
            if (resources.containsKey(uri)) {
                String content = resources.get(uri);
                ByteBuf buffer = Unpooled.copiedBuffer(content, CharsetUtil.UTF_8);
                
                FullHttpResponse response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
                
                String contentType = getContentType(uri);
                log.info("Serving content with type: {}", contentType);
                
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, buffer.readableBytes());
                
                // Set CORS headers
                response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
                response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, OPTIONS");
                response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type");
                
                ctx.writeAndFlush(response);
            } else {
                log.warn("Resource not found: {}", uri);
                ByteBuf buffer = Unpooled.copiedBuffer("Resource not found: " + uri, CharsetUtil.UTF_8);
                FullHttpResponse response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND, buffer);
                
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, buffer.readableBytes());
                
                ctx.writeAndFlush(response);
            }
        }

        /**
         * Handle health check requests to verify server connectivity.
         */
        private void handleHealthCheckRequest(ChannelHandlerContext ctx) {
            try {
                String healthStatus = "{\"status\":\"ok\",\"timestamp\":" + System.currentTimeMillis() + "}";
                ByteBuf buffer = Unpooled.copiedBuffer(healthStatus, CharsetUtil.UTF_8);
                
                FullHttpResponse response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
                
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, buffer.readableBytes());
                
                // Set CORS headers
                response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
                response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, OPTIONS");
                response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type");
                
                // Add cache control to prevent caching
                response.headers().set(HttpHeaderNames.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
                response.headers().set(HttpHeaderNames.PRAGMA, "no-cache");
                response.headers().set(HttpHeaderNames.EXPIRES, "0");
                
                ctx.writeAndFlush(response);
                log.debug("Health check response sent");
                
            } catch (Exception e) {
                log.error("Error handling health check request", e);
                
                // Send error response
                ByteBuf buffer = Unpooled.copiedBuffer("{\"status\":\"error\"}", CharsetUtil.UTF_8);
                FullHttpResponse response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR, buffer);
                
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, buffer.readableBytes());
                
                ctx.writeAndFlush(response);
            }
        }

        private @NotNull String getContentType(@NotNull String uri) {
            if (uri.endsWith(".js")) {
                return "application/javascript";
            } else if (uri.endsWith(".css")) {
                return "text/css";
            } else if (uri.endsWith(".html") || uri.startsWith("/dynamic/")) {
                return "text/html";
            } else if (uri.endsWith(".svg")) {
                return "image/svg+xml";
            } else {
                return "text/plain";
            }
        }

        @Override
        public void exceptionCaught(@NotNull ChannelHandlerContext ctx, Throwable cause) {
            log.error("Exception in web server handler", cause);
            ctx.close();
        }
    }
}