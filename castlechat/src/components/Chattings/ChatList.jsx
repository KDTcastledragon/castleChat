import './ChatList.css';
import { useCallback, useEffect, useRef, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useMe } from '../../hooks/useAuthUser';
import { useChatRoomActions, useGetMyAllRooms } from '../../hooks/useChatRoom';
import { registerGlobalWsHandler } from '../../webSocket/wsClient';

function ChatList() {
    // const { data: myAllRooms = [], refetch: refetchMyAllRooms } = useGetMyAllRooms(!!me);
    const { data: me, isLoading: isCheckingLogin } = useMe();
    const { data: myAllRooms = [], isLoading, isError } = useGetMyAllRooms(!!me);
    const queryClient = useQueryClient();
    const [visibleRooms, setVisibleRooms] = useState([]);
	const visibleRoomsRef = useRef([]);
	const localUnreadCountRef = useRef({});

    const { enterExistingRoom } = useChatRoomActions();

    useEffect(() => {
		const nextRooms = myAllRooms.map(room => {
			const roomKey = String(room.roomId);
			const localUnreadCount = localUnreadCountRef.current[roomKey];

			if (localUnreadCount === undefined) {
				return room;
			}

			return {
				...room,
				unreadMessageCount: localUnreadCount,
				unreadCount: localUnreadCount
			};
		});

		visibleRoomsRef.current = nextRooms;
		setVisibleRooms(nextRooms);
    }, [myAllRooms]);

	const updateRoomLists = useCallback((updater) => {
		const nextVisibleRooms = updater(visibleRoomsRef.current);

		visibleRoomsRef.current = nextVisibleRooms;
		setVisibleRooms(nextVisibleRooms);

		queryClient.setQueryData(['myAllRooms'], prevRooms => {
			if (!Array.isArray(prevRooms)) return prevRooms;
			return updater(prevRooms);
		});
	}, [queryClient]);

	const resetRoomUnreadCount = useCallback((roomId) => {
		const roomKey = String(roomId);
		localUnreadCountRef.current[roomKey] = 0;

		updateRoomLists(rooms => rooms.map(room =>
			Number(room.roomId) === Number(roomId)
				? { ...room, unreadMessageCount: 0, unreadCount: 0 }
				: room
		));
	}, [updateRoomLists]);

    useEffect(() => {
        if (!me) return;

        return registerGlobalWsHandler((wsEvt) => {
            if (wsEvt.wsType === 'MSG_READ') {
				const readPosition = wsEvt.payload;

				if (readPosition?.readerPublicId === me.publicId && readPosition?.roomId) {
					resetRoomUnreadCount(readPosition.roomId);
				}

				return;
			}

			if (wsEvt.wsType !== 'CHAT_ROOM_UPDATED' && wsEvt.wsType !== 'MSG_CREATED') {
                return;
            }

			const payload = wsEvt.payload;
			if (!payload?.roomId) return;

			const roomKey = String(payload.roomId);
			const currentRoom = visibleRoomsRef.current.find(room => Number(room.roomId) === Number(payload.roomId));

			if (!currentRoom) {
				queryClient.invalidateQueries({ queryKey: ['myAllRooms'] });
				return;
			}

			const isMyMessage = payload.senderPublicId === me.publicId;
			const previewText = payload.previewText ?? payload.messageText ?? currentRoom.lastMessage ?? '';
			const lastMessageAt = payload.notifiedAt ?? payload.createdAt ?? currentRoom.lastMessageAt;
			const currentUnreadCount = Number(localUnreadCountRef.current[roomKey]
				?? currentRoom.unreadMessageCount
				?? currentRoom.unreadCount
				?? 0);
			const shouldIncreaseUnread = wsEvt.wsType === 'CHAT_ROOM_UPDATED' && !isMyMessage;
			const nextUnreadCount = shouldIncreaseUnread ? currentUnreadCount + 1 : currentUnreadCount;

			localUnreadCountRef.current[roomKey] = nextUnreadCount;

			updateRoomLists(prevRooms => {
				const nextRooms = prevRooms.map(room => Number(room.roomId) === Number(payload.roomId)
					? {
						...room,
						lastMessage: previewText,
						lastMessageAt,
						unreadMessageCount: nextUnreadCount,
						unreadCount: nextUnreadCount
					}
					: room);

                return nextRooms.sort((a, b) => {
                    const aTime = a.lastMessageAt ? new Date(a.lastMessageAt).getTime() : 0;
                    const bTime = b.lastMessageAt ? new Date(b.lastMessageAt).getTime() : 0;
                    return bTime - aTime;
                });
            });
        });
	}, [me, queryClient, resetRoomUnreadCount, updateRoomLists]);

    function formatRoomTime(value) {
        if (!value) return '';

        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return '';

        return date.toTimeString().slice(0, 5);
    }

    function handleEnterRoom(roomId) {
		resetRoomUnreadCount(roomId);

        enterExistingRoom(roomId);
    }

    if (isCheckingLogin || isLoading) {
        return <div className='ChatListContainer'>채팅방 목록 불러오는 중...</div>;
    }

    if (!me) {
        return <div className='ChatListContainer'>로그인이 필요합니다.</div>;
    }

    if (isError) {
        return <div className='ChatListContainer'>채팅방 목록 불러오기 실패</div>;
    }


    // ======< return >=======================================================================================================
    return (
        <div className='ChatListContainer'>
            <div className='chatList'>
                <div className='chatListTitle'><span>채팅방 목록</span></div>
                {visibleRooms.map((r) => {
                    const unreadCount = Number(r.unreadMessageCount ?? r.unreadCount ?? 0);

                    return (
                    <div className='chatListBox' key={r.roomId}>
                        <img
                            className="chatListRoomThumbnail"
                            src={r.customRoomThumbnail || '/images/mococo_question.png'}
                            alt={r.customRoomName}
                        />

                        <div className="chatListMainInfo">
                            <div className="chatListRoomNameLine">
                                <span className="chatListRoomName">{r.customRoomName}</span>
                                {r.roomType !== 'DIRECT' && (
                                    <span className="chatListMemberCount">{r.activeMemberCount}명</span>
                                )}
                            </div>

                            <div className="chatListLastMessage">
                                {r.lastMessage || '아직 메시지가 없습니다.'}
                            </div>
                        </div>

                        <div className="chatListSubInfo">
                            <div className="chatListLastTime">{formatRoomTime(r.lastMessageAt)}</div>

                            {unreadCount > 0 && (
                                <div className="chatListUnreadBadge">{unreadCount > 999 ? '999+' : unreadCount}</div>
                            )}
                        </div>

                        <button onClick={() => handleEnterRoom(r.roomId)}>채팅</button>
                    </div>
                    );
                })}
            </div>
        </div>
    );
}

export default ChatList;
