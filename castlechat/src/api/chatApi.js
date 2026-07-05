import axios from "axios";

export async function sendFileApi(roomId, files) {
    const res = await axios.post(`/chat/`)
}

export async function loadMessagesInRoomApi(roomId, messagePageSize) {
    const res = await axios.get(`/room/loadMessagesInRoom/${roomId}`, {
        params: {
            limit: messagePageSize
        }
    });
    return res.data;
}