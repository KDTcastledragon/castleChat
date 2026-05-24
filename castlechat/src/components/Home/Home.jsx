import './Home.css';

import axios from 'axios';
import { useEffect, useState, useRef } from 'react';
import LogIn from '../LogIn/LogIn';
import ChatBox from '../Chattings/ChatBox';

function Home() {
    const userID = sessionStorage.getItem('userID');
    const loginID = sessionStorage.getItem('loginID');
    const [userList, setUserList] = useState([]);

    const [chatWindows, setChatWindows] = useState([]);

    const isWsConnectedRef = useRef(false);
    const wsRef = useRef(null);

    const [enteredID, setEnteredID] = useState('');
    // const [roomId, setRoomId] = useState(null);
    // const [targetUserID, setTargetUserID] = useState('');
    // const [targetLoginID, setTargetLoginID] = useState('');
    // const [friList, setFriList] = useState([]);
    // const [isChattingOpen, setIsChattingOpen] = useState(false);

    // const [chatRooms, setChatRooms] = useState([]);


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

    // ======== WebSocket 연결 + 유저 목록 ======= ※ useEffect쓰는 이유? "컴포넌트가 화면에 등장했을 때" 웹소켓 연결하려고. 처음 렌더링될 때만 딱! 한! 번! 실행되어야한다.
    useEffect(() => {
        // 만약 new Ws를 바깥으로 뺀다면? --> React 생명주기랑 충돌해서 터짐. 컴포넌트 랜더링 될때마다 연결함.
        const webSocket = new WebSocket(`ws://localhost:8080/ws/chat`); // roomId=${roomId}&userId=${userID} 삭제. query string --> ENTER 이벤트송신으로 변경.
        wsRef.current = webSocket;
        // onopen = FUNCTION_NAME 식으로 function저장을 해도 되지만,,,? 어차피 onopen때 딱 한!번! 쓰고 말것이기 때문에 굳이 바깥으로 function으로 빼지 않는다.

        webSocket.onopen = async () => { // async라서 useEffect안쪽에 callback함수 못 넣는다. useEffect는 cleaup function을 return해야 할수도있다. 바깥으로 빼면, parameter전달필요 , stale closure 위험, 의존성 증가 등이 생김.
            // --> onopen 호출하면 연결된다 (x)  / 연결이 성공하면 onopen에 저장된 함수가 "자동으로 실행된다" (o). 현재는 익명함수
            // wsRef.current = webSocket; // 연결후에 집어넣을 경우, onopen전에 sendMsg할수도있어서 위험함. 그래서 new Ws하자마자 위에서 바로 ㄱㄱ.
            isWsConnectedRef.current = true; // 연결 상태 false --> true
            console.log(`webSocket연결 완료.`);
        }

        axios
            .get(`/user/allUsers`)
            .then((res) => {
                console.log(`모든유저`);
                setUserList(res.data);
                console.log(res.data);
            }).catch((e) => {
                console.log(e.message);
            });
    }, [])

    // =====[로그인/로그아웃 함수]======================================================
    function login(id1, pw1) {
        const data = { id: id1, pw: pw1 }

        axios
            .post(`/user/login`, data)
            .then((res) => {
                sessionStorage.setItem('userID', res.data.userId); // res안의 data안에 정보가 있다.
                sessionStorage.setItem('loginID', res.data.loginId);
                window.location.reload();

            }).catch((e) => {
                if (e.response.status) {
                    switch (e.response.status) {
                        case 401:
                            alert('아이디 없음');
                            break;

                        case 403:
                            alert('이용이 제한된 사용자입니다.');
                            break;

                        case 409:
                            alert(`비밀번호가 틀립니다.`);
                            break;

                        default:
                            alert(`로그인 오류`);
                            console.log(e);
                            break;
                    }
                } else {
                    alert(`알 수 없는 오류`);
                }
            });

    }

    function logout() {
        sessionStorage.clear();
        // webSocket.close();
        wsRef.current = null;
        isWsConnectedRef.current = false;
        // alert(`로그아웃 성공`);
        window.location.reload();
    }

    // ==== 채팅방 ===================================================
    const openChattingRoom = async (targetUser) => {
        try {
            const res = await axios.post(`/chat/enterRoom`, {
                senderId: userID,
                targetUserId: targetUser.userId
            });

            const openedRoomId = res.data.roomId;


            wsRef.current.send(JSON.stringify({
                type: "ENTER",
                roomId: openedRoomId,
                userId: userID
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

    // ===< return >===========================================================================================================
    return (
        <div className='HomeContainer'>

            {/**============== 로그인 구역==================== */}
            <div className='loginSection'>
                {loginID ?
                    <>
                        <div className='loginForm'> {loginID} 님 -- ({userID})</div>

                        <button onClick={() => logout()}>로그아웃</button>
                    </>
                    :
                    <>
                        <div className='loginForm'>
                            ID : <input
                                type="text"
                                value={enteredID}
                                onChange={(e) => setEnteredID(e.target.value)}
                            />
                        </div>
                        <button onClick={() => login(enteredID)}>로그인</button>
                    </>
                }

            </div>

            {/**========= 유저 목록 및 채팅 오픈 버튼=================== */}
            <div>
                {userList.length > 0 ? userList.map((d, i) => (
                    <span key={d.userId}> {/**Fragment에 key를 줘야한다...why? 나중에 질문하자. */}
                        <button
                            onClick={() => openChattingRoom(d)}>
                            {d.loginId}-({d.userId})
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

                    x={win.x}
                    y={win.y}
                    zIndex={win.zIndex}
                    onClose={() => closeChatWindow(win.roomId)}
                    onMove={(x, y) => moveChatWindow(win.roomId, x, y)}
                    onFocus={() => focusChatWindow(win.roomId)}
                />
            ))}

            {/**========= 채팅창 Legacy=================== */}
            {/* { 
                isChattingOpen &&
                <ChatBox
                    setIsChattingOpen={setIsChattingOpen}
                    targetUserID={targetUserID}
                    targetLoginID={targetLoginID}
                    roomId={roomId}
                />
            } */}

            {/* <div className='friendsListSection'>
                <div className='friendsListTitle'><span>친구 목록</span></div>
                <div className='friendsList'>
                    {friList && friList.length > 0 ?
                        friList.map((d, i) => (
                            <>
                                <div className='friend' onClick={() => openChatBoxWithFriend(d.user_id)}>
                                    <div>{i + 1}번째 친구</div>
                                    <div>ID : {d.user_id}</div>
                                    <div>이름 : {d.user_name}</div>
                                    <hr />
                                </div>
                            </>
                        ))
                        :
                        <>
                            <div>나는 개똥벌레,, 친구가 없네....</div>
                            <div>어서 친추 ㄱㄱ</div>
                        </>
                    }
                </div>
            </div> */}

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