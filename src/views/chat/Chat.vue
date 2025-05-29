<script>
import axios from "axios"
import getIdenticon from "@/utils/identicon";
import { computed } from "vue";
import topicSetting from "@/utils/topic-setting";

import Sidebar from "./AbstractSidebar.vue"; // 引入侧边栏组件

import discussionService from "@/utils/discussion";


export default {
  name: "Chat",

  data() {
    return {
      selectedRoom: 0,
      messageInput: '',
      chatRooms: [],
      createSheet: false,
      inviteSheet: false,
      expelSheet: false,
      createRoomName: '',
      createRoomDesc: '',
      projectPopulation: [],
      selectedPopulation: [],
      expelledPopulation: [],
      inviteNominees: computed(() => {
        if (!this.chatRooms.length || this.selectedRoom < 0) return [];
        return this.projectPopulation.filter((item) => {
          return !this.chatRooms[this.selectedRoom]?.users?.find((user) => {
            return user.userId === item.peopleId;
          });
        });
      }),
      inviteSelected: [],
      messageServiceAvailable: false,
      sidebarCollapsed: true, // 控制侧边栏折叠状态，默认折叠
      allowQA: true, // 是否允许生成问答元组
      pendingInitialMessage: null, // 添加待发送的初始消息
      wsStabilityTimer: null, // 添加WebSocket稳定性检查定时器
    }
  },
  inject: {
    user: { default: null },
    proj: { default: null }
  },
  components: {
    Sidebar, // 注册侧边栏组件
  },
  created() {
    // this.initWS(1)
    this.updateChatRooms()
    this.updatePopulation()
    this.initMessagingService()
  },
  watch: {
    // 同时监听路由参数和聊天室数据
    '$route.query.highlight': {
      handler(newVal) {
        this.handleHighlight(newVal);
      },
      immediate: true 
    },
    chatRooms: {
      handler(newRooms) {
        if (newRooms.length > 0) {
          this.handleHighlight(this.$route.query.highlight);
        }
      },
      deep: true
    },
    '$route.query.mess': {
      handler(newVal) {
        if (newVal) {
          console.log("newVal " + newVal)
          // 等待 chatRooms 更新和 WebSocket 初始化完成
          this.$nextTick(() => {
            this.autoSendInitMessage(newVal);
          });
        }
      },
      immediate: true,
    }
  },
  methods: {
    getIdenticon,
    handleHighlight(highlightId) {
      if (!highlightId || this.chatRooms.length === 0) return;
      
      const index = this.chatRooms.findIndex(r => r.id == highlightId);
      if (index > -1) {
        this.selectedRoom = index;
        this.$router.replace({ query: {} }); // 清除高亮参数
      }
    },
    async updateChatRooms() {
      console.log('updating chat rooms...')
      // 保存现有的ws和history
      let tempWS = {}
      let tempHistory = {}
      this.chatRooms.forEach((item, index) => {
        if (item.ws !== null) {
          tempWS[item.id] = item.ws
          tempHistory[item.id] = item.history
        }
      })

      // 保存当前选中的聊天室ID
      const currentRoomId = this.chatRooms[this.selectedRoom]?.id;

      try {
        const res = await axios.post('/api/chat/discussions', {
          projectId: this.proj.projectId,
          userId: this.user.id
        });
        
        if (res.data.errcode === 0) {
          let chatRooms = res.data.data.discussions.map((item, index) => {
            return {
              id: item.roomId,
              title: item.roomName,
              desc: item.outline,
              users: item.users,
              history: [],
              selectedUser: null
            }
          });
          
          // 去除chatRooms中id重复的项目
          let temp = {}
          chatRooms.forEach((item, index) => {
            if (temp[item.id] === undefined) {
              temp[item.id] = item
            }
          });
          
          // 更新chatRooms数组
          this.chatRooms = Object.values(temp);
          
          // 设置selectedRoom
          if (this.chatRooms.length > 0) {
            // 尝试找到之前选中的聊天室
            const newIndex = this.chatRooms.findIndex(room => room.id === currentRoomId);
            this.selectedRoom = newIndex >= 0 ? newIndex : 0;
          } else {
            this.selectedRoom = -1;
          }
        } else {
          throw new Error('get discussion list failure with non 0 errcode (' + res.data.errcode + ')');
        }
      } catch (err) {
        console.error(err);
        this.$message({
          type: 'error',
          message: 'get discussion list failure with error: ' + err
        });
      } finally {
        // 重新分配ws
        for (let i = 0; i < this.chatRooms.length; i++) {
          const room = this.chatRooms[i];
          if (tempWS[room.id] !== undefined && tempWS[room.id].readyState === WebSocket.OPEN) {
            room.ws = tempWS[room.id];
            tempWS[room.id] = undefined;
            room.history = tempHistory[room.id];
          } else {
            room.ws = this.initWS(room.id);
            await this.getChatHistory(room);
          }
        }

        // 关闭多余的ws
        for (const [key, value] of Object.entries(tempWS)) {
          if (value !== undefined) {
            value.close(1000); // 使用正常关闭代码
          }
        }
      }
    },
    updatePopulation() {
      axios.post('/api/plan/showPersonList', {
        projectId: this.proj.projectId,
        userId: this.user.id
      }).then((res) => {
        if (res.data.errcode !== 0) {
          throw new Error('get person list failure with non 0 errcode (' + res.data.errcode + ')')
        } else {
          this.projectPopulation = res.data.data.filter((item, index) => {
            return item.peopleId !== this.user.id
          })
          this.expelledPopulation = res.data.data.filter((item, index) => {
            return item.peopleId !== this.user.id
          })
          console.log(res.data.data)
        }
      }).catch((err) => {
        console.error(err)
        this.$message({
          type: 'error',
          message: 'get person list failure with error: ' + err
        })
      })
    },
    initWS(rid) {
      console.log('initWS: connecting to ws://8.140.206.102:8000/ws/chat/' + this.user.id.toString() + '/' + rid.toString());
      const socket = new WebSocket('ws://8.140.206.102:8000/ws/chat/' + this.user.id.toString() + '/' + rid.toString());

      const onopen = (e) => {
        console.log('socket opened');
        // 清除之前的稳定性检查定时器
        if (this.wsStabilityTimer) {
          clearTimeout(this.wsStabilityTimer);
        }
        
        // 设置新的稳定性检查定时器
        this.wsStabilityTimer = setTimeout(() => {
          if (socket.readyState === WebSocket.OPEN) {
            console.log('WebSocket connection is stable');
            // 如果有待发送的初始消息，现在发送
            if (this.pendingInitialMessage) {
              console.log('Sending pending initial message');
              this.messageInput = this.pendingInitialMessage;
              this.sendMsg();
              this.pendingInitialMessage = null;
              const query = { ...this.$route.query };
              delete query.mess;
              this.$router.replace({ query });
            }
          }
        }, 3000); // 等待3秒确保连接稳定
      };

      socket.onopen = onopen;

      const onmessage = (fromName, fromId, content, time) => {
        // 找到对应的聊天室
        let room = this.chatRooms.find((item) => item.id === rid);
        if (!room) {
          console.error('room not found');
        } else {
          // 将消息插入到聊天室历史记录中
          room.history.splice(0, 0, {
            from: fromName,
            type: 'group',
            content: content,
            time: time
          });

          // 如果页面不可见且消息来自其他用户，则发送通知
          if (this.messageServiceAvailable && document.visibilityState === 'hidden' && fromName !== this.user.name) {
            const notification = new Notification(`来自讨论室 ${room.title} 的一条新消息`, {
              body: `${fromName}: ${content}`,
              icon: getIdenticon(fromName, 100, 'user')
            });
          }
        }
      };

      socket.onmessage = function (event) {
        console.log('Message from server ', event.data);
        const data = JSON.parse(event.data);
        onmessage(data.senderName, data.senderId, data.content, data.time);
      };

      socket.onerror = function (event) {
        console.error('WebSocket error observed:', event);
      };

      socket.onclose = function (e) {
        console.log('Socket is closed.', e.reason);
        // 如果不是正常关闭，尝试重新连接
        if (e.code !== 1000) {
          console.log('Attempting to reconnect...');
          setTimeout(() => {
            const room = this.chatRooms.find((item) => item.id === rid);
            if (room) {
              room.ws = this.initWS(rid);
            }
          }, 1000);
        }
      }.bind(this);

      return socket;
    },
    sendMsg() {
      const room = this.chatRooms[this.selectedRoom];
            if (!room.ws || room.ws.readyState !== WebSocket.OPEN) {
              console.warn('WebSocket未就绪，稍后重试...');
              setTimeout(() => this.sendMsg(), 100); // 延迟重试
              return;
            }
      if ((this.messageInput || '').length > 500) {
        this.$message({
          type: 'error',
          message: '消息太长了'
        })
        return
      }
      if (this.messageInput === '') {
        this.$message({
          type: 'error',
          message: '消息不能为空'
        })
        return
      }

      const ws = this.chatRooms[this.selectedRoom]?.ws;
      const readyState = ws?.readyState ?? -1; // 获取 WebSocket 的 readyState 值
      if (!ws || readyState !== 1) { // 检查 WebSocket 是否已连接
        this.$message({
          type: 'error',
          message: `连接未建立，无法发送消息 (readyState: ${readyState})`
        });
        return;
      }

      console.log('will send: ' + this.messageInput);

      // 确保将网址转换为超链接
      const urlRegex = /(https?:\/\/[^\s]+)/g;
      const formattedMessage = this.messageInput.replace(
        urlRegex,
        '<a href="$1" target="_blank" style="color: blue; text-decoration: underline;">$1</a>'
      );

      this.chatRooms[this.selectedRoom].ws.send(JSON.stringify({
        sender: this.user.id,
        type: 1,
        message: formattedMessage // 确保发送的是格式化后的消息
      }));

      this.messageInput = '';
    },
    initMessagingService() {
          if ("Notification" in window) {
            console.log('Notification is supported, initing messaging service')
            Notification.requestPermission().then(permission => {
              if (permission === "granted") {
                console.log('Notification permission granted')
                this.messageServiceAvailable = true;
                const notification = new Notification('已注册消息通知', {
                  icon: '../../favicon.ico',
                  body: "消息通知已开启，JiHub会在收到新消息时显示提醒",
                })
              } else {
                console.log('Notification permission denied')
                this.messageServiceAvailable = false;
              }
            })
          } else {
            console.log('Notification permission denied')
            this.messageServiceAvailable = false;
          }
        
    },
    expelUser(room, expelledUser) {
      console.log(room, expelledUser)
      axios.post('/api/chat/deletePerson', {
        userId: expelledUser.userId,
        roomId: room.id
      }).finally(() => {
        this.messageInput = `${this.user.name}` + '将' + `${expelledUser.userName}` + '移出了讨论室'
        this.sendMsg()
        setTimeout(() => {
          // 更新聊天室列表并保持当前选中的聊天室
          const currentRoomId = this.chatRooms[this.selectedRoom].id
          this.updateChatRooms().then(() => {
            const roomIndex = this.chatRooms.findIndex(room => room.id === currentRoomId)
            if (roomIndex !== -1) {
              this.selectedRoom = roomIndex
            }
          })
          this.expelSheet = false
        }, 2000)
      })
    },
    getDarkColor: topicSetting.getDarkColor,
    getTopicColor: topicSetting.getColor,
    getLinearGradient: topicSetting.getLinearGradient,
    getRadialGradient: topicSetting.getRadialGradient,
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed; // 切换侧边栏状态
    },
    getChatHistory(room) {
      axios.post('/api/chat/getRoomMessages', {
        roomId: room.id,
        userId: this.user.id
      }).then((res) => {
        console.log(res.data)
        room.history = []
        if (res.data.errcode === 0) {
          res.data.data.messages.reverse().map((item, index) => {
            room.history.push({
              from: item.senderName,
              content: item.content,
              time: item.time,
              type: 'group'
            })
          })
          // 去除room.history中time相同的项目
          let temp = {}
          room.history.forEach((item, index) => {
            if (temp[item.time] === undefined) {
              temp[item.time] = item
            }
          })
          room.history = Object.values(temp)
        } else {
          throw new Error('get room messages failure with non 0 errcode (' + res.data.errcode + ')')
        }
      }).catch((err) => {
        console.error(err)
        this.$message({
          type: 'error',
          message: 'get room messages failure with error: ' + err
        })
      })
    },
    inviteUserToChat(roomId, userId) {
      axios.post('/api/chat/addPerson', {
        roomId: roomId,
        userId: userId,
      }).then((res) => {
        console.log(res)
      }).finally(() => {
        this.messageInput = `${this.user.name}` + '邀请' + `${this.inviteNominees[this.inviteSelected].peopleName}` + '加入了聊天室'
        this.sendMsg()
        setTimeout(() => {
          // 更新聊天室列表并保持当前选中的聊天室
          const currentRoomId = this.chatRooms[this.selectedRoom].id
          this.updateChatRooms().then(() => {
            const roomIndex = this.chatRooms.findIndex(room => room.id === currentRoomId)
            if (roomIndex !== -1) {
              this.selectedRoom = roomIndex
            }
          })
          this.inviteSheet = false
        }, 2000)
      })
    },
    createChatRoom() {
      if (!this.createRoomName) {
        this.$message({
          type: 'error',
          message: '请输入聊天室名称'
        })
        return
      }

      // 准备用户列表，只包含被邀请的成员
      const users = new Set() // 使用 Set 来确保用户 ID 唯一
      this.selectedPopulation.forEach(user => {
        users.add(user.peopleId)
      })

      axios.post('/api/chat/createRoom', {
        projectId: this.proj.projectId,
        roomName: this.createRoomName,
        outline: this.createRoomDesc,
        currentUserId: this.user.id,
        users: Array.from(users) // 转换回数组
      }).then((res) => {
        if (res.data.errcode === 0) {
          this.$message({
            type: 'success',
            message: '聊天室创建成功'
          })
          this.createSheet = false
          this.createRoomName = ''
          this.createRoomDesc = ''
          this.selectedPopulation = []
          this.expelledPopulation = []
          // 更新聊天室列表并跳转到新创建的聊天室
          this.updateChatRooms().then(() => {
            // 找到新创建的聊天室并选中它
            const newRoomId = res.data.data.roomId
            const newRoomIndex = this.chatRooms.findIndex(room => room.id === newRoomId)
            if (newRoomIndex !== -1) {
              this.selectedRoom = newRoomIndex
            }
          })
        } else {
          throw new Error('创建聊天室失败，错误码：' + res.data.errcode)
        }
      }).catch((err) => {
        console.error(err)
        this.$message({
          type: 'error',
          message: '创建聊天室失败：' + err.message
        })
      })
    },
    deleteRoom(room) {
      this.$confirm('确定要退出并删除此聊天室吗？这不会删除其他成员的聊天记录。', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        axios.post('/api/chat/deletePerson', {
          roomId: room.id,
          userId: this.user.id
        }).then(res => {
          if (res.data.errcode === 0) {
            // 保存当前选中的聊天室
            const currentSelectedRoom = this.selectedRoom;
            
            // 临时切换到要删除的聊天室
            const roomToDeleteIndex = this.chatRooms.findIndex(r => r.id === room.id);
            if (roomToDeleteIndex !== -1) {
              this.selectedRoom = roomToDeleteIndex;
              // 发送系统消息
              this.messageInput = `${this.user.name}已离开群聊`;
              this.sendMsg();
            }

            // Update chat rooms after a short delay to allow message to send
            setTimeout(() => {
              // 先关闭当前聊天室的 WebSocket 连接
              if (room.ws) {
                room.ws.close();
              }
              
              // 更新聊天室列表
              this.updateChatRooms().then(() => {
                this.$message.success('已退出聊天室');
                
                // 只有在删除最后一个聊天室时才重置状态
                if (this.chatRooms.length === 0) {
                  this.selectedRoom = -1;
                  this.messageInput = '';
                  this.sidebarCollapsed = true;
                } else {
                  // 如果还有其他聊天室，选择第一个
                  this.selectedRoom = 0;
                }
              });
            }, 500);

          } else {
            this.$message.error(res.data.message || '退出聊天室失败');
          }
        }).catch(err => {
          console.error('Error deleting room:', err);
          this.$message.error('退出聊天室失败');
        });
      }).catch(() => {
        this.$message({ type: 'info', message: '已取消操作' });
      });
    },
    autoSendInitMessage(mess) {
      console.log("mess " + mess)
      try {
        const content = mess;
        // 保存待发送的消息
        this.pendingInitialMessage = content;
        
        const room = this.chatRooms[this.selectedRoom];
        if (room && room.ws && room.ws.readyState === WebSocket.OPEN) {
          // 如果WebSocket已经连接，等待稳定性检查
          console.log('WebSocket is open, waiting for stability check');
        } else {
          // 如果WebSocket未连接，等待连接建立
          console.log('WebSocket is not open, waiting for connection');
          const checkWS = () => {
            if (room && room.ws && room.ws.readyState === WebSocket.OPEN) {
              console.log('WebSocket connected, waiting for stability check');
            } else {
              setTimeout(checkWS, 500);
            }
          };
          checkWS();
        }
      } catch (e) {
        console.error('初始化消息解析失败:', e);
      }
    },
  },
  beforeRouteLeave(to, from, next) {
    console.log('leaving chat room, closing all ws')
    this.chatRooms.forEach((item, index) => {
      item.ws.close()
    })
    next()
  },
}
</script>

