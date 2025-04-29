package com.devoxx.genie.ui.webview;

import com.devoxx.genie.ui.webview.template.ResourceLoader;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class WebServer {
    private static WebServer instance;
    private int port = -1;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private final Map<String, String> resources = new ConcurrentHashMap<>();

    private static final String PRISM_CSS_RESOURCE = "/prism.css";
    private static final String PRISM_JS_RESOURCE = "/prism.js";
    private static final String BASE_CSS_RESOURCE = "/base.css";
    private static final String BASE_JS_RESOURCE = "/base.js";

    private WebServer() {
        initializeEmbeddedResources();
    }

    public static synchronized WebServer getInstance() {
        if (instance == null) {
            instance = new WebServer();
        }
        return instance;
    }

    private void initializeEmbeddedResources() {
        // Add base HTML template
        resources.put("/base.html", getBaseHtml());
    }

    public void start() {
        if (isRunning()) return;

        try {
            port = findAvailablePort();
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();

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
            log.info("Web server started on port " + port);
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
        log.info("Adding dynamic resource: " + resourceId + ", content length: " + content.length());
        resources.put(resourceId, content);
        return resourceId;
    }

    public String getResourceUrl(String resourcePath) {
        return getServerUrl() + resourcePath;
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

    /**
     * Get the PrismJS CSS URL.
     * 
     * @return URL to PrismJS CSS
     */
    public String getPrismCssUrl() {
        if (!resources.containsKey(PRISM_CSS_RESOURCE)) {
            try {
                String cssContent = new String(getClass().getResourceAsStream("/webview/prism/1.29.0/prism.css").readAllBytes());
                resources.put(PRISM_CSS_RESOURCE, cssContent);
                log.info("Loaded Prism CSS from resources");
            } catch (Exception e) {
                log.error("Failed to load Prism CSS from resources", e);
            }
        }
        return getServerUrl() + PRISM_CSS_RESOURCE;
    }
    
    /**
     * Get the PrismJS JS URL.
     * 
     * @return URL to PrismJS JavaScript
     */
    public String getPrismJsUrl() {
        if (!resources.containsKey(PRISM_JS_RESOURCE)) {
            try (var inputStream = getClass().getResourceAsStream("/webview/prism/1.29.0/prism.js")) {
                if (inputStream != null) {
                    String jsContent = new String(inputStream.readAllBytes());
                    resources.put(PRISM_JS_RESOURCE, jsContent);
                    log.info("Loaded Prism JS from resources");
                } else {
                    log.error("Prism JS resource not found");
                }
            } catch (Exception e) {
                log.error("Failed to load Prism JS from resources", e);
            }
        }
        return getServerUrl() + PRISM_JS_RESOURCE;
    }
    
    /**
     * Get the base CSS URL.
     * 
     * @return URL to base CSS
     */
    public String getBaseCssUrl() {
        if (!resources.containsKey(BASE_CSS_RESOURCE)) {
            try {
                String cssContent = ResourceLoader.loadResource("webview/css/base.css");
                resources.put(BASE_CSS_RESOURCE, cssContent);
                log.info("Loaded base CSS from resources");
            } catch (Exception e) {
                log.error("Failed to load base CSS from resources", e);
            }
        }
        return getServerUrl() + BASE_CSS_RESOURCE;
    }
    
    /**
     * Get the base JS URL.
     * 
     * @return URL to base JavaScript
     */
    public String getBaseJsUrl() {
        if (!resources.containsKey(BASE_JS_RESOURCE)) {
            try {
                String jsContent = ResourceLoader.loadResource("webview/js/base.js");
                resources.put(BASE_JS_RESOURCE, jsContent);
                log.info("Loaded base JS from resources");
            } catch (Exception e) {
                log.error("Failed to load base JS from resources", e);
            }
        }
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
            
            if (resources.containsKey(uri)) {
                String content = resources.get(uri);
                ByteBuf buffer = Unpooled.copiedBuffer(content, CharsetUtil.UTF_8);
                
                FullHttpResponse response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
                
                String contentType = getContentType(uri);
                log.info("Serving content with type: " + contentType);
                
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, buffer.readableBytes());
                
                // Set CORS headers
                response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
                response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, OPTIONS");
                response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type");
                
                ctx.writeAndFlush(response);
            } else {
                log.warn("Resource not found: " + uri);
                ByteBuf buffer = Unpooled.copiedBuffer("Resource not found: " + uri, CharsetUtil.UTF_8);
                FullHttpResponse response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND, buffer);
                
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
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

    /**
     * Generate the base HTML template
     * 
     * @return Base HTML template with proper resource URLs
     */
    private @NotNull String getBaseHtml() {
        // Initialize all resources
        getPrismCssUrl();
        getPrismJsUrl();
        getBaseCssUrl();
        getBaseJsUrl();

        // Load the HTML template from the external file
        String htmlTemplate = ResourceLoader.loadResource("webview/html/base.html");
        
        // Replace the placeholders with actual URLs
        return htmlTemplate
                .replace("${prismCssUrl}", getPrismCssUrl())
                .replace("${baseCssUrl}", getBaseCssUrl())
                .replace("${prismJsUrl}", getPrismJsUrl())
                .replace("${baseJsUrl}", getBaseJsUrl());
    }
}