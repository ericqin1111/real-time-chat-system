<template>
    <div class="sidebar-container">
        <div class="search-bar">
            <input type="text" placeholder="搜索" />
        </div>

        <div class="menu-tabs">
            <div v-for="tab in tabs" :key="tab.id" :class="{ active: activeTab === tab.id }"
                @click="activeTab = tab.id">
                {{ tab.name }}
            </div>
        </div>

        <div class="chat-list">
            <div v-for="chat in filteredChats" :key="chat.id" class="chat-item"
                :class="{ active: activeChat === chat.id }" @click="selectChat(chat.id)">
                <div class="avatar">{{ chat.name.charAt(0) }}</div>
                <div class="info">
                    <div class="name">{{ chat.name }}</div>
                    <div class="preview">{{ chat.lastMessage }}</div>
                </div>
                <div class="time">{{ chat.time }}</div>
            </div>
        </div>
    </div>
</template>

<script>
export default {
    name: 'AppSidebar',
    data() {
        return {
            activeTab: 'chat',
            tabs: [
                { id: 'chat', name: '聊天' },
                { id: 'contact', name: '联系人' },
                { id: 'collect', name: '收藏' },
            ],
            chats: [
                { id: 1, name: '前端开发群', lastMessage: '大家早上好！', time: '09:30', unread: 2, type: 'group' },
                { id: 2, name: '张三', lastMessage: '项目进展如何？', time: '昨天', unread: 0, type: 'private' },
                { id: 3, name: '李四', lastMessage: '晚上一起吃饭吗？', time: '星期一', unread: 1, type: 'private' },
                { id: 4, name: '项目讨论组', lastMessage: 'UI设计稿已更新', time: '2023/12/1', unread: 0, type: 'group' },
            ],
            activeChat: 1
        }
    },
    computed: {
        filteredChats() {
            return this.chats
        }
    },
    methods: {
        selectChat(chatId) {
            this.activeChat = chatId
            this.$emit('chat-selected', chatId)
        }
    }
}
</script>

<style scoped>
.sidebar-container {
    height: 100%;
    display: flex;
    flex-direction: column;
}

.search-bar {
    padding: 10px;
    border-bottom: 1px solid #e6e6e6;
}

.search-bar input {
    width: 100%;
    padding: 8px;
    border-radius: 4px;
    border: 1px solid #e6e6e6;
    outline: none;
}

.menu-tabs {
    display: flex;
    border-bottom: 1px solid #e6e6e6;
}

.menu-tabs div {
    flex: 1;
    text-align: center;
    padding: 12px 0;
    cursor: pointer;
}

.menu-tabs div.active {
    background-color: #e6e6e6;
}

.chat-list {
    flex: 1;
    overflow-y: auto;
}

.chat-item {
    display: flex;
    padding: 12px;
    border-bottom: 1px solid #f0f0f0;
    cursor: pointer;
}

.chat-item:hover {
    background-color: #f9f9f9;
}

.chat-item.active {
    background-color: #e6e6e6;
}

.avatar {
    width: 40px;
    height: 40px;
    border-radius: 4px;
    background-color: #07c160;
    color: white;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 10px;
}

.info {
    flex: 1;
    overflow: hidden;
}

.name {
    font-weight: bold;
    margin-bottom: 4px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.preview {
    font-size: 12px;
    color: #999;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.time {
    font-size: 12px;
    color: #999;
    margin-left: 10px;
}
</style>