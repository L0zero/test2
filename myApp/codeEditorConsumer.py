# consumers.py

import json
from channels.generic.websocket import AsyncWebsocketConsumer

class CodeEditorConsumer(AsyncWebsocketConsumer):
    async def connect(self):
        self.room_name = "code_editor_room"
        self.room_group_name = f"editor_{self.room_name}"

        # join room
        await self.channel_layer.group_add(
            self.room_group_name,
            self.channel_name
        )

        # syn WebSocket connect
        await self.accept()

    async def disconnect(self, close_code):
        # leave room
        await self.channel_layer.group_discard(
            self.room_group_name,
            self.channel_name
        )

    # 接收 WebSocket 消息
    async def receive(self, text_data):
        data = json.loads(text_data)

        # 发送代码更改或光标位置等
        if data["type"] == "code_change":
            # 处理代码更改逻辑
            await self.channel_layer.group_send(
                self.room_group_name,
                {
                    'type': 'code_change',
                    'user': data['user'],
                    'content': data['content'],
                }
            )

        elif data["type"] == "cursor_move":
            # 处理光标位置变化
            await self.channel_layer.group_send(
                self.room_group_name,
                {
                    'type': 'cursor_move',
                    'user': data['user'],
                    'position': data['position'],
                }
            )

    # 处理广播消息
    async def code_change(self, event):
        user = event['user']
        content = event['content']

        # 向 WebSocket 发送消息
        await self.send(text_data=json.dumps({
            'type': 'code_change',
            'user': user,
            'content': content
        }))

    async def cursor_move(self, event):
        user = event['user']
        position = event['position']

        # 向 WebSocket 发送消息
        await self.send(text_data=json.dumps({
            'type': 'cursor_move',
            'user': user,
            'position': position
        }))
