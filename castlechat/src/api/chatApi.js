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

export async function loadMessagesInRoomApi(roomId, messagePageSize, beforeMessageId = null) {
    const res = await axios.get(`/room/loadMessagesInRoom/${roomId}`, {
        params: {
            beforeMessageId,
            limit: messagePageSize
        }
    });
    return res.data;
}

export async function getMessageReactionMembersApi(roomId, messageId) {
    const res = await axios.get(`/chat/messages/${roomId}/${messageId}/reactions`);
    return res.data;
}

export async function getMessageReadersApi(roomId, messageId) {
    const res = await axios.get(`/chat/messages/${roomId}/${messageId}/readers`);
    return res.data;
}