<template>
  <v-container fluid class="chat-container pa-0">
    <v-row no-gutters class="fill-height">
      <!-- 左侧聊天室列表 -->
      <v-col cols="3" class="chat-sidebar">
        <v-list class="chat-list pa-0">
          <v-list-item-group v-model="selectedRoom" mandatory>
            <v-list-item v-for="item in chatRooms" :key="item.id" class="chat-list-item">
              <v-list-item-content>
                <v-list-item-title class="chat-room-title">
                  <span :style="'color: ' + getDarkColor(user.topic)">{{ item.title }}</span>
                </v-list-item-title>
                <v-list-item-subtitle class="chat-room-subtitle">
                  {{ item.history.length === 0 ? '还没有消息哦' : item.history[0].from + ' : ' + item.history[0].content }}
                  <span class="chat-time">{{ item.history.length === 0 ? '' : new Date(item.history[0].time).toLocaleTimeString()}}</span>
                </v-list-item-subtitle>
              </v-list-item-content>
              <v-list-item-action>
                <v-btn
                  icon
                  small
                  color="error"
                  @click.stop="deleteRoom(item)"
                  title="删除聊天室"
                >
                  <v-icon>mdi-delete</v-icon>
                </v-btn>
              </v-list-item-action>
            </v-list-item>
          </v-list-item-group>
          
          <v-divider></v-divider>
          
          <v-list-item ripple @click="createSheet = !createSheet" class="create-room-btn">
            <v-list-item-content>
              <span :style="'color: ' + getDarkColor(user.topic)">创建新的聊天室</span>
            </v-list-item-content>
            <v-list-item-icon>
              <v-icon :color="getDarkColor(user.topic)">mdi-plus-circle</v-icon>
            </v-list-item-icon>
          </v-list-item>
        </v-list>
      </v-col>

      <!-- 中间聊天内容 -->
      <v-col :cols="sidebarCollapsed ? 9 : 6" class="chat-main">
        <v-card v-if="chatRooms.length > 0 && selectedRoom >= 0" flat class="chat-card">
          <!-- 聊天室头部 -->
          <v-card-title class="chat-header">
            <div class="d-flex align-center">
              <span class="chat-title">{{ chatRooms[selectedRoom]?.title }}</span>
            </div>
            <div class="chat-actions">
              <v-switch
                v-model="allowQA"
                label="允许生成问答元组"
                class="qa-switch"
                dense
                hide-details
              ></v-switch>
              <v-btn icon @click="toggleSidebar" :color="getDarkColor(user.topic)" title="展开侧边栏">
                <v-icon>{{ sidebarCollapsed ? "mdi-chevron-right" : "mdi-chevron-left" }}</v-icon>
              </v-btn>
            </div>
          </v-card-title>

          <!-- 聊天室成员 -->
          <v-card-subtitle class="chat-subtitle">
            <div class="d-flex align-center">
              <v-icon
                v-if="chatRooms[selectedRoom]?.ws?.readyState === 1"
                class="green--text mr-2"
              >mdi-circle</v-icon>
              <v-icon v-else class="yellow--text mr-2">mdi-circle</v-icon>
              <span>{{ chatRooms[selectedRoom]?.desc || '这个聊天室没有简介哦' }}</span>
              <span class="ml-2">| {{ chatRooms[selectedRoom]?.users?.length }} 名成员</span>
            </div>
            <div class="member-list">
              <v-avatar
                v-for="user in chatRooms[selectedRoom]?.users"
                :key="user.userId"
                size="32"
                class="mr-1"
                @click="chatRooms[selectedRoom].selectedUser = chatRooms[selectedRoom].selectedUser === user ? null : user"
              >
                <v-img :src="getIdenticon(user.userName, 50, 'user')"></v-img>
              </v-avatar>
              <v-btn
                icon
                small
                class="ml-1"
                @click="inviteSheet = !inviteSheet"
                :color="getDarkColor(user.topic)"
              >
                <v-icon>mdi-plus</v-icon>
              </v-btn>
            </div>
          </v-card-subtitle>

          <!-- 选中用户操作面板 -->
          <v-expand-transition>
            <div v-if="chatRooms[selectedRoom]?.selectedUser" class="user-panel">
              <v-divider></v-divider>
              <v-card-actions class="py-2">
                <v-avatar size="32" class="mr-2">
                  <v-img :src="getIdenticon(chatRooms[selectedRoom].selectedUser.userName, 50, 'user')"></v-img>
                </v-avatar>
                <span class="font-weight-bold">{{ chatRooms[selectedRoom].selectedUser.userName }}</span>
                <span v-if="chatRooms[selectedRoom].selectedUser.userName === user.name">（您自己）</span>
                <v-spacer></v-spacer>
                <v-btn
                  v-if="chatRooms[selectedRoom].selectedUser.userName !== user.name"
                  color="error"
                  small
                  @click="expelSheet = !expelSheet"
                >
                  <v-icon small class="mr-1">mdi-alert</v-icon>
                  移除群聊
                </v-btn>
              </v-card-actions>
            </div>
          </v-expand-transition>

          <!-- 聊天消息区域 -->
          <v-card-text class="chat-messages pa-0">
            <div class="messages-container">
              <div
                v-for="(item, index) in [...chatRooms[selectedRoom]?.history].reverse()"
                :key="index"
                class="message-wrapper"
              >
                <!-- 系统消息 -->
                <div v-if="item.content.includes('移出了讨论室') || item.content.includes('加入了聊天室') || item.content.includes('已离开群聊')" class="system-message">
                  <div class="system-message-content">{{ item.content }}</div>
                </div>
                <!-- 他人消息 -->
                <div v-else-if="user.name !== item.from" class="message other-message">
                  <v-avatar size="40" class="mr-2">
                    <v-img :src="getIdenticon(item.from, 50, 'user')"></v-img>
                  </v-avatar>
                  <div class="message-content">
                    <div class="message-info">
                      <span class="message-sender">{{ item.from }}</span>
                      <span class="message-time">{{ new Date(item.time).getDate() === new Date().getDate() ? new Date(item.time).toLocaleTimeString() : new Date(item.time).toLocaleString() }}</span>
                    </div>
                    <div class="message-bubble" v-html="item.content"></div>
                  </div>
                </div>
                <!-- 自己的消息 -->
                <div v-else class="message self-message">
                  <div class="message-content">
                    <div class="message-info">
                      <span class="message-time">{{ new Date(item.time).getDate() === new Date().getDate() ? new Date(item.time).toLocaleTimeString() : new Date(item.time).toLocaleString() }}</span>
                      <span class="message-sender">您</span>
                    </div>
                    <div class="message-bubble" :style="getLinearGradient(user.topic)" v-html="item.content"></div>
                  </div>
                  <v-avatar size="40" class="ml-2">
                    <v-img :src="getIdenticon(item.from, 50, 'user')"></v-img>
                  </v-avatar>
                </div>
              </div>
            </div>
          </v-card-text>

          <!-- 消息输入区域 -->
          <v-card-actions class="chat-input">
            <v-text-field
              v-model="messageInput"
              placeholder="输入消息..."
              rounded
              filled
              counter="500"
              :append-outer-icon="'mdi-send'"
              @click:append-outer="sendMsg"
              @keydown.enter="sendMsg"
              hide-details
              class="message-input"
            ></v-text-field>
          </v-card-actions>
        </v-card>
        <v-card v-else flat class="chat-card d-flex align-center justify-center">
          <div class="text-center">
            <v-icon size="64" color="grey lighten-1">mdi-chat-outline</v-icon>
            <div class="text-h6 mt-4 grey--text">没有可用的聊天室</div>
            <v-btn
              color="primary"
              class="mt-4"
              @click="createSheet = true"
            >
              创建新的聊天室
            </v-btn>
          </div>
        </v-card>
      </v-col>

      <!-- 右侧侧边栏 -->
      <v-col v-if="!sidebarCollapsed && chatRooms.length > 0 && selectedRoom >= 0" cols="3" class="sidebar-column">
        <Sidebar
          :collapsed="sidebarCollapsed"
          :allowQA="allowQA"
          :roomId="String(chatRooms[selectedRoom]?.id)"
          :projectId="String(proj.projectId)"
          @toggle="toggleSidebar"
        />
      </v-col>
    </v-row>

    <!-- 创建聊天室对话框 -->
    <v-dialog v-model="createSheet" max-width="800px">
      <v-card>
        <v-card-title>创建新的聊天室</v-card-title>
        <v-card-text>
          <v-row>
            <v-col cols="12" md="6">
              <v-text-field
                label="聊天室名称"
                v-model="createRoomName"
                outlined
                dense
              ></v-text-field>
            </v-col>
            <v-col cols="12" md="6">
              <v-text-field
                label="聊天室简介"
                v-model="createRoomDesc"
                outlined
                dense
              ></v-text-field>
            </v-col>
          </v-row>
          <v-row>
            <v-col cols="12" md="6">
              <div class="text-subtitle-1 mb-2">聊天室成员</div>
              <v-chip
                pill
                class="ma-1"
                color="primary"
                @click="() => {
                  this.$message({
                    type: 'error',
                    message: '把自己踢出聊天室，这也太狠了！'
                  })
                }"
              >
                <v-avatar left>
                  <v-img :src="getIdenticon(user.name, 50, 'user')"></v-img>
                </v-avatar>
                您
              </v-chip>
              <v-chip
                v-for="item in selectedPopulation"
                :key="item.peopleId"
                class="ma-1"
                color="primary"
                @click="() => {
                  expelledPopulation.push(item)
                  selectedPopulation.splice(selectedPopulation.indexOf(item), 1)
                }"
              >
                <v-avatar left>
                  <v-img :src="getIdenticon(item.peopleName, 50, 'user')"></v-img>
                </v-avatar>
                {{ item.peopleName }}
              </v-chip>
            </v-col>
            <v-col cols="12" md="6">
              <div class="text-subtitle-1 mb-2">
                {{ expelledPopulation.length ? '可选成员' : '大家都在聊天室里了哦' }}
              </div>
              <v-chip
                v-for="item in expelledPopulation"
                :key="item.peopleId"
                class="ma-1"
                outlined
                @click="() => {
                  selectedPopulation.push(item)
                  expelledPopulation.splice(expelledPopulation.indexOf(item), 1)
                }"
              >
                <v-avatar left>
                  <v-img :src="getIdenticon(item.peopleName, 50, 'user')"></v-img>
                </v-avatar>
                {{ item.peopleName }}
              </v-chip>
            </v-col>
          </v-row>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn
            color="primary"
            @click="createChatRoom"
            :disabled="!createRoomName"
          >
            创建聊天室
          </v-btn>
          <v-btn
            text
            @click="createSheet = false"
          >
            取消
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- 邀请成员对话框 -->
    <v-dialog v-model="inviteSheet" max-width="500px">
      <v-card>
        <v-card-title>邀请新成员加入聊天室</v-card-title>
        <v-card-text v-if="inviteNominees.length">
          <v-chip-group
            v-model="inviteSelected"
            mandatory
            active-class="primary--text"
          >
            <v-chip
              v-for="item in inviteNominees"
              :key="item.peopleId"
              class="ma-1"
              @click="() => {
                expelledPopulation.push(item)
                selectedPopulation.splice(selectedPopulation.indexOf(item), 1)
              }"
            >
              <v-avatar left>
                <v-img :src="getIdenticon(item.peopleName, 50, 'user')"></v-img>
              </v-avatar>
              {{ item.peopleName }}
            </v-chip>
          </v-chip-group>
          <v-divider class="my-3"></v-divider>
          <div v-if="inviteNominees[inviteSelected]" class="text-center">
            要邀请 {{ inviteNominees[inviteSelected].peopleName }} 进入群聊"{{ chatRooms[selectedRoom].title }}"吗？
          </div>
        </v-card-text>
        <v-card-text v-else>
          <div class="text-center">项目所有的成员都在聊天室里了！</div>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn
            v-if="inviteNominees.length"
            color="primary"
            @click="() => inviteUserToChat(chatRooms[selectedRoom].id, inviteNominees[inviteSelected].peopleId)"
          >
            确定邀请
          </v-btn>
          <v-btn
            text
            @click="inviteSheet = false"
          >
            {{ inviteNominees.length ? '取消' : '确定' }}
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- 移除成员确认对话框 -->
    <v-dialog v-model="expelSheet" max-width="400px">
      <v-card>
        <v-card-title>删除成员确认</v-card-title>
        <v-card-text>
          警告！这样做会导致成员 {{ chatRooms[selectedRoom]?.selectedUser?.userName }}
          无法访问聊天室"{{ chatRooms[selectedRoom]?.title }}"。
          您确定要删除成员 {{ chatRooms[selectedRoom]?.selectedUser?.userName }} 吗？
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn
            color="primary"
            @click="expelSheet = false"
          >
            取消
          </v-btn>
          <v-btn
            color="error"
            @click="() => expelUser(chatRooms[selectedRoom], chatRooms[selectedRoom].selectedUser)"
          >
            <v-icon small class="mr-1">mdi-alert</v-icon>
            确定删除
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-container>
</template>

