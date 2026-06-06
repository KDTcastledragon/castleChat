import axios from "axios";

export const getMyAllRoomsApi = async () => {
    const res = await axios.get(`/chat/getMyAllRooms`);
    return res.data;
}