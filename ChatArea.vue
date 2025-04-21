<template>
  <div class="chat-container">
    <div class="chat-header">
      <div class="title">{{ currentChat.name }}</div>
    </div>
    
    <MessageList :messages="currentChat.messages" class="message-list" />
    
    <MessageInput @send-message="handleSendMessage" />
  </div>
</template>

<script>
import MessageList from './MessageList.vue'
import MessageInput from './MessageInput.vue'

export default {
  name: 'ChatArea',
  components: {
    MessageList,
    MessageInput
  },
  props: {
    currentChatId: {
      type: Number,
      required: true
    }
  },
  data() {
    return {
      chats: {
        1: {
          name: '前端开发群',
          messages: [
            { id: 1, sender: '张三', content: '早上好！', time: '09:30', isMe: false },
            { id: 2, sender: '我', content: '早上好！今天有什么计划？', time: '09:32', isMe: true },
            { id: 3, sender: '李四', content: '早上好', time: '09:33', isMe: false },
          ]
        },
        2: {
          name: '张三',
          messages: [
            { id: 1, sender: '张三', content: '项目进展如何？', time: '昨天', isMe: false },
            { id: 2, sender: '我', content: '基本完成了', time: '昨天', isMe: true },
          ]
        }
      }
    }
  },
  computed: {
    currentChat() {
      return this.chats[this.currentChatId]
    }
  },
  methods: {
    handleSendMessage(content) {
      const newMessage = {
        id: Date.now(),
        sender: '我',
        content,
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        isMe: true
      }
      this.currentChat.messages.push(newMessage)
    }
  }
}
</script>

<style scoped>
.chat-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chat-header {
  padding: 15px;
  border-bottom: 1px solid #e6e6e6;
  font-weight: bold;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 15px;
}
</style>