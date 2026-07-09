import axios from "axios";

const AI_ASSIST_BASE_URL = 'http://localhost:9200';

export async function recommendMessagesApi(roomId) {
    const res = await axios.get(`${AI_ASSIST_BASE_URL}/aiRecommend/recommendMessages/${roomId}`, {
        withCredentials: true
    });

    return res.data;
}
