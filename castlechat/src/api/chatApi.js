import axios from "axios";

export async function uploadImageApi(file, imageTarget) {
    const formData = new FormData();

    formData.append('file', file);
    formData.append('imageTarget', imageTarget);

    const res = await axios.post('/chat/image', formData, {
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    });

    if (typeof res.data === 'string') {
        return res.data;
    }

    return res.data.fileUrl
        || res.data.url
        || res.data.profileImg
        || res.data.roomThumbnail
        || res.data.imageUrl;
}

export async function sendFileApi(roomId, files, onUploadProgress) {
    const formData = new FormData();

    formData.append('roomId', roomId);

    Array.from(files).forEach(file => {
        formData.append('files', file);
    });

    const res = await axios.post('/chat/attachments', formData, {
        headers: {
            'Content-Type': 'multipart/form-data'
        },
        onUploadProgress
    });

    return res.data;
}

const ROOM_FEED_PREFIX = '[ROOM_FEED]';

// room feed는 DB에 TEXT + prefix로 저장된다. 어떤 조회 경로로 내려와도 SYSTEM 메시지로 정규화한다.
function normalizeRoomFeedMessage(message) {
    if (typeof message?.messageText === 'string' && message.messageText.startsWith(ROOM_FEED_PREFIX)) {
        return {
            ...message,
            messageType: 'SYSTEM',
            messageText: message.messageText.slice(ROOM_FEED_PREFIX.length)
        };
    }
    return message;
}

export async function loadMessagesInRoomApi(roomId, messagePageSize, beforeMessageId = null) {
    const res = await axios.get(`/room/loadMessagesInRoom/${roomId}`, {
        params: {
            beforeMessageId,
            limit: messagePageSize
        }
    });
    return (res.data ?? []).map(normalizeRoomFeedMessage);
}

export async function getMessageReactionMembersApi(roomId, messageId) {
    const res = await axios.get(`/chat/messages/${roomId}/${messageId}/reactions`);
    return res.data;
}

export async function getMessageReadersApi(roomId, messageId) {
    const res = await axios.get(`/chat/messages/${roomId}/${messageId}/readers`);
    return res.data;
}

export async function getMessageUnreadCountsApi(roomId, messageIds) {
    const res = await axios.post(`/chat/messages/${roomId}/unreadCounts`, messageIds);
    return res.data;
}