<style>
.chat-container {
  height: 100vh;
  background-color: #f5f5f5;
}

.chat-sidebar {
  background-color: #fff;
  border-right: 1px solid #e0e0e0;
  height: 100vh;
}

.chat-list {
  height: 100%;
  overflow-y: auto;
}

.chat-list-item {
  border-bottom: 1px solid #f0f0f0;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.chat-list-item .v-list-item__content {
  flex: 1;
  min-width: 0;
}

.chat-list-item .v-list-item__action {
  margin-left: 8px;
  flex-shrink: 0;
}

.chat-room-title {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 4px;
}

.chat-room-subtitle {
  font-size: 14px;
  color: #666;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-time {
  font-size: 12px;
  color: #999;
}

.create-room-btn {
  margin-top: 8px;
  padding: 12px 16px;
}

.chat-main {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.chat-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chat-header {
  background-color: #fff;
  border-bottom: 1px solid #e0e0e0;
  padding: 12px 16px;
}

.chat-title {
  font-size: 18px;
  font-weight: 500;
}

.chat-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chat-subtitle {
  background-color: #f8f8f8;
  padding: 8px 16px;
}

.member-list {
  display: flex;
  align-items: center;
  margin-top: 8px;
}

.user-panel {
  background-color: #f8f8f8;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background-color: #f5f5f5;
  height: calc(100vh - 200px); /* 减去头部和输入框的高度 */
  position: relative;
}

.messages-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
  min-height: 100%;
  overflow-y: auto;
  justify-content: flex-end; /* 确保内容从底部开始显示 */
}

