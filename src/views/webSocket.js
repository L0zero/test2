export default class WebSocketClient {
    constructor(url) {
      this.socket = new WebSocket(url);
    }

    get readyState() {
      return this.socket.readyState;
    }    
  
    connect(onMessageCallback) {
      this.socket.onopen = () => {
        console.log('WebSocket connected!');
      };
  
      this.socket.onmessage = (event) => {
        //console.log('Received message:', event.data);  // 检查收到的原始数据
        try {
          const message = JSON.parse(event.data);
          onMessageCallback(message);  // 调用回调传递消息
        } catch (error) {
          console.error('Error parsing message:', error);
        }
      };
  
      this.socket.onclose = () => {
        console.log('WebSocket closed!');
      };
    }
  
    sendMessage(message) {
      this.socket.send(JSON.stringify(message));
    }
  
    close() {
      this.socket.close();
    }
  }