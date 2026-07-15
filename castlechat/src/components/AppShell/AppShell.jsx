
import './AppShell.css';

import { useEffect, useRef, useState } from "react";
import { useSelector, useDispatch } from 'react-redux';
import { useQueryClient } from '@tanstack/react-query';
import { useLocation, useNavigate } from 'react-router-dom';

import Header from "../Home/Header"
import RouteBody from "../Home/RouteBody"

import { useMe } from "../../hooks/useAuthUser";
import { closeChatWindow, moveChatWindow, focusChatWindow, clearChatWindows } from '../../store/chatWindowsSlice';
import { connectWs, disconnectWs, emitWsEnterRoom, emitWsExitRoom, registerGlobalWsHandler, registerWsCloseListener } from "../../webSocket/wsClient";
import { addAcceptedFriendToCache, addReceivedFriendRequestToCache, removeReceivedFriendRequestFromCache } from '../../hooks/useFriend';
import { useChatRoomActions } from '../../hooks/useChatRoom';
import { getMyAllRoomsApi, updateMyRoomSettingsApi } from '../../api/roomApi';

import ChatBox from '../Chattings/ChatBox';

// 1. me 조회
// 2. WebSocket 연결
// 6. chatWindows
// 7. openDirectRoom
// 8. openRoomFromList
// 9. ChatBox 렌더링

