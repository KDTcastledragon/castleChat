
import './AppShell.css';

import { useEffect, useState } from "react";
import { useSelector, useDispatch } from 'react-redux';
import { useQueryClient } from '@tanstack/react-query';
import { useLocation, useNavigate } from 'react-router-dom';

import Header from "../Home/Header"
import RouteBody from "../Home/RouteBody"

import { useMe } from "../../hooks/useAuthUser";
import { closeChatWindow, moveChatWindow, focusChatWindow, clearChatWindows } from '../../store/chatWindowsSlice';
import { connectWs, disconnectWs, registerGlobalWsHandler, registerWsCloseListener } from "../../webSocket/wsClient";
import { addAcceptedFriendToCache, addReceivedFriendRequestToCache, removeReceivedFriendRequestFromCache } from '../../hooks/useFriend';

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
    const navigator = useNavigate();
    const location = useLocation();

    const dispatch = useDispatch();
    const chatWindows = useSelector(state => state.chatWindows.windows);
    const canShowChatWindows = location.pathname === '/chatList';

    // ======== WebSocket 연결 + 유저 목록 ======= ※ useEffect쓰는 이유? "컴포넌트가 화면에 등장했을 때" 웹소켓 연결하려고. 처음 렌더링될 때만 딱! 한! 번! 실행되어야한다.
    useEffect(() => {
        if (isCheckingLogin) return;

        if (!me) {
            navigator('/login', { replace: true });
            return;
        }

        connectWs();
    }, [me, isCheckingLogin, navigator]);

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
        if (!me) {

            return;
        }


        return registerGlobalWsHandler((wsEvt) => {
            const payload = wsEvt.payload ?? {};
            let text = '';

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

            if (wsEvt.wsType === 'CHAT_MESSAGE_NOTIFICATION') {
                text = `${payload.senderNickname ?? '상대'}: ${payload.previewText ?? ''}`;
            }

            if (wsEvt.wsType === 'ADD_FRIEND_FAIL' || wsEvt.wsType === 'RESPOND_FRIEND_FAIL') {
                text = payload?.errorMessage ?? '친구 처리 실패';
            }

            if (!text) return;

            const toastId = crypto.randomUUID();
            setToastList(prev => [...prev, { toastId, text }]);

            setTimeout(() => {
                setToastList(prev => prev.filter(toast => toast.toastId !== toastId));
            }, 3000);
        });
    }, [me, queryClient]);

    return (
        <>
            <Header />

            <RouteBody />

            {canShowChatWindows && chatWindows.map((win) => (
                <ChatBox
                    key={win.chatWindowKey}

                    chatWindowKey={win.chatWindowKey}
                    roomId={win.roomId}
                    isDraft={win.isDraft}
                    draftKey={win.draftKey}
                    targetPublicId={win.targetPublicId}
                    roomType={win.roomType}
                    roomName={win.roomName}
                    roomThumbnail={win.roomThumbnail}
                    customRoomBackground={win.customRoomBackground}
                    messageNotificationEnabled={win.messageNotificationEnabled}
                    memberList={win.memberList}

                    x={win.x}
                    y={win.y}
                    zIndex={win.zIndex}

                    exitChatRoom={() => dispatch(closeChatWindow(win.chatWindowKey))}
                    onMove={(x, y) => dispatch(moveChatWindow({ chatWindowKey: win.chatWindowKey, x, y }))}
                    onFocus={() => dispatch(focusChatWindow(win.chatWindowKey))}
                />
            ))}

            <div className="globalToastBox">
                {toastList.map(toast => (
                    <div className="globalToastItem" key={toast.toastId}>
                        {toast.text}
                    </div>
                ))}
            </div>
        </>

    );
}

export default AppShell;
