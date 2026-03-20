package demo.gbt32960;

import demo.gbt32960.codec.Gbt32960FrameDecoder;
import demo.gbt32960.codec.Gbt32960FrameEncoder;
import demo.gbt32960.codec.Gbt32960Protocol;
import demo.gbt32960.handler.Gbt32960ServerHandler;
import demo.gbt32960.model.Packet32960;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

public class Application {

    public static void main(String[] args) throws Exception {
        int port = 32960;
        boolean demoClient = false;
        for (String arg : args) {
            if ("--demo-client".equals(arg)) {
                demoClient = true;
            }
        }

        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    .addLast("idle", new IdleStateHandler(90, 0, 0, TimeUnit.SECONDS))
                                    .addLast("decoder", new Gbt32960FrameDecoder())
                                    .addLast("encoder", new Gbt32960FrameEncoder())
                                    .addLast("handler", new Gbt32960ServerHandler());
                        }
                    });

            ChannelFuture bindFuture = bootstrap.bind(port).sync();
            System.out.println("[SERVER] started at 0.0.0.0:" + port);

            if (demoClient) {
                runDemoClient(port);
            }

            bindFuture.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully().sync();
            workerGroup.shutdownGracefully().sync();
        }
    }

    private static void runDemoClient(int port) {
        Thread thread = new Thread(() -> {
            NioEventLoopGroup group = new NioEventLoopGroup(1);
            try {
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(group)
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ch.pipeline()
                                        .addLast(new Gbt32960FrameDecoder())
                                        .addLast(new Gbt32960FrameEncoder())
                                        .addLast(new SimpleChannelInboundHandler<Packet32960>() {
                                            @Override
                                            public void channelActive(ChannelHandlerContext ctx) {
                                                String vin = "LJ8ABC12345678901";
                                                byte[] loginBody = Gbt32960Protocol.buildDemoLoginBody();
                                                ctx.writeAndFlush(new Packet32960(0x01, 0xFE, vin, 0x01, loginBody.length, loginBody, 0));
                                                byte[] realtimeBody = Gbt32960Protocol.buildDemoRealtimeBody();
                                                ctx.writeAndFlush(new Packet32960(0x02, 0xFE, vin, 0x01, realtimeBody.length, realtimeBody, 0));
                                            }

                                            @Override
                                            protected void channelRead0(ChannelHandlerContext ctx, Packet32960 msg) {
                                                System.out.println("[CLIENT] recv response: " + msg.toPrettyJson());
                                            }
                                        });
                            }
                        });
                ChannelFuture future = bootstrap.connect("127.0.0.1", port).sync();
                future.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                group.shutdownGracefully();
            }
        }, "demo-client-thread");
        thread.setDaemon(true);
        thread.start();
    }
}
