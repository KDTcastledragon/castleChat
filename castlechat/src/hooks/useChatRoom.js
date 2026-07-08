import { useDispatch } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import { useQueryClient, useQuery } from '@tanstack/react-query';
import { openChatWindow } from '../store/chatWindowsSlice';
import { emitWsEnterRoomRequest, emitWsOpenDirectChat, emitWsStartGroupChat } from '../webSocket/wsClient';
import { getMyAllRoomsApi } from '../api/roomApi';

export function useChatRoomActions() {
    const dispatch = useDispatch();
    const nav = useNavigate();
    const queryClient = useQueryClient();

    function openRoom(roomInfo) {
        dispatch(openChatWindow({
            roomId: roomInfo.roomId,
            roomType: roomInfo.roomType,
            roomName: roomInfo.customRoomName,
            roomThumbnail: roomInfo.customRoomThumbnail,
            customRoomBackground: roomInfo.customRoomBackground,
            messageNotificationEnabled: roomInfo.messageNotificationEnabled,
            roomMemberCount: roomInfo.roomMemberCount,
            memberList: roomInfo.memberList,
            lastReadMessageId: roomInfo.lastReadMessageId
        }));

        nav('/chatList');
    }

    async function getOrCreateDirectRoom(friend) {
        const wsResponse = await emitWsOpenDirectChat(friend.publicId);
        const roomInfo = wsResponse.payload;

        openRoom(roomInfo);
        return roomInfo;
    }

    async function createGroupRoom(roomName, roomThumbnail, selectedFriends, openAfterCreate = false) {
        if (selectedFriends.length === 0) {
            throw new Error('초대할 친구를 선택해주세요.');
        }

        const inviteMemberPublicIds = selectedFriends.map(friend => friend.publicId);
        const firstMessageText = `${roomName?.trim() || '단톡방'}이 생성되었습니다.`;

        const startResponse = await emitWsStartGroupChat(
            roomName,
            roomThumbnail,
            inviteMemberPublicIds,
            firstMessageText
        );

        const createdMessage = startResponse.payload;
        const enterResponse = await emitWsEnterRoomRequest(createdMessage.roomId);
        const roomInfo = enterResponse.payload;

        queryClient.invalidateQueries({ queryKey: ['myAllRooms'] });

        if (openAfterCreate) {
            openRoom(roomInfo);
        }

        return roomInfo;
    }

    // getOr & enterExist -> 둘 다 “방 정보 반환”이라 겹쳐 보이지만, 식별자가 다르다는 점에서 역할이 분명히 달라.
    async function enterExistingRoom(roomId) {
        const wsResponse = await emitWsEnterRoomRequest(roomId);
        const roomInfo = wsResponse.payload;

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
