import { useDispatch } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import { useQueryClient, useQuery } from '@tanstack/react-query';
import { openChatWindow } from '../store/chatWindowsSlice';
import { emitWs } from '../webSocket/wsClient';
import { getOrCreateDirectRoomApi, createGroupRoomApi, enterExistedRoomApi, getMyAllRoomsApi } from '../api/chatApi';

export function useChatRoomActions() {
    const dispatch = useDispatch();
    const nav = useNavigate();
    const queryClient = useQueryClient();

    function openRoom(roomInfo) {
        emitWs("ENTER_ROOM", { roomId: roomInfo.roomId });

        dispatch(openChatWindow({
            roomId: roomInfo.roomId,
            roomType: roomInfo.roomType,
            roomName: roomInfo.customRoomName,
            roomThumbnail: roomInfo.customRoomThumbnail,
            roomMemberCount: roomInfo.roomMemberCount,
            memberList: roomInfo.memberList,
            lastReadMessageId: roomInfo.lastReadMessageId
        }));

        nav('/chatList');
    }

    async function getOrCreateDirectRoom(friend) {
        const roomInfo = await getOrCreateDirectRoomApi(friend.publicId);
        openRoom(roomInfo);
        return roomInfo;
    }

    async function createGroupRoom(roomName, roomThumbnail, selectedFriends, openAfterCreate = false) {
        if (selectedFriends.length === 0) {
            throw new Error('초대할 친구를 선택해주세요.');
        }

        const selectedFriendPublicIdList = selectedFriends.map(friend => friend.publicId);

        const roomInfo = await createGroupRoomApi({
            roomName,
            roomThumbnail,
            selectedFriendPublicIdList
        });

        queryClient.invalidateQueries({ queryKey: ['myAllRooms'] });

        if (openAfterCreate) {
            openRoom(roomInfo);
        }

        return roomInfo;
    }

    // getOr & enterExist -> 둘 다 “방 정보 반환”이라 겹쳐 보이지만, 식별자가 다르다는 점에서 역할이 분명히 달라.
    async function enterExistingRoom(roomId) {
        const roomInfo = await enterExistedRoomApi(roomId);
        openRoom(roomInfo);
        return roomInfo;
    }

    return {
        openRoom,
        getOrCreateDirectRoom,
        createGroupRoom,
        enterExistingRoom
    };
}

export function useGetMyAllRooms(enabled) {
    return useQuery({
        queryKey: ['myAllRooms'],
        queryFn: getMyAllRoomsApi,
        enabled,
        retry: false
    });
}