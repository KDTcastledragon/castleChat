
import axios from "axios";

export const getMyAllRoomsApi = async () => {
    const res = await axios.get(`/room/getMyAllChatRooms`);
    return res.data;
}

export async function updateMyRoomSettingsApi(data) {
    const res = await axios.post('/room/updateMyRoomSettings', data);
    return res.data;
}

export async function loadRoomNoticesApi(roomId, beforeRoomNoticeId = null, limit = 20) {
    const res = await axios.get(`/room/${roomId}/notices`, {
        params: {
            beforeRoomNoticeId,
            limit
        }
    });

    return res.data;
}