function AppShell() {
    const { data: me, isLoading: isCheckingLogin } = useMe();
    const [toastList, setToastList] = useState([]);
    const queryClient = useQueryClient();
    const { enterExistingRoom } = useChatRoomActions();
    const navigator = useNavigate();
    const location = useLocation();

    const dispatch = useDispatch();
    const chatWindows = useSelector(state => state.chatWindows.windows);
    const canShowChatWindows = location.pathname === '/chatList';
    const isPublicRoute = location.pathname === '/login' || location.pathname === '/join';
    const activeRoomId = chatWindows[0]?.roomId ?? null;
    const wasChatRouteRef = useRef(canShowChatWindows);
    const routeExitedRoomIdRef = useRef(null);

    // ======== WebSocket 연결 + 유저 목록 ======= ※ useEffect쓰는 이유? "컴포넌트가 화면에 등장했을 때" 웹소켓 연결하려고. 처음 렌더링될 때만 딱! 한! 번! 실행되어야한다.
    useEffect(() => {
        if (isCheckingLogin) return;

        if (!me && !isPublicRoute) {
            navigator('/login', { replace: true });
            return;
        }

        if (!me) return;

        connectWs();
    }, [me, isCheckingLogin, isPublicRoute, navigator]);

    useEffect(() => {
        return () => {
            console.log(`AppShell UNMOUNT`);
            disconnectWs('AppShell_UNMOUNT');
        };
    }, []);

    useEffect(() => {
        const unregister = registerWsCloseListener(() => {
            dispatch(clearChatWindows());
        });

        return unregister;
    }, [dispatch]);

    useEffect(() => {
        const wasChatRoute = wasChatRouteRef.current;

        if (wasChatRoute && !canShowChatWindows && activeRoomId != null) {
            emitWsExitRoom(activeRoomId);
            routeExitedRoomIdRef.current = activeRoomId;
        }

        if (
            !wasChatRoute
            && canShowChatWindows
            && activeRoomId != null
            && Number(routeExitedRoomIdRef.current) === Number(activeRoomId)
        ) {
            emitWsEnterRoom(activeRoomId);
            routeExitedRoomIdRef.current = null;
        }

        wasChatRouteRef.current = canShowChatWindows;
    }, [activeRoomId, canShowChatWindows]);

    useEffect(() => {
        if (!me) {

            return;
        }


        return registerGlobalWsHandler(async (wsEvt) => {
            const payload = wsEvt.payload ?? {};
            let text = '';
			let toastType = 'default';
			let toastRoom = null;

            if (wsEvt.wsType === 'FRIEND_REQUEST_RECEIVED') {
                addReceivedFriendRequestToCache(queryClient, payload);
                queryClient.invalidateQueries({ queryKey: ['receivedFriendRequests'] });
                queryClient.invalidateQueries({ queryKey: ['searchUsers'] });
                text = `${payload.requesterNickname ?? payload.requesterPublicId ?? '상대'}님에게 친구 요청이 왔습니다.`;
            }

            if (wsEvt.wsType === 'FRIEND_REQUEST_RESPONDED') {
                addAcceptedFriendToCache(queryClient, payload, me.publicId);
                removeReceivedFriendRequestFromCache(queryClient, payload);
                queryClient.invalidateQueries({ queryKey: ['friends'] });
                queryClient.invalidateQueries({ queryKey: ['receivedFriendRequests'] });
                queryClient.invalidateQueries({ queryKey: ['searchUsers'] });
                text = payload.friendStatus === 'ACCEPTED'
                    ? `${payload.targetNickname ?? payload.targetPublicId ?? '상대'}님이 친구 요청을 수락했습니다.`
                    : `${payload.targetNickname ?? payload.targetPublicId ?? '상대'}님이 친구 요청을 거절했습니다.`;
            }

            if (wsEvt.wsType === 'RESPOND_FRIEND_OK') {
                addAcceptedFriendToCache(queryClient, payload, me.publicId);
                removeReceivedFriendRequestFromCache(queryClient, payload);
                queryClient.invalidateQueries({ queryKey: ['friends'] });
                queryClient.invalidateQueries({ queryKey: ['receivedFriendRequests'] });
                queryClient.invalidateQueries({ queryKey: ['searchUsers'] });
            }

            if (wsEvt.wsType === 'FRIEND_ONLINE_NOTIFICATION') {
                text = payload.notificationText ?? `${payload.nickname ?? '친구'}님이 접속했습니다.`;
            }

            if (wsEvt.wsType === 'FRIEND_PROFILE_UPDATED') {
                queryClient.invalidateQueries({ queryKey: ['friends'] });
                queryClient.invalidateQueries({ queryKey: ['myAllRooms'] });
            }

            if (wsEvt.wsType === 'CHAT_MESSAGE_NOTIFICATION') {
                text = `${payload.senderNickname ?? '상대'}: ${payload.previewText ?? ''}`;
				toastType = 'chat';
				let roomList = queryClient.getQueryData(['myAllRooms']);

				if (!Array.isArray(roomList)) {
					try {
						roomList = await getMyAllRoomsApi();
						queryClient.setQueryData(['myAllRooms'], roomList);
					} catch {
						roomList = [];
					}
				}

				toastRoom = roomList.find(room => Number(room.roomId) === Number(payload.roomId)) ?? null;
			}

			if (wsEvt.wsType === 'ROOM_INVITED') {
				queryClient.invalidateQueries({ queryKey: ['myAllRooms'] });
				text = `${payload.requesterNickname ?? '상대'}님이 채팅방에 초대했습니다.`;
            }

			if (wsEvt.wsType === 'ROOM_KICKED' && (payload.targetPublicIds ?? []).includes(me.publicId)) {
				dispatch(closeChatWindow(`room:${payload.roomId}`));
				queryClient.setQueryData(['myAllRooms'], rooms => Array.isArray(rooms)
					? rooms.filter(room => Number(room.roomId) !== Number(payload.roomId))
					: rooms);
				queryClient.invalidateQueries({ queryKey: ['myAllRooms'] });
				text = '채팅방에서 추방되었습니다.';
			}

            if (wsEvt.wsType === 'ADD_FRIEND_FAIL' || wsEvt.wsType === 'RESPOND_FRIEND_FAIL') {
                text = payload?.errorMessage ?? '친구 처리 실패';
            }

            if (!text) return;

            const toastId = crypto.randomUUID();
			setToastList(prev => [...prev, {
				toastId,
				text,
				toastType,
				roomId: payload.roomId ?? null,
				roomName: toastRoom?.customRoomName ?? null,
				roomThumbnail: toastRoom?.customRoomThumbnail ?? payload.senderProfileImg ?? null,
				messageNotificationEnabled: toastRoom?.messageNotificationEnabled ?? true
			}].slice(-4));

            setTimeout(() => {
                setToastList(prev => prev.filter(toast => toast.toastId !== toastId));
			}, 4000);
        });
	}, [me, queryClient, dispatch]);

	function closeToast(toastId) {
		setToastList(prev => prev.filter(toast => toast.toastId !== toastId));
	}

	async function openToastRoom(toast) {
		if (!toast.roomId) return;

		closeToast(toast.toastId);

		try {
			await enterExistingRoom(toast.roomId);
		} catch (e) {
			console.error('알림 채팅방 입장 실패', e);
		}
	}

	async function turnOffToastRoomNotification(e, toast) {
		e.stopPropagation();

		try {
			const cachedRoomList = queryClient.getQueryData(['myAllRooms']);
			const roomList = Array.isArray(cachedRoomList) ? cachedRoomList : await getMyAllRoomsApi();
			const room = roomList.find(item => Number(item.roomId) === Number(toast.roomId));

			if (!room) return;

			await updateMyRoomSettingsApi({
				roomId: room.roomId,
				customRoomName: room.customRoomName,
				customRoomThumbnail: room.customRoomThumbnail,
				customRoomBackground: room.customRoomBackground,
				messageNotificationEnabled: false
			});

			setToastList(prev => prev.filter(item => Number(item.roomId) !== Number(toast.roomId)));
			queryClient.invalidateQueries({ queryKey: ['myAllRooms'] });
		} catch (error) {
			console.error('알림에서 방 알림 끄기 실패', error);
		}
	}

    return (
        <>
            <Header />

            {/* 디스코드식 싱글뷰 : /chatList에서는 좌(방 목록) + 우(활성 방) 2패널로 배치한다. 다른 라우트는 기존 그대로. */}
            <div className={canShowChatWindows ? 'appBody chatSplitLayout' : 'appBody'}>
                <RouteBody />

                {canShowChatWindows && (
                    <div className="chatMainPane">
                        {chatWindows.length === 0 && (
                            <div className="chatMainPaneEmpty">
                                <img className="chatMainPaneEmptyImg" src="/images/mococo_question.png" alt="채팅방 미선택" />
                                <span>왼쪽 목록에서 채팅방을 선택하세요.</span>
                            </div>
                        )}

                        {chatWindows.map((win) => (
                            <ChatBox
                                key={win.chatWindowKey}

                                chatWindowKey={win.chatWindowKey}
                                roomId={win.roomId}
                                isDraft={win.isDraft}
                                draftKey={win.draftKey}
                                targetPublicId={win.targetPublicId}
                                inviteMemberPublicIds={win.inviteMemberPublicIds}
                                roomType={win.roomType}
                                roomName={win.roomName}
                                roomThumbnail={win.roomThumbnail}
                                customRoomBackground={win.customRoomBackground}
                                messageNotificationEnabled={win.messageNotificationEnabled}
                                roomNotice={win.roomNotice}
                                memberList={win.memberList}
                                initialMessages={win.initialMessages}

                                isDocked={true}
                                x={win.x}
                                y={win.y}
                                zIndex={win.zIndex}

                                exitChatRoom={() => dispatch(closeChatWindow(win.chatWindowKey))}
                                onMove={(x, y) => dispatch(moveChatWindow({ chatWindowKey: win.chatWindowKey, x, y }))}
                                onFocus={() => dispatch(focusChatWindow(win.chatWindowKey))}
                            />
                        ))}
                    </div>
                )}
            </div>

            <div className="globalToastBox">
                {toastList.map(toast => (
					<div
						className={`globalToastItem ${toast.toastType === 'chat' ? 'chatNotificationToast' : ''}`}
						key={toast.toastId}
						onClick={() => openToastRoom(toast)}
					>
						{toast.toastType === 'chat' && (
							<img src={toast.roomThumbnail || '/images/mococo_question.png'} alt="채팅방" />
						)}
						<div className="globalToastTextBox">
							{toast.toastType === 'chat' && <strong>{toast.roomName ?? '채팅방'}</strong>}
							<span>{toast.text}</span>
						</div>
						{toast.toastType === 'chat' && toast.messageNotificationEnabled && (
							<button
								className="globalToastBellButton"
								title="이 방 알림 끄기"
								onClick={(e) => turnOffToastRoomNotification(e, toast)}
							>
								🔔
							</button>
						)}
						<button
							className="globalToastCloseButton"
							aria-label="알림 닫기"
							onClick={(e) => {
								e.stopPropagation();
								closeToast(toast.toastId);
							}}
						>
							×
						</button>
                    </div>
                ))}
            </div>
        </>

    );
}

export default AppShell;
