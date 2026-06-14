
import { useEffect } from "react";
import { useSelector, useDispatch } from 'react-redux';

import Header from "../Home/Header"
import RouteBody from "../Home/RouteBody"

import { useMe } from "../../hooks/useAuthUser";
import { closeChatWindow, moveChatWindow, focusChatWindow, clearChatWindows } from '../../store/chatWindowsSlice';
import { connectWs, disconnectWs, registerWsCloseListener } from "../../webSocket/wsClient";

import ChatBox from '../Chattings/ChatBox';

// 1. me 조회
// 2. WebSocket 연결
// 6. chatWindows
// 7. openDirectRoom
// 8. openRoomFromList
// 9. ChatBox 렌더링

function AppShell() {
    const { data: me, isLoading: isCheckingLogin } = useMe();

    const dispatch = useDispatch();
    const chatWindows = useSelector(state => state.chatWindows.windows);

    // ======== WebSocket 연결 + 유저 목록 ======= ※ useEffect쓰는 이유? "컴포넌트가 화면에 등장했을 때" 웹소켓 연결하려고. 처음 렌더링될 때만 딱! 한! 번! 실행되어야한다.
    useEffect(() => {
        if (!me) return;

        connectWs();

        return () => {
            console.log(`AppShell UNMOUNT`);
            disconnectWs('AppShell_UNMOUNT');
        }
    }, [me])

    useEffect(() => {
        const unregister = registerWsCloseListener(() => {
            dispatch(clearChatWindows());
        });

        return unregister;
    }, [dispatch]);

    return (
        <>
            <Header />

            <RouteBody />

            {chatWindows.map((win) => (
                <ChatBox
                    key={win.roomId}

                    roomId={win.roomId}
                    roomType={win.roomType}
                    roomName={win.roomName}
                    memberList={win.memberList}

                    x={win.x}
                    y={win.y}
                    zIndex={win.zIndex}

                    exitChatRoom={() => dispatch(closeChatWindow(win.roomId))}
                    onMove={(x, y) => dispatch(moveChatWindow({ roomId: win.roomId, x, y }))}
                    onFocus={() => dispatch(focusChatWindow(win.roomId))}
                />
            ))}
        </>

    );
}

export default AppShell;