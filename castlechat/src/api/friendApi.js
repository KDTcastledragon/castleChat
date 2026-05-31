import axios from "axios";

export const addFriendApi = async (targetPublicId) => {
    await axios.post('/friend/addFriend', {
        publicId: targetPublicId
    });
};

export const getFriendListApi = async () => {
    const res = await axios.get('/friend/getFriendList');
    return res.data;
};

export const getReceivedFriendRequestsApi = async () => {
    const res = await axios.get('/friend/getReceivedFriendRequests');
    return res.data;
};
