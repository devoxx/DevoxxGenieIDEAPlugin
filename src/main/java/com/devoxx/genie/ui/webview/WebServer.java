package com.devoxx.genie.ui.webview;

import com.intellij.openapi.diagnostic.Logger;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class WebServer {
    private static final Logger LOG = Logger.getInstance(WebServer.class);
    private static WebServer instance;
    private int port = -1;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private final Map<String, String> resources = new ConcurrentHashMap<>();
    
    // PrismJS CDN URLs
    private static final String PRISM_CSS_URL = "https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/themes/prism-okaidia.min.css";
    private static final String PRISM_JS_URL = "https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/prism.min.js";

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
            LOG.info("Web server started on port " + port);
        } catch (Exception e) {
            LOG.error("Failed to start web server", e);
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
        LOG.info("Adding dynamic resource: " + resourceId + ", content length: " + content.length());
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
            LOG.error("Failed to find available port", e);
            return 8090; // Fallback port
        }
    }
    
    /**
     * Get the PrismJS CSS URL.
     * 
     * @return URL to PrismJS CSS
     */
    public String getPrismCssUrl() {
        return PRISM_CSS_URL;
    }
    
    /**
     * Get the PrismJS JS URL.
     * 
     * @return URL to PrismJS JavaScript
     */
    public String getPrismJsUrl() {
        return PRISM_JS_URL;
    }

    private class WebServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
            String uri = request.uri();
            
            // Normalize URI by removing query string if present
            if (uri.contains("?")) {
                uri = uri.substring(0, uri.indexOf("?"));
            }
            
            LOG.info("Handling request for: " + uri);
            
            if (resources.containsKey(uri)) {
                String content = resources.get(uri);
                ByteBuf buffer = Unpooled.copiedBuffer(content, CharsetUtil.UTF_8);
                
                FullHttpResponse response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);
                
                String contentType = getContentType(uri);
                LOG.info("Serving content with type: " + contentType);
                
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, buffer.readableBytes());
                
                // Set CORS headers
                response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
                response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, OPTIONS");
                response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type");
                
                ctx.writeAndFlush(response);
            } else {
                LOG.warn("Resource not found: " + uri);
                ByteBuf buffer = Unpooled.copiedBuffer("Resource not found: " + uri, CharsetUtil.UTF_8);
                FullHttpResponse response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND, buffer);
                
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, buffer.readableBytes());
                
                ctx.writeAndFlush(response);
            }
        }

        private String getContentType(String uri) {
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
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            LOG.error("Exception in web server handler", cause);
            ctx.close();
        }
    }

    // Base HTML template with PrismJS includes from CDN
    private String getBaseHtml() {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>DevoxxGenie</title>
                    <link rel="stylesheet" href="%s">
                    <style>
                        body { 
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, 
                                         Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif; 
                            line-height: 1.6; 
                            margin: 0; 
                            padding: 10px; 
                            color: #333;
                            background-color: #f8f8f8;
                        }
                        pre { 
                            margin: 1em 0; 
                            border-radius: 4px;
                            position: relative;
                        }
                        code { 
                            font-family: 'JetBrains Mono', Consolas, Monaco, 'Andale Mono', 
                                        'Ubuntu Mono', monospace; 
                        }
                        .toolbar-container {
                            position: absolute;
                            top: 0;
                            right: 0;
                            padding: 5px;
                            opacity: 0.7;
                            transition: opacity 0.3s;
                        }
                        .toolbar-container:hover {
                            opacity: 1;
                        }
                        .copy-button {
                            background: rgba(255, 255, 255, 0.2);
                            border: none;
                            border-radius: 3px;
                            color: #fff;
                            cursor: pointer;
                            font-size: 12px;
                            padding: 3px 8px;
                        }
                        .copy-button:hover {
                            background: rgba(255, 255, 255, 0.3);
                        }
                        h1, h2, h3, h4, h5, h6 {
                            margin-top: 1.5em;
                            margin-bottom: 0.5em;
                        }
                        p {
                            margin-bottom: 1em;
                        }
                        ul, ol {
                            margin-bottom: 1em;
                            padding-left: 2em;
                        }
                        blockquote {
                            border-left: 4px solid #ddd;
                            padding-left: 1em;
                            margin-left: 0;
                            color: #666;
                        }
                        table {
                            border-collapse: collapse;
                            width: 100%%;
                            margin-bottom: 1em;
                        }
                        table, th, td {
                            border: 1px solid #ddd;
                        }
                        th, td {
                            padding: 8px 12px;
                            text-align: left;
                        }
                        th {
                            background-color: #f2f2f2;
                        }
                    </style>
                </head>
                <body>
                    <div id="content"></div>
                    <script src="%s"></script>
                    <script>
                        // Will be initialized when content is loaded
                        function highlightCode() {
                            if (typeof Prism !== 'undefined') {
                                Prism.highlightAll();
                                
                                // Add copy buttons to code blocks
                                document.querySelectorAll('pre').forEach(function(pre) {
                                    const container = document.createElement('div');
                                    container.className = 'toolbar-container';
                                    
                                    const copyButton = document.createElement('button');
                                    copyButton.className = 'copy-button';
                                    copyButton.textContent = 'Copy';
                                    
                                    copyButton.addEventListener('click', function() {
                                        const code = pre.querySelector('code');
                                        const text = code.textContent;
                                        
                                        navigator.clipboard.writeText(text).then(function() {
                                            copyButton.textContent = 'Copied!';
                                            setTimeout(function() {
                                                copyButton.textContent = 'Copy';
                                            }, 2000);
                                        }).catch(function(err) {
                                            console.error('Failed to copy: ', err);
                                        });
                                    });
                                    
                                    container.appendChild(copyButton);
                                    pre.appendChild(container);
                                });
                            }
                        }
                        
                        // Call highlight when the page loads
                        document.addEventListener('DOMContentLoaded', highlightCode);
                    </script>
                </body>
                </html>
                """.formatted(PRISM_CSS_URL, PRISM_JS_URL);
    }
}
