import './Home.css';

import axios from 'axios';
import { useEffect, useState, useRef } from 'react';
import { useNavigate, Navigate } from 'react-router-dom'; //  Navigate : “화면 렌더링 중 조건에 따라 다른 주소로 보내는 컴포넌트”야.

import { useMe } from '../../hooks/useMe';
import { useUsers } from '../../hooks/useUsers';
import { useLogout } from '../../hooks/useLogout';
import { useDebounce } from '../../hooks/useDebounce';
import { useSearchUsers } from '../../hooks/useSearchUsers';
import { useAddFriend } from '../../hooks/useAddFriend';
import { useFriendList } from '../../hooks/useFriendList';
import { useReceivedFriendRequests } from '../../hooks/useReceivedFriendRequests';
import { useRespondFriendRequest } from '../../hooks/useRespondFriendRequest';

import ChatBox from '../Chattings/ChatBox';

function Home() {
    // const [loginUser, setLoginUser] = useState(null);
    // const [isCheckingLogin, setIsCheckingLogin] = useState(true); // “지금 서버에 로그인 상태 확인 중인가?” --> “아직 서버 판정 전이라 로그인폼을 보여주면 안 되는 시간”을 처리하는 장치.
    // const [userList, setUserList] = useState([]);

    const [chatWindows, setChatWindows] = useState([]);
    const { data: me, isLoading: isCheckingLogin } = useMe();
    const { data: userList = [] } = useUsers(!!me);
    const { data: friendList = [] } = useFriendList(!!me);
    const { data: receivedFriendRequests = [] } = useReceivedFriendRequests(!!me);
    const logoutMutation = useLogout();
    const addFriendMutation = useAddFriend();
    const respondFriendRequestMutation = useRespondFriendRequest();

    const [roomName, setRoomName] = useState('');
    const [selectedFriendList, setSelectedFriendList] = useState([]);
    // const [isChecked, setIsChecked] = useState(false);

    const [searchWord, setSearchWord] = useState('');

    const isWsConnectedRef = useRef(false);
    const wsRef = useRef(null);

    const roomHandlersRef = useRef({});

    const navigator = useNavigate();

    const debouncedSearchWord = useDebounce(searchWord, 500);
    const {
        data: searchUsersResults = [],
        isLoading: isSearching
    } = useSearchUsers(debouncedSearchWord);

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
        if (!me) return;

        // 만약 new Ws를 바깥으로 뺀다면? --> React 생명주기랑 충돌해서 터짐. 컴포넌트 랜더링 될때마다 연결함.
        const webSocket = new WebSocket(`ws://localhost:8080/ws/chat`); // roomId=${roomId}&userId=${userID} 삭제. query string --> ENTER 이벤트송신으로 변경.
        wsRef.current = webSocket;
        // onopen = FUNCTION_NAME 식으로 function저장을 해도 되지만,,,? 어차피 onopen때 딱 한!번! 쓰고 말것이기 때문에 굳이 바깥으로 function으로 빼지 않는다.

        webSocket.onopen = async () => { // async라서 useEffect안쪽에 callback함수 못 넣는다. useEffect는 cleaup function을 return해야 할수도있다. 바깥으로 빼면, parameter전달필요 , stale closure 위험, 의존성 증가 등이 생김.
            // --> onopen 호출하면 연결된다 (x)  / 연결이 성공하면 onopen에 저장된 함수가 "자동으로 실행된다" (o). 현재는 익명함수
            // wsRef.current = webSocket; // 연결후에 집어넣을 경우, onopen전에 sendMsg할수도있어서 위험함. 그래서 new Ws하자마자 위에서 바로 ㄱㄱ.

            isWsConnectedRef.current = true; // 연결 상태 false --> true

            console.log(`webSocket연결 완료.`);

            // wsRef.current.send(JSON.stringify({
            webSocket.send(JSON.stringify({
                requestId: crypto.randomUUID(),
                wsType: "CONNECT_USER",
                payload: {
                    // session정보를 신뢰하고, f-e에서는 useMe로 로그인정보 관리하므로, 굳이 보낼 필요 없다. 
                }
            }))

        }

        webSocket.onmessage = (evt) => {
            const wsEvt = JSON.parse(evt.data);
            console.log(`ws 수신`, wsEvt);

            switch (wsEvt.wsType) {
                case "CONNECT_USER_OK":
                    console.log(`CONNECT_USER_OK`);
                    break;


                case "ENTER_ROOM_OK":
                    console.log(`ENTER_ROOM_OK`);
                    break;

                case "MSG_CREATED": {
                    const roomId = wsEvt.payload.roomId;
                    roomHandlersRef.current[roomId]?.(wsEvt);
                    console.log(`MSG_CREATED`);
                    break;
                }

                case "MSG_READ": {
                    const roomId = wsEvt.payload.roomId;
                    roomHandlersRef.current[roomId]?.(wsEvt);
                    console.log(`MSG_READ`);
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

        webSocket.onclose = (evt) => {
            console.log('WebSocket 종료', {
                code: evt.code,
                reason: evt.reason,
                wasClean: evt.wasClean
            });

            isWsConnectedRef.current = false;

            if (wsRef.current === webSocket) {
                wsRef.current = null;
            }
        };

        webSocket.onerror = (evt) => {
            console.error('WebSocket 오류', evt);
        };

        return () => {
            webSocket.close();
        }
    }, [me])

    // ===== 로그아웃 ===========================================================================================
    function logout() {
        if (wsRef.current) {
            wsRef.current.close();
            wsRef.current = null;
            console.log(`로그아웃 및 ws 연결종료`);
        }

        isWsConnectedRef.current = false;

        logoutMutation.mutate(null, {
            onSuccess: () => {
                navigator('/login');
            }
        });
    }

    if (isCheckingLogin) {
        return <div>로그인 확인 중...</div>;
    }

    if (!me) {
        return <Navigate to="/login" replace />;
    }

    //useState, useRef, useEffect, useQuery 같은 Hook들은 return보다 항상 위에서, 매 렌더링마다 같은 순서로 호출되어야 함. return이 맨 위로 가면 Hook after an early return? 오류뜸.


    // ==== 친구 추가 ==================================================
    function addFriend(targetPublicId) {
        addFriendMutation.mutate(targetPublicId, {
            onSuccess: () => {
                alert('친구 요청을 보냈습니다!');
            },
            onError: (e) => {
                if (e.response?.status === 409) {
                    alert('이미 친구이거나 친구 요청 중입니다.');
                    return;
                }

                if (e.response?.status === 401) {
                    alert('로그인이 필요합니다.');
                    navigator('/login');
                    return;
                }

                alert('친구 요청 실패');
                console.log(e);
            }
        });
    }

    // ==== 친구 수락/거절 ===================================================
    function respondFriendRequest(publicId, action) {
        respondFriendRequestMutation.mutate(
            { publicId, action },
            {
                onSuccess: () => {
                    if (action === 'ACCEPT') {
                        alert('친구 요청을 수락 했습니다. accept');
                    }

                    if (action === 'REJECT') {
                        alert('친구 요청을 거절 했습니다. reject');
                    }
                },
                onError: (e) => {
                    alert('친구 요청 처리 실패');
                    console.log(e);
                }
            }
        );
    }

    // ===== 친구 목록 체크 박스 설정=================================================================
    const isFriendSelected = (publicId) => {
        return selectedFriendList.some(friend => friend.publicId === publicId);
    };
    const toggleFriendSelect = (friend) => {
        setSelectedFriendList(prev => {
            const alreadySelected = prev.some(
                selectedFriend => selectedFriend.publicId === friend.publicId
            );

            if (alreadySelected) {
                return prev.filter(
                    selectedFriend => selectedFriend.publicId !== friend.publicId
                );
            }

            return [...prev, friend];
        });
    };

    // const isAllSelected_legacy =
    //     friendList.length > 0 &&
    //     selectedFriendList.length === friendList.length;
    const isAllSelected =
        friendList.length > 0 &&
        friendList.every(friend =>
            selectedFriendList.some(
                selectedFriend => selectedFriend.publicId === friend.publicId
            )
        );
    const toggleSelectAllFriends = () => {
        setSelectedFriendList(prev => {
            const isAllSelectedNow =
                friendList.length > 0 &&
                prev.length === friendList.length;

            if (isAllSelectedNow) {
                return [];
            }

            return friendList;
        });
    };

    // ==== 채팅방 ===================================================
    const enterDirectRoom = async (friInfo) => {
        try {
            const res = await axios.post(`/chat/enterDirectRoom`, {
                friendPublicId: friInfo.publicId
            });

            const openedRoomId = res.data.roomId;

            const ws = wsRef.current;

            if (!ws || ws.readyState !== WebSocket.OPEN) {
                console.log('WebSocket 방 입장 전송 실패', {
                    ws,
                    readyState: ws?.readyState
                });
                return;
            }

            ws.send(JSON.stringify({
                requestId: crypto.randomUUID(),
                wsType: "ENTER_DIRECT_ROOM",
                payload: {
                    roomId: openedRoomId,
                }
            }));

            // setChatWindows는 현재 열린 채팅창 목록을 변경하는 함수야.
            // prev는 변경 직전의 채팅창 배열이야.
            // e.g.)     prev = [
            //                      { roomId: 4, friend: { nickname: '공성전차' } },
            //                      { roomId: 7, friend: { nickname: '마법사' } }
            //                  ];
            setChatWindows(prev => {
                // 함수형 업데이트를 사용하는 이유는 채팅창을 연속으로 열거나 닫을 때 가장 최신 state를 기준으로 계산하기 위해서야.
                // some()은 배열 안에 조건을 만족하는 요소가 하나라도 있는지 확인해서 true 또는 false를 반환해.
                // prev 안에 openedRoomId와 같은 roomId를 가진 채팅창이 있는가?
                const alreadyOpen = prev.some(
                    win => Number(win.roomId) === Number(openedRoomId)
                );

                // ...win은 기존 채팅창 객체의 모든 정보를 복사한다는 뜻이야. 기존 정보는 유지하고 zIndex만 새 값으로 덮어써.
                // 결과적으로 이미 열린 채팅창을 새로 만들지 않고 화면 맨 앞으로 가져오는 거야.
                if (alreadyOpen) {
                    return prev.map(win =>
                        Number(win.roomId) === Number(openedRoomId)
                            ? { ...win, zIndex: Date.now() }
                            : win
                    );
                }
                // [...prev, 새객체]는 기존 배열을 복사하고 끝에 새 채팅창 객체를 추가한다는 뜻이야.
                return [
                    ...prev,
                    {
                        roomId: openedRoomId,
                        // 여기서 fri정보를 첨가.
                        // friPublicId: friInfo.publicId,
                        // friNickname: friInfo.nickname,
                        // friProfileImg: friInfo.profileImg,
                        // friCode: friInfo.friendCode,
                        friend: friInfo,
                        x: 420 + prev.length * 30,
                        y: 120 + prev.length * 30,
                        zIndex: Date.now() // : 현재 시간을 큰 숫자로 사용해서, 가장 최근에 열린 창이 가장 위에 보이도록 하는 방식이야.
                    }
                ];
            });

            // alert(`${openedRoomId}입장!`);

        } catch (e) {
            console.log(`채팅방 열기 실패!`);
            console.log(e);
        }
    };

    const inviteGroupRoom = async (roomName, selectedFriends) => {
        if (selectedFriends.length === 0) {
            alert(`초대할 친구를 선택해주세요.`);
            return;
        }

        try {
            const selectedFriPubIdList = selectedFriends.map(f => f.publicId); // axios 즉시요청해서 객체 전부 보내지말고, 필터링 한번빼라.

            const res = await axios.post(`/chat/enterGroupRoom`, {
                roomName: roomName,
                selectedFriPubIdList: selectedFriPubIdList
            });

            const openedGroupRoomId = res.data.roomId;

            const ws = wsRef.current;

            if (ws && ws.readyState === WebSocket.OPEN) {
                ws.send(JSON.stringify({
                    requestId: crypto.randomUUID(),
                    wsType: "ENTER_GROUP_ROOM",
                    payload: {
                        roomName: roomName,
                        roomId: openedGroupRoomId
                    }
                }));
            }

            console.log(`단톡방`);


        } catch (e) {
            console.log(`단톡실패`);
        }
    }



    // ===< return >===========================================================================================================
    return (
        <div className='HomeContainer'>

            {/**============== 로그인 구역==================== */}
            <div className='loginSection'>
                {me ?
                    <>
                        <div>{me.profileImg ? me.profileImg : '프사 없음'}</div>
                        <div className='loginForm'> {me.nickname} 님 안녕하세요.</div>
                        <div>{me.publicId}</div>
                        <div>{me.friendCode}</div>

                        <button onClick={() => logout()}>로그아웃</button>

                        {/* <div>{selectedFriendList}</div> */} {/** Object그 자체는 React가 rendering 할 수 없다. */}
                        <div>단톡 초대 체크된 친구들</div>
                        <br />
                        <input
                            type="checkbox"
                            checked={isAllSelected}
                            onChange={toggleSelectAllFriends}
                        />
                        <span>모두선택</span>
                        &nbsp;&nbsp;&nbsp;
                        <input type="text" value={roomName} onChange={(e) => setRoomName(e.target.value)} placeholder='단톡방 이름 입력...' />
                        <button
                            onClick={() => inviteGroupRoom(roomName, selectedFriendList)}
                        >
                            단톡초대하기
                        </button>
                        <div></div>
                        <br />
                        <div>
                            {/* <pre>{JSON.stringify(selectedFriendList, null, 2)}</pre> */} {/**객체 정보 다 보기 */}
                            {selectedFriendList.map(friend => friend.nickname).join(', ')} {/**닉만 보기. */}
                        </div>
                    </>
                    :
                    <>
                        <div className='loginForm'>
                            <div>오류</div>
                        </div>
                    </>
                }

                <br /><br />

            </div>

            {/**========= 친구 목록=================== */}
            <div className='friendsListSection'>
                <div>친구목록</div>

                {friendList.length > 0 ? friendList.map((friend) => (
                    <div key={friend.publicId}>
                        <input
                            type="checkbox"
                            checked={selectedFriendList.some(
                                selectedFriend => selectedFriend.publicId === friend.publicId
                            )}
                            onChange={() => toggleFriendSelect(friend)}
                        />
                        <span>{friend.nickname}</span>
                        <span>{friend.friendCode}</span>
                        &nbsp;&nbsp;
                        <button onClick={() => enterDirectRoom(friend)}>
                            채팅
                        </button>
                    </div>
                )) : (
                    <div>친구 없음</div>
                )}
            </div>
            <div className='friendsAlertList'>
                <div>친구 요청 목록</div>

                {receivedFriendRequests.length > 0 ? receivedFriendRequests.map((requestUser) => (
                    <div key={requestUser.publicId}>
                        <span>{requestUser.nickname}</span>
                        <span>{requestUser.friendCode}</span>
                        &nbsp;&nbsp;
                        <button
                            onClick={() => respondFriendRequest(requestUser.publicId, 'ACCEPT')}
                            disabled={respondFriendRequestMutation.isPending}
                        >
                            수락
                        </button>

                        <button
                            onClick={() => respondFriendRequest(requestUser.publicId, 'REJECT')}
                            disabled={respondFriendRequestMutation.isPending}
                        >
                            거절
                        </button>
                    </div>
                )) : (
                    <div>받은 친구 요청 없음</div>
                )}
            </div>

            {/**========= 유저 검색 및 친구추가 =================== */}
            <div className='searchOthersSection'>
                <div className='searchFriend'>
                    <span>검색 : </span>
                    <input
                        type="text"
                        value={searchWord}
                        onChange={(e) => setSearchWord(e.target.value)}
                        placeholder='닉네임/친구코드 검색'
                    />
                </div>
                <div>
                    {isSearching && <div>검색 중...</div>}

                    {!isSearching && debouncedSearchWord.trim().length > 0 && searchUsersResults.length === 0 && (
                        <div>검색 결과 없음</div>
                    )}

                    {searchUsersResults.map((user) => (
                        <div key={user.publicId}>
                            <span>{user.nickname}</span>
                            <span>{user.friendCode}</span> {/** <-- 임시로 개발중에만 띄움. 추후 삭제 예정. */}
                            &nbsp;&nbsp;
                            <button
                                onClick={() => addFriend(user.publicId)}
                                disabled={addFriendMutation.isPending}>
                                추가
                            </button>

                        </div>
                    ))}
                </div>
            </div>


            {/**========= 채팅창 =================== */}
            {chatWindows.map((win) => (
                <ChatBox
                    key={win.roomId}

                    me={me}

                    wsRef={wsRef}
                    isWsConnectedRef={isWsConnectedRef}

                    roomId={win.roomId}
                    friend={win.friend}

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

// // ====== 로그인 감지 legacy ======================================
// useEffect(() => {
//     axios.get('/user/isMe')
//         .then((res) => {
//             setLoginUser(res.data);
//         })
//         .catch(() => {
//             setLoginUser(null);
//         })
//         // 성공하든 실패하든 무조건 실행되는 마지막 처리
//         .finally(() => {
//             setIsCheckingLogin(false);
//             // 왜 isCheckingLogin이 필요하냐? 없으면 처음 렌더링 때 문제가 생겨. 
//             // 처음에는 무조건: loginUser = null 이니까, 화면이 바로 로그인 폼을 보여줄 수 있어. --> {loginUser ? 로그인화면 : 로그인폼}
//             // 근데 사실 서버 확인 중일 뿐인데, 0.1초 동안 로그인폼이 깜빡 보일 수 있어.
//             // e.g.) 처음 렌더링: loginUser null → 로그인폼 표시
//             // e.g.) /user/me 성공 → loginUser 있음 → 로그인화면 표시
//             // 그러면 사용자는 새로고침할 때 로그인 폼이 순간적으로 보이는 이상한 UX를 볼 수 있어.
//             // 그래서 isCheckingLogin으로 중간 상태를 하나 더 만드는 거야.

//         });
// }, []);

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

{/**Fragment에 key를 줘야한다...why? 나중에 질문하자. */ }
{/** <div>
                {userList.length > 0 ? userList.map((d, i) => (
                    <span key={i}> 
                        <button
                            onClick={() => openChattingRoom(d)}>
                            {d.public_id}-({d.nickname})
                        </button><span>&nbsp;&nbsp;&nbsp;</span>
                    </span>
                ))
                    :
                    <div>유저없음</div>
                }
            </div> */}