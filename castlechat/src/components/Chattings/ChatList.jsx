import './ChatList.css';

import { useMe } from '../../hooks/useMe';
import { useGetMyAllRooms } from '../../hooks/useGetMyAllRooms';

function ChatList() {
    const { data: me, isLoading: isCheckingLogin } = useMe();
    const [chatWindows, setChatWindows] = useState([]);

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

    // ==== 채팅방 입장 ===================================================
    const enterDirectRoom = async (friInfo) => {
        try {
            const res = await axios.post(`/chat/enterDirectRoom`, {
                friendPublicId: friInfo.publicId
            });

            const createdDirectRoom = res.data;

            const openedRoomId = createdDirectRoom.roomId;

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
                wsType: "ENTER_ROOM", // ENTER_DIRECT/GROUP_ROOM 으로 개인/단톡을 나눌 필요는 없다. 어차피 '입장'이기 때문.
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
                        roomType: createdDirectRoom.roomType,
                        roomName: createdDirectRoom.roomName,
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

    const enterRoom = async () => {

    }
    // ====== 단톡방 만들기 ============================================================================
    const createGroupRoom = async (roomName, selectedFriends) => {
        if (selectedFriends.length === 0) {
            alert(`초대할 친구를 선택해주세요.`);
            return;
        }

        try {
            const selectedFriendPublicIdList = selectedFriends.map(f => f.publicId); // axios 즉시요청해서 객체 전부 보내지말고, 필터링 한번빼라.

            // const res = await axios.post(`/chat/createGroupRoom`, {
            await axios.post(`/chat/createGroupRoom`, {
                roomName: roomName,
                selectedFriendPublicIdList: selectedFriendPublicIdList
            });

            // const createdGroupRoom = res.data;

            // const ws = wsRef.current;

            // if (ws && ws.readyState === WebSocket.OPEN) {
            //     ws.send(JSON.stringify({
            //         requestId: crypto.randomUUID(),
            //         wsType: "ENTER_GROUP_ROOM",  // CREATE는 이미 위에서 했으니, roomSession에 enter만 하자.
            //         payload: {
            //             roomId: createdGroupRoom.roomId
            //             // roomName: createdGroupRoomName, // 이 둘은 필요할 거 같지만 필요 없당.
            //             // groupRoomMemberList : groupRoomMemberList // 이 둘은 필요할 거 같지만 필요 없당.
            //         }
            //     }));
            // }

            // setChatWindows(prev => [
            //     ...prev,
            //     {
            //         roomId: createdGroupRoom.roomId,
            //         roomType: createdGroupRoom.roomType,
            //         roomName: createdGroupRoom.roomName,
            //         memberList: createdGroupRoom.memberList,
            //         x: 420 + prev.length * 30,
            //         y: 120 + prev.length * 30,
            //         zIndex: Date.now()
            //     }
            // ]);

            await refetchMyAllRooms();

            setRoomName('');
            setSelectedFriendList([]);

            console.log(`단톡방 만들기 성공`);

        } catch (e) {
            console.log(`단톡실패`);
        }
    }// createGroupRoom

    const enterRoomFromList = (room) => {
        const ws = wsRef.current;

        if (!ws || ws.readyState !== WebSocket.OPEN) {
            console.log('WebSocket 방 입장 전송 실패');
            return;
        }

        ws.send(JSON.stringify({
            requestId: crypto.randomUUID(),
            wsType: "ENTER_ROOM",
            payload: {
                roomId: room.roomId
            }
        }));

        setChatWindows(prev => {
            const alreadyOpen = prev.some(
                win => Number(win.roomId) === Number(room.roomId)
            );

            if (alreadyOpen) {
                return prev.map(win =>
                    Number(win.roomId) === Number(room.roomId)
                        ? { ...win, zIndex: Date.now() }
                        : win
                );
            }

            return [
                ...prev,
                {
                    roomId: room.roomId,
                    roomType: room.roomType,
                    roomName: room.displayRoomName || room.roomName,
                    x: 420 + prev.length * 30,
                    y: 120 + prev.length * 30,
                    zIndex: Date.now()
                }
            ];
        });
    };
    // ======< return >=======================================================================================================
    return (
        <div className='ChatListContainer'>
            {/**========= 채팅창 =================== */}
            {chatWindows.map((win) => (
                <ChatBox
                    key={win.roomId}

                    me={me}

                    wsRef={wsRef}
                    isWsConnectedRef={isWsConnectedRef}

                    roomId={win.roomId}
                    roomType={win.roomType}
                    roomName={win.roomName}
                    friend={win.friend}
                    memberList={win.memberList}

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
        </div>
    );
}

export default ChatList;