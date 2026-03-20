package demo.gbt32960.response;

import demo.gbt32960.model.Packet32960;

public final class ResponseFactory {

    private ResponseFactory() {
    }

    public static Packet32960 buildCommonResponse(Packet32960 request) {
        byte[] body = new byte[0];
        return new Packet32960(request.cmd(), 0x01, request.vin(), 0x01, body.length, body, 0);
    }
}
