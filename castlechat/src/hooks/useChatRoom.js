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
            isDraft: false,
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

    function openDirectDraft(draft, fallbackFriend) {
        const friendPublicId = draft?.friendPublicId ?? fallbackFriend?.publicId;
        const friendNickname = draft?.friendNickname ?? fallbackFriend?.nickname;
        const friendProfileImg = draft?.friendProfileImg ?? fallbackFriend?.profileImg;

        dispatch(openChatWindow({
            isDraft: true,
            draftKey: `direct:${friendPublicId}`,
            roomId: null,
            targetPublicId: friendPublicId,
            roomType: 'DIRECT',
            roomName: `${friendNickname ?? '상대'}님과의 채팅방`,
            roomThumbnail: friendProfileImg,
            memberList: [
                {
                    publicId: friendPublicId,
                    nickname: friendNickname,
                    profileImg: friendProfileImg,
                    role: 'MEMBER'
                }
            ]
        }));

        nav('/chatList');
    }

    async function getOrCreateDirectRoom(friend) {
        const wsResponse = await emitWsOpenDirectChat(friend.publicId);
        const openDirectResult = wsResponse.payload;

        if (openDirectResult.roomExists && openDirectResult.enterRoomInfo) {
            openRoom(openDirectResult.enterRoomInfo);
            return openDirectResult.enterRoomInfo;
        }

        openDirectDraft(openDirectResult.draft, friend);
        return openDirectResult;
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

        const startChat = startResponse.payload;
        const roomInfo = startChat.enterRoomInfo;

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
