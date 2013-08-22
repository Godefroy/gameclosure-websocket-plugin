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
    this.protocol = "";

    this.init = function(url, protocols) {
      this.id = nInstances++;
      this.url = this.URL = url;
      instances[this.id] = this;
      var params = {
        id: this.id,
        url: url
      };
      if (protocols) {
        params.protocols = protocols;
      }
      NATIVE.plugins.sendEvent("WebSocketPlugin", "connect", JSON.stringify(params));
      this.readyState = this.CONNECTING;
    };

    this.send = function(data) {
      if (this.readyState == this.CONNECTING) {
        var error = new Error("InvalidStateError: An attempt was made to use an object that is not, or is no longer, usable.");
        error.name = "InvalidStateError";
        throw error;
      }
      if (this.readyState == this.CLOSING || this.readyState == this.CLOSED) {
        throw new Error("WebSocket is already in CLOSING or CLOSED state.");
      }
      if (this.readyState == this.OPEN) {
        NATIVE.plugins.sendEvent("WebSocketPlugin", "send", JSON.stringify({
          id: this.id,
          data: data
        }));
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

  var propagateEvent = function(instance, eventType, params) {
    var event = merge(params || {}, {
      type: eventType,
      bubbles: false,
      cancelable: false
    });
    if (instance["on" + eventType]) {
      instance["on" + eventType](event);
    }
    instance.emit(eventType, event);
  };

  NATIVE.events.registerHandler("websocket:open", function(evt) {
    var instance = instances[evt.id];
    if (instance) {
      instance.readyState = instance.OPEN;
      propagateEvent(instance, "open");
    }
  });


  NATIVE.events.registerHandler("websocket:close", function(evt) {
    var instance = instances[evt.id];
    if (instance) {
      instance.readyState = instance.CLOSED;
      propagateEvent(instance, "close", {
        wasClean: evt.wasClean,
        code: evt.code,
        reason: evt.reason
      });
    }
  });

  NATIVE.events.registerHandler("websocket:message", function(evt) {
    var instance = instances[evt.id];
    if (instance) {
      propagateEvent(instance, "message", {
        data: evt.data
      });
    }
  });

  NATIVE.events.registerHandler("websocket:error", function(evt) {
    var instance = instances[evt.id];
    if (instance) {
      propagateEvent(instance, "error");
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