package com.tealeaf.plugin.plugins;
import de.tavendo.autobahn.WebSocket;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketConnectionHandler;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketOptions;
import com.tealeaf.logger;
import com.tealeaf.EventQueue;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class WebSocketPlugin {

    private Map<Integer, WebSocketConnection> instances = new HashMap<Integer, WebSocketConnection>();

    public class WebSocketEventOpen extends com.tealeaf.event.Event {
        Integer id;
        public WebSocketEventOpen(Integer id) {
            super("websocket:open");
            this.id = id;
        }
    }
    public class WebSocketEventClose extends com.tealeaf.event.Event {
        Integer id;
        Integer code;
        String reason;
        public WebSocketEventClose(Integer id, int code, String reason) {
            super("websocket:close");
            this.id = id;
            this.code = code;
            this.reason = reason;
        }
    }
    public class WebSocketEventMessage extends com.tealeaf.event.Event {
        Integer id;
        String data;
        public WebSocketEventMessage(Integer id, String data) {
            super("websocket:message");
            this.id = id;
            this.data = data;
        }
    }
    public class WebSocketEventError extends com.tealeaf.event.Event {
        Integer id;
        String message;
        public WebSocketEventError(Integer id, String message) {
            super("websocket:error");
            this.id = id;
            this.message = message;
        }
    }

    public void connect(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            final Integer id = obj.getInt("id");
            String url = obj.getString("url");

            String[] protocols = null;
            if (obj.has("protocols")) {
                try {
                    protocols = new String[1];
                    protocols[0] = obj.getString("protocols");
                } catch (JSONException ex) {
                    try {
                        JSONArray protocolsJSON = obj.getJSONArray("protocols");
                        protocols = new String[protocolsJSON.length()];
                        for(int i = 0; i < protocolsJSON.length(); i++) {
                            protocols[i] = protocolsJSON.getString(i);
                        }
                    } catch (JSONException exx) { }
                }
            }

            try {

                WebSocketOptions options = new WebSocketOptions();
                List<BasicNameValuePair> headers = null;

                final WebSocketConnection websocket = new WebSocketConnection();

                websocket.connect(url, protocols, new WebSocketConnectionHandler() {
                    @Override
                    public void onOpen() {
                        instances.put(id, websocket);
                        EventQueue.pushEvent(new WebSocketEventOpen(id));
                        logger.log("{websocket} onOpen");
                    }

                    @Override
                    public void onTextMessage(String data) {
                        EventQueue.pushEvent(new WebSocketEventMessage(id, data));
                    }

                    @Override
                    public void onClose(int code, String reason) {
                        logger.log("{websocket} onClose - Code: " + code + " - Reason: " + reason);
                        EventQueue.pushEvent(new WebSocketEventClose(id, code, reason));
                        instances.remove(id);
                    }
                }, options, headers);

            } catch (WebSocketException wsException) {
                logger.log("{websocket} Error WebSocketPlugin.connect - " + wsException.getMessage());
                EventQueue.pushEvent(new WebSocketEventError(id, wsException.getMessage()));
            }

        } catch (JSONException jsonException) {
            logger.log("{websocket} Error WebSocketPlugin.connect - " + jsonException.getMessage() + ", value: \"" + json + "\"");
        }
    }

    public void send(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            final Integer id = obj.getInt("id");
            String data = obj.getString("data");
            if (!instances.containsKey(id)) {
                throw new Exception("WebSocket n°" + id + " not found");
            }
            instances.get(id).sendTextMessage(data);

        } catch (JSONException e) {
            logger.log("{websocket} Error WebSocketPlugin.send - " + e.getMessage() + ", value: \"" + json + "\"");
        } catch (Exception e) {
            logger.log("{websocket} Error WebSocketPlugin.send - " + e.getMessage());
        }
    }

    public void close(String idStr) {
        try {
            Integer id = new Integer(idStr);
            if (!instances.containsKey(id)) {
                throw new Exception("WebSocket n°" + id + " not found");
            }
            instances.get(id).disconnect();

        } catch (Exception e) {
            logger.log("{websocket} Error WebSocketPlugin.close - " + e.getMessage());
        }
    }
}
