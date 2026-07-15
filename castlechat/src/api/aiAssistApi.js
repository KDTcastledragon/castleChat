import axios from "axios";

// const AI_ASSIST_BASE_URL = 'http://localhost:9200'; // Dev Env
const AI_ASSIST_BASE_URL = ''; // NginX : on premise

export async function recommendMessagesApi(roomId) {
    const res = await axios.get(`${AI_ASSIST_BASE_URL}/aiRecommend/recommendMessages/${roomId}`, {
        withCredentials: true
    });

    return res.data;
}

export async function refineMessageToneApi(messageText, tone) {
    const res = await axios.post(`${AI_ASSIST_BASE_URL}/aiRecommend/refineMessage`, {
        messageText,
        tone
    }, {
        withCredentials: true
    });

    return res.data.refinedMessage;
}

export async function recommendPersonalizedMessagesApi(roomId, targetPublicId, relationshipType) {
    const res = await axios.post(`${AI_ASSIST_BASE_URL}/aiRecommend/recommendPersonalizedMessages`, {
        roomId,
        targetPublicId,
        relationshipType
    }, {
        withCredentials: true
    });

    return res.data;
}
