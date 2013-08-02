import device;
import event.Emitter as Emitter;

// If on true native mobile platform,
if (device.isMobileNative) {
  logger.log("{websocket} Installing JS component for native");

  var instances = [];
  var nInstances = 0;

  /**
   * WebSocket
   *
   * @todo fix onclose
   * @todo implement onerror
   * @see http://www.w3.org/TR/websockets/
   */
  GLOBAL.WebSocket = Class(Emitter, function() {

    this.CONNECTING = 0;
    this.OPEN = 1;
    this.CLOSING = 2;
    this.CLOSED = 3;

    this.init = function(url, protocol) {
      this.id = nInstances++;
      this.url = this.URL = url;
      this.protocol = protocol;
      instances[this.id] = this;
      var params = {
        id: this.id,
        url: url
      };
      if (protocol) {
        params.protocol = protocol;
      }
      NATIVE.plugins.sendEvent("WebSocketPlugin", "connect", JSON.stringify(params));
      this.readyState = this.CONNECTING;
    };

    this.send = function(data) {
      if (this.readyState == this.CONNECTING) {
        throw "INVALID_STATE_ERR: Web Socket connection has not been established";
      }
      if (this.readyState == this.OPEN) {
        NATIVE.plugins.sendEvent("WebSocketPlugin", "send", JSON.stringify({
          id: this.id,
          data: data
        }));
        return true;
      } else {
        return false;
      }
    };

    this.close = function() {
      if (this.readyState == this.CONNECTING ||  this.readyState == this.OPEN) {
        this.readyState = this.CLOSING;
        NATIVE.plugins.sendEvent("WebSocketPlugin", "close", String(this.id));
      }
    };

    this.addEventListener = this.on;
    this.removeEventListener = this.removeListener;
    this.dispatchEvent = this.emit;

  });


  // Websocket events
  NATIVE.events.registerHandler('websocket', function(evt) {
    var instance = instances[evt.id];
    if (instance) {
      var newEvent = {
        type: evt.type,
        bubbles: false,
        cancelable: false
      };

      switch (evt.type) {
        case "open":
          instance.readyState = instance.OPEN;
          break;

        case "close":
          instance.readyState = instance.CLOSED;
          newEvent.wasClean = evt.wasClean;
          newEvent.code = evt.code;
          newEvent.reason = evt.reason;
          break;

        case "message":
          newEvent.data = evt.data;
          break;
      }

      if (instance["on" + evt.type]) {
        instance["on" + evt.type](newEvent);
      }
      instance.emit(evt.type, newEvent);
    }

  });


} else {
  logger.log("{websocket} Skipping installing JS wrapper on non-native target");
}


/**
 * Socket.io
 */
import addons.websocket.js.socketio as socketio;
GLOBAL.io = socketio;
