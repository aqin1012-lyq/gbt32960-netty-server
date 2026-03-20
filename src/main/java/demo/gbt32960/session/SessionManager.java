package demo.gbt32960.session;

import io.netty.channel.Channel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private final Map<String, Channel> vinToChannel = new ConcurrentHashMap<>();
    private final Map<Channel, String> channelToVin = new ConcurrentHashMap<>();

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public void bindVin(String vin, Channel channel) {
        Channel old = vinToChannel.put(vin, channel);
        channelToVin.put(channel, vin);
        if (old != null && old != channel) {
            channelToVin.remove(old);
            old.close();
        }
    }

    public void unbindVin(String vin, Channel channel) {
        Channel bound = vinToChannel.get(vin);
        if (bound == channel) {
            vinToChannel.remove(vin);
        }
        channelToVin.remove(channel);
    }

    public void removeChannel(Channel channel) {
        String vin = channelToVin.remove(channel);
        if (vin != null) {
            Channel bound = vinToChannel.get(vin);
            if (bound == channel) {
                vinToChannel.remove(vin);
            }
        }
    }

    public Channel getChannelByVin(String vin) {
        return vinToChannel.get(vin);
    }
}
