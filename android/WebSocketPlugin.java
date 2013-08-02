package com.tealeaf.plugin.plugins;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpClient.WebSocketConnectCallback;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.WebSocket.StringCallback;
import com.koushikdutta.async.http.libcore.RequestHeaders;
import com.koushikdutta.async.callback.CompletedCallback;
import com.tealeaf.logger;
import com.tealeaf.EventQueue;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Map;
import java.util.HashMap;

public class WebSocketPlugin {

    private Map<Integer, WebSocket> instances = new HashMap<Integer, WebSocket>();

    public class WebSocketEvent extends com.tealeaf.event.Event {
        String type, data;
        Integer id;

        public WebSocketEvent(Integer id, String type) {
            super("websocket");
            this.id = id;
            this.type = type;
        }

        public WebSocketEvent(Integer id, String type, String data) {
            super("websocket");
            this.id = id;
            this.type = type;
            this.data = data;
        }
    }

    public void connect(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            final Integer id = obj.getInt("id");
            String url = obj.getString("url");
            String protocol = obj.has("protocol") ? obj.getString("protocol") : null;

            AsyncHttpClient.getDefaultInstance().websocket(url, protocol, new WebSocketConnectCallback() {
                @Override
                public void onCompleted(Exception ex, WebSocket websocket) {
                    instances.put(id, websocket);

                    // onOpen
                    EventQueue.pushEvent(new WebSocketEvent(id, "open"));

                    // onMessage
                    websocket.setStringCallback(new StringCallback() {
                        public void onStringAvailable(String data) {
                            EventQueue.pushEvent(new WebSocketEvent(id, "message", data));
                        }
                    });

                    // onClose
                    websocket.setClosedCallback(new CompletedCallback() {
                        public void onCompleted(Exception ex) {
                            logger.log("{websocket} websocketOnclose - " + ex.getMessage() + ", " + ex);
                            EventQueue.pushEvent(new WebSocketEvent(id, "close"));
                            instances.remove(id);
                        }
                    });
                }
            });

        } catch (JSONException e) {
            logger.log("{websocket} Error WebSocketPlugin.connect - " + e.getMessage() + ", value: \"" + json + "\"");
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
            instances.get(id).send(data);

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
            instances.get(id).close();

        } catch (Exception e) {
            logger.log("{websocket} Error WebSocketPlugin.close - " + e.getMessage());
        }
    }
}
