import './Home.css';

import axios from 'axios';
import { useEffect, useState, useRef } from 'react';
import { useNavigate, Navigate } from 'react-router-dom'; //  Navigate : “화면 렌더링 중 조건에 따라 다른 주소로 보내는 컴포넌트”야.
// import LogIn from '../LogIn/LogIn';
import ChatBox from '../Chattings/ChatBox';

function Home() {

    const [loginUser, setLoginUser] = useState(null);
    const [isCheckingLogin, setIsCheckingLogin] = useState(true); // “지금 서버에 로그인 상태 확인 중인가?” --> “아직 서버 판정 전이라 로그인폼을 보여주면 안 되는 시간”을 처리하는 장치.
    const [userList, setUserList] = useState([]);

    const [chatWindows, setChatWindows] = useState([]);

    const isWsConnectedRef = useRef(false);
    const wsRef = useRef(null);

    const roomHandlersRef = useRef({});

    const navigator = useNavigate();


    // ==== 채팅방 옮기기 기본 설정 ===============================================================================
    const closeChatWindow = (roomId) => {
        setChatWindows(prev =>
            prev.filter(win => Number(win.roomId) !== Number(roomId))
        );
    };

    const moveChatWindow = (roomId, x, y) => {
        setChatWindows(prev =>
            prev.map(win =>
                Number(win.roomId) === Number(roomId)
                    ? { ...win, x, y }
                    : win
            )
        );
    };

    const focusChatWindow = (roomId) => {
        setChatWindows(prev =>
            prev.map(win =>
                Number(win.roomId) === Number(roomId)
                    ? { ...win, zIndex: Date.now() }
                    : win
            )
        );
    };

    // ====== 로그인 감지 ======================================
    useEffect(() => {
        axios.get('/user/isMe')
            .then((res) => {
                setLoginUser(res.data);
            })
            .catch(() => {
                setLoginUser(null);
            })
            // 성공하든 실패하든 무조건 실행되는 마지막 처리
            .finally(() => {
                setIsCheckingLogin(false);
                // 왜 isCheckingLogin이 필요하냐? 없으면 처음 렌더링 때 문제가 생겨. 
                // 처음에는 무조건: loginUser = null 이니까, 화면이 바로 로그인 폼을 보여줄 수 있어. --> {loginUser ? 로그인화면 : 로그인폼}
                // 근데 사실 서버 확인 중일 뿐인데, 0.1초 동안 로그인폼이 깜빡 보일 수 있어.
                // e.g.) 처음 렌더링: loginUser null → 로그인폼 표시
                // e.g.) /user/me 성공 → loginUser 있음 → 로그인화면 표시
                // 그러면 사용자는 새로고침할 때 로그인 폼이 순간적으로 보이는 이상한 UX를 볼 수 있어.
                // 그래서 isCheckingLogin으로 중간 상태를 하나 더 만드는 거야.

            });
    }, []);

    // ======== WebSocket 연결 + 유저 목록 ======= ※ useEffect쓰는 이유? "컴포넌트가 화면에 등장했을 때" 웹소켓 연결하려고. 처음 렌더링될 때만 딱! 한! 번! 실행되어야한다.
    useEffect(() => {
        if (!loginUser) return;

        // 만약 new Ws를 바깥으로 뺀다면? --> React 생명주기랑 충돌해서 터짐. 컴포넌트 랜더링 될때마다 연결함.
        const webSocket = new WebSocket(`ws://localhost:8080/ws/chat`); // roomId=${roomId}&userId=${userID} 삭제. query string --> ENTER 이벤트송신으로 변경.
        wsRef.current = webSocket;
        // onopen = FUNCTION_NAME 식으로 function저장을 해도 되지만,,,? 어차피 onopen때 딱 한!번! 쓰고 말것이기 때문에 굳이 바깥으로 function으로 빼지 않는다.

        webSocket.onopen = async () => { // async라서 useEffect안쪽에 callback함수 못 넣는다. useEffect는 cleaup function을 return해야 할수도있다. 바깥으로 빼면, parameter전달필요 , stale closure 위험, 의존성 증가 등이 생김.
            // --> onopen 호출하면 연결된다 (x)  / 연결이 성공하면 onopen에 저장된 함수가 "자동으로 실행된다" (o). 현재는 익명함수
            // wsRef.current = webSocket; // 연결후에 집어넣을 경우, onopen전에 sendMsg할수도있어서 위험함. 그래서 new Ws하자마자 위에서 바로 ㄱㄱ.

            isWsConnectedRef.current = true; // 연결 상태 false --> true

            console.log(`webSocket연결 완료.`);

            wsRef.current.send(JSON.stringify({
                requestId: crypto.randomUUID(),
                wsType: "CONNECT_USER",
                payload: {
                    // publicId: publicId
                }
            }))

        }

        webSocket.onmessage = (evt) => {
            const wsEvt = JSON.parse(evt.data);
            console.log(`ws 수신`, wsEvt);

            switch (wsEvt.wsType) {
                case "CONNECT_USER_OK":
                    console.log(`접속 성공`);
                    break;


                case "ENTER_ROOM_OK":
                    console.log(`방 접속 성공`);
                    break;

                case "MSG_SENDED": {
                    const roomId = wsEvt.payload.roomId;
                    roomHandlersRef.current[roomId]?.(wsEvt);
                    break;
                }

                case "MSG_READ": {
                    const roomId = wsEvt.payload.roomId;
                    roomHandlersRef.current[roomId]?.(wsEvt);
                    break;
                }

                case "TYPING_START":
                case "TYPING_STOP": {
                    const roomId = wsEvt.payload.roomId;
                    roomHandlersRef.current[roomId]?.(wsEvt);
                    break;
                }

                default:
                    // alert(`알수없는 타입`);
                    break;
            }

        }

        axios
            .get(`/user/allUsers`)
            .then((res) => {
                // console.log(`모든유저`);
                setUserList(res.data);
                // console.log(res.data);
            }).catch((e) => {
                console.log(e.message);
            });

        return () => {
            webSocket.close();
        }
    }, [loginUser])



    // ==== 채팅방 ===================================================
    const openChattingRoom = async (targetUser) => {
        try {
            const res = await axios.post(`/chat/enterRoom`, {
                // senderId: publicId,
                targetPublicId: targetUser.publicId
            });

            const openedRoomId = res.data.roomId;


            wsRef.current.send(JSON.stringify({
                requestId: crypto.randomUUID(),
                wsType: "ENTER_ROOM",
                payload: {
                    roomId: openedRoomId,
                    // publicId: publicId
                }
            }));

            setChatWindows(prev => {
                const alreadyOpen = prev.some(
                    win => Number(win.roomId) === Number(openedRoomId)
                );

                if (alreadyOpen) {
                    return prev.map(win =>
                        Number(win.roomId) === Number(openedRoomId)
                            ? { ...win, zIndex: Date.now() }
                            : win
                    );
                }

                return [
                    ...prev,
                    {
                        roomId: openedRoomId,
                        targetUserID: targetUser.userId,
                        targetLoginID: targetUser.loginId,
                        x: 420 + prev.length * 30,
                        y: 120 + prev.length * 30,
                        zIndex: Date.now()
                    }
                ];
            });

            console.log(`${targetUser.loginId}한테 대화 요청!`);

        } catch (e) {
            console.log(`채팅방 열기 실패!`);
            console.log(e);
        }
    };

    function logout() {
        if (wsRef.current) {
            wsRef.current.close();
            wsRef.current = null;
            console.log(`로그아웃 및 ws 연결종료`);
        }

        isWsConnectedRef.current = false;
        sessionStorage.clear();
        window.location.reload();
    }

    if (isCheckingLogin) {
        return <div>로그인 확인 중...</div>;
    }

    if (!loginUser) {
        return <Navigate to="/login" replace />;
    }

    // ===< return >===========================================================================================================
    return (
        <div className='HomeContainer'>

            {/**============== 로그인 구역==================== */}
            <div className='loginSection'>
                {loginUser ?
                    <>
                        <div className='loginForm'> publicId 님 안녕하세요.</div>

                        <button onClick={() => logout()}>로그아웃</button>
                    </>
                    :
                    <>
                        <div className='loginForm'>
                            <div>오류</div>
                        </div>
                    </>
                }

                <br /><br />

                <div className='loginButton'><button onClick={() => navigator('/JoinPage')}>회원가입</button></div>

            </div>

            {/**========= 유저 목록 및 채팅 오픈 버튼=================== */}
            <div>
                {userList.length > 0 ? userList.map((d, i) => (
                    <span key={i}> {/**Fragment에 key를 줘야한다...why? 나중에 질문하자. */}
                        <button
                            onClick={() => openChattingRoom(d)}>
                            {d.public_id}-({d.nickname})
                        </button><span>&nbsp;&nbsp;&nbsp;</span>
                    </span>
                ))
                    :
                    <div>유저없음</div>
                }
            </div>

            {/**========= 채팅창 =================== */}
            {chatWindows.map((win) => (
                <ChatBox
                    key={win.roomId}
                    wsRef={wsRef}
                    isWsConnectedRef={isWsConnectedRef}

                    roomId={win.roomId}
                    targetUserID={win.targetUserID}
                    targetLoginID={win.targetLoginID}

                    registerRoomHandler={(roomId, handler) => {
                        roomHandlersRef.current[roomId] = handler;
                    }}
                    unregisterRoomHandler={(roomId) => {
                        delete roomHandlersRef.current[roomId];
                    }}

                    x={win.x}
                    y={win.y}
                    zIndex={win.zIndex}
                    exitChatRoom={() => closeChatWindow(win.roomId)}
                    onMove={(x, y) => moveChatWindow(win.roomId, x, y)}
                    onFocus={() => focusChatWindow(win.roomId)}
                />
            ))}

        </div >
    );
}

export default Home;

// ====채팅방 오픈 함수222레거시 ===================================================
// const openChattingRoom22 = async (targetUser) => {
//     try {
//         const res = await axios.post(`/chat/enterRoom`,
//             {
//                 senderId: userID,
//                 targetUserId: targetUser.userId
//             });

//         setRoomId(res.data.roomId);
//         setTargetLoginID(targetUser.loginId);

//         setIsChattingOpen(true);
//         console.log(`${targetUser.loginId}한테 대화 요청!`);
//     } catch (e) {
//         console.log(e);
//     }
// }

// const [roomId, setRoomId] = useState(null);
// const [targetUserID, setTargetUserID] = useState('');
// const [targetLoginID, setTargetLoginID] = useState('');
// const [friList, setFriList] = useState([]);
// const [isChattingOpen, setIsChattingOpen] = useState(false);

// const [chatRooms, setChatRooms] = useState([]);