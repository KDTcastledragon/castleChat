import axios from "axios";
import { emitWsAddFriend, emitWsRespondFriend } from "../webSocket/wsClient";

export const addFriendApi = async (targetPublicId) => {
    const emitted = emitWsAddFriend(targetPublicId);

    if (!emitted) {
        throw new Error('WebSocket 연결 안 됨');
    }
};

export const getFriendListApi = async () => {
    const res = await axios.get('/friend/getFriendList');
    return res.data;
};

export const getReceivedFriendRequestsApi = async () => {
    const res = await axios.get('/friend/getReceivedFriendRequests');
    return res.data;
};

export const respondFriendRequestApi = async ({ publicId, action }) => {
    const emitted = emitWsRespondFriend(publicId, action);

    if (!emitted) {
        throw new Error('WebSocket 연결 안 됨');
    }
};
