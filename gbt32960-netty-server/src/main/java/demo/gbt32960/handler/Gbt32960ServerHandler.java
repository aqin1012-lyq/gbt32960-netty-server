package demo.gbt32960.handler;

import demo.gbt32960.model.CommandBody;
import demo.gbt32960.model.Packet32960;
import demo.gbt32960.parser.BodyParser;
import demo.gbt32960.response.ResponseFactory;
import demo.gbt32960.session.SessionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;

public class Gbt32960ServerHandler extends SimpleChannelInboundHandler<Packet32960> {

    private final BodyParser bodyParser = new BodyParser();
    private final SessionManager sessionManager = SessionManager.getInstance();

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("[SERVER] channel active: " + ctx.channel().remoteAddress());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet32960 msg) {
        System.out.println("[SERVER] recv packet: " + msg.toPrettyJson());
        CommandBody body = bodyParser.parse(msg);
        System.out.println("[SERVER] parsed body: " + body.toPrettyJson());

        if (msg.cmd() == 0x01) {
            sessionManager.bindVin(msg.vin(), ctx.channel());
        } else if (msg.cmd() == 0x04) {
            sessionManager.unbindVin(msg.vin(), ctx.channel());
        }

        ctx.writeAndFlush(ResponseFactory.buildCommonResponse(msg));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        sessionManager.removeChannel(ctx.channel());
        System.out.println("[SERVER] channel inactive: " + ctx.channel().remoteAddress());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            System.out.println("[SERVER] idle timeout, close channel: " + ctx.channel().remoteAddress());
            sessionManager.removeChannel(ctx.channel());
            ctx.close();
            return;
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.err.println("[SERVER] exception: " + cause.getMessage());
        sessionManager.removeChannel(ctx.channel());
        ctx.close();
    }
}
