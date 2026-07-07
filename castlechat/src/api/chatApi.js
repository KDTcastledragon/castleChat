import axios from "axios";

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
