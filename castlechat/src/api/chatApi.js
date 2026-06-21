import axios from "axios";

export const getMyAllRoomsApi = async () => {
    const res = await axios.get(`/chat/getMyAllChatRooms`);
    return res.data;
}

export async function getOrCreateDirectRoomApi(friendPublicId) {
    const res = await axios.post('/chat/getOrCreateDirectRoom', {
        friendPublicId: friendPublicId
    });

    return res.data;
}

export async function createGroupRoomApi(data) {
    const res = await axios.post('/chat/createGroupRoom', data);
    return res.data;
}

// ====== enterRoom은 ws로 roomSession등록 + chatWindow Rendering만 하기때문에 http Request 필요 없음.!
// 라고 생각했는데, 사실 틀렸음. 1:1챗은 getOrCre 함수가 기존 room의 존재와 관계없이 room정보를 한꺼번에 다 주지만,
// cregroupRoom은 room'생성'에 기본적으로 포커스가 맞춰져있고, roomInfo를 주긴하지만, 이걸 enter때도 쓸수가 없잖아.
// enter때는 '기존'의 정보를 'get'만 해야하기때문에. 근데 creGroupRoom은 '새로운 room'의 info를 주는거니까
// 이미 'created'된 enterRoom의 info를 get하기 위해서는 결국 http request api가 필요하다.
export async function enterExistedRoomApi(roomId) {
    const res = await axios.get(`/chat/enterExistedRoom/${roomId}`);

    return res.data;
}

export async function leftRoomApi(roomId) {
    const res = await axios.post(`/chat/leftRoom`, {
        roomId: roomId
    });

    return res.data;
}

export async function deleteMsgApi(msgId) {
    const res = await axios.post(`/chat/deleteMsg`, {
        msgId: msgId
    });

    return res.data;
}