.message-wrapper {
  display: flex;
  flex-direction: column;
  animation: fadeIn 0.3s ease;
  margin-bottom: 8px; /* 添加消息间距 */
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message {
  display: flex;
  align-items: flex-start;
  max-width: 80%;
  word-wrap: break-word;
}

.other-message {
  align-self: flex-start;
}

.self-message {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message-content {
  display: flex;
  flex-direction: column;
}

.message-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.message-sender {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.message-time {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}

.message-bubble {
  padding: 8px 12px;
  border-radius: 12px;
  background-color: #fff;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  word-break: break-word;
  max-width: 100%;
}

.self-message .message-bubble {
  background-color: #95ec69;
}

.chat-input {
  background-color: #fff;
  border-top: 1px solid #e0e0e0;
  padding: 12px 16px;
  position: sticky;
  bottom: 0;
  z-index: 1;
}

.message-input {
  margin: 0;
}

.qa-switch {
  margin: 0;
}

/* 自定义滚动条样式 */
::-webkit-scrollbar {
  width: 6px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
}

::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #555;
}

.sidebar-column {
  background-color: #fff;
  border-left: 1px solid #e0e0e0;
  height: 100vh;
  overflow: hidden;
}

.chat-main {
  transition: all 0.3s ease;
}

.system-message {
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 8px 0;
  width: 100%;
}

.system-message-content {
  background-color: rgba(0, 0, 0, 0.1);
  color: #666;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  max-width: 80%;
  text-align: center;
  word-break: break-word;
}
</style>