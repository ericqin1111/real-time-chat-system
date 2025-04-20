<template>
  <div class="wechat-container">
    <AppSidebar class="sidebar" @chat-selected="handleChatSelected" />
    <ChatArea class="chat-area" :current-chat-id="currentChatId" />
  </div>
</template>

<script>
import AppSidebar from './components/AppSidebar.vue'
import ChatArea from './components/ChatArea.vue'

export default {
  name: 'App',
  components: {
    AppSidebar,
    ChatArea
  },
  data() {
    return {
      currentChatId: 1
    }
  },
  methods: {
    handleChatSelected(chatId) {
      this.currentChatId = chatId
    }
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.wechat-container {
  display: flex;
  height: 100vh;
  width: 100vw;
}

.sidebar {
  width: 280px;
  background-color: #f5f5f5;
  border-right: 1px solid #e6e6e6;
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
}
</style>