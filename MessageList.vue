<template>
    <div class="message-list">
        <div v-for="message in messages" :key="message.id" :class="['message', { 'me': message.isMe }]">
            <div v-if="!message.isMe" class="sender">{{ message.sender }}</div>
            <div class="content">{{ message.content }}</div>
            <div class="time">{{ message.time }}</div>
        </div>
    </div>
</template>

<script>
export default {
    name: 'MessageList',
    props: {
        messages: {
            type: Array,
            required: true
        }
    }
}
</script>

<style scoped>
.message-list {
    display: flex;
    flex-direction: column;
    gap: 15px;
}

.message {
    max-width: 70%;
    padding: 8px 12px;
    border-radius: 4px;
    background-color: #f0f0f0;
    align-self: flex-start;
}

.message.me {
    background-color: #95ec69;
    align-self: flex-end;
}

.sender {
    font-size: 12px;
    color: #666;
    margin-bottom: 4px;
}

.content {
    word-break: break-word;
}

.time {
    font-size: 10px;
    color: #999;
    text-align: right;
    margin-top: 4px;
}
</style>