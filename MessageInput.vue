<template>
    <div class="message-input">
        <textarea v-model="message" @keydown.enter.exact.prevent="sendMessage" placeholder="输入消息..."></textarea>
        <button @click="sendMessage">发送</button>
    </div>
</template>

<script>
export default {
    name: 'MessageInput',
    data() {
        return {
            message: ''
        }
    },
    methods: {
        sendMessage() {
            if (this.message.trim()) {
                this.$emit('send-message', this.message.trim())
                this.message = ''
            }
        }
    }
}
</script>

<style scoped>
.message-input {
    padding: 15px;
    border-top: 1px solid #e6e6e6;
    display: flex;
    gap: 10px;
}

textarea {
    flex: 1;
    padding: 10px;
    border-radius: 4px;
    border: 1px solid #e6e6e6;
    resize: none;
    height: 60px;
    outline: none;
}

button {
    padding: 0 20px;
    background-color: #07c160;
    color: white;
    border: none;
    border-radius: 4px;
    cursor: pointer;
}

button:hover {
    background-color: #06ad56;
}
</style>