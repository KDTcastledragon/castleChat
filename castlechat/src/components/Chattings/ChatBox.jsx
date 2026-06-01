import './ChatBox.css';

import axios from 'axios';
import { useEffect, useState, useRef } from 'react';

// home에서 me항목 하나씩 일일히 다 넘기게 되면 Home이 ChatBox 내부에서 뭘 쓰는지 너무 많이 관여하게 돼. 그래서 me를 통째로 받는게 좋다.
function ChatBox({ me, wsRef, isWsConnectedRef, roomId, friendPublicId, registerRoomHandler,
    unregisterRoomHandler, x, y, zIndex, exitChatRoom, onMove, onFocus }) {

    const [chatMessage, setChatMessage] = useState('');
    const [prevChattings, setPrevChattings] = useState([]);
    const [typingUsers, setTypingUsers] = useState([]);



    // const myPublicId = me.publicId;
    // const myNickname = me.nickname;
    // const myFriendCode = me.friendCode;
    // const myProfileImg = me.profileImg;

    // const { myPublicId, myNickname, myFriendCode, myProfileImg } = me; (이렇게 쓰면 망한다.)

    const { publicId: myPublicId, nickname: myNickname, friendCode: myFriendCode, profileImg: myProfileImg } = me || {}; // me가 null일경우, undefined상태로 만듦.

    const userID = myPublicId;

    const chatEndRef = useRef(null);

    const isTypingRef = useRef(false);
    const typingTimerRef = useRef(null);



    const formatTime = (isoString) => {
        const date = new Date(isoString);
        return date.toTimeString().split(" ")[0];
    };

    // ============== 상단바 드래그 & 드롭 1 ===================================
    const dragRef = useRef({
        isDragging: false,
        startMouseX: 0,
        startMouseY: 0,
        startX: 0,
        startY: 0
    });

    const startDrag = (e) => {
        onFocus();

        dragRef.current = {
            isDragging: true,
            startMouseX: e.clientX,
            startMouseY: e.clientY,
            startX: x,
            startY: y
        };
    };

    // ============== 상단바 드래그 & 드롭 2 ===================================
    useEffect(() => {
        const handleMouseMove = (e) => {
            if (!dragRef.current.isDragging) return;

            const movedX = e.clientX - dragRef.current.startMouseX;
            const movedY = e.clientY - dragRef.current.startMouseY;

            onMove(
                dragRef.current.startX + movedX,
                dragRef.current.startY + movedY
            );
        };

        const handleMouseUp = () => {
            dragRef.current.isDragging = false;
        };

        window.addEventListener('mousemove', handleMouseMove);
        window.addEventListener('mouseup', handleMouseUp);

        return () => {
            window.removeEventListener('mousemove', handleMouseMove);
            window.removeEventListener('mouseup', handleMouseUp);
        };
    }, [onMove]);

    // ================ 최신 메세지 기준으로 보기. 스크롤 항상 최하단 ===============================================
    useEffect(() => {
        chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [prevChattings]);



    // ========== 이전 메시지 불러오기 ==============================================================================================
    useEffect(() => {

        if (!roomId || !myPublicId) return; // roomId,userID 가 존재하지 않을 시  mount 차단. "채팅방 준비 완료 전 실행 방지"
        // 렌더링 타이밍상 아직 값이 없을 때 불필요한 API 호출 방지. 사용자 경험/콘솔 에러 정리.
        // 특히 React에서는 useEffect가 컴포넌트 mount 후 실행되기 때문에, 값이 아직 안정적이지 않은 순간이 생길 수 있습니다.
        // 지금 ChatBox는 roomId를 props로 받습니다. 대부분은 값이 들어온 뒤 mount되겠지만, 리팩토링 중이거나 조건부 렌더링이 살짝 바뀌면 undefined 상태로 들어올 수도 있습니다.
        // 그래서 이 guard는 남기는 게 좋아요.


        const initChatRoom = async () => {

            try {
                // 1. 메시지 조회
                const getedMsgInRoom = await axios.get(`/chat/getMessages/${roomId}`); // 가독성과 추후 재사용 가능성 때문에 변수에 저장 사용.  초기 데이터 로딩은 HTTP가 더 적합

                setPrevChattings(getedMsgInRoom.data); // await 못 쓰는 이유? --> setPrevChattings는 Promise를 반환하지 않기 때문. "React야 state 변경 예약해줘"라고 요청만 한다.

                // 2. 마지막 메시지 읽음 처리
                if (getedMsgInRoom.data.length > 0 && wsRef.current?.readyState === WebSocket.OPEN) {
                    // optional Chaining : wsRef.current가 null 또는 undefined면 에러 내지 말고 undefined를 반환해라.
                    // ws.readyState : 현재 ws 연결상태. 0:connecting , 1:open , 2:closed / WebSocket.OPEN : "연결 성공 상태를 의미하는 고정 상수"
                    // 굳이 '1'을 안쓰고 readyState === OPEN 쓰는 이유는?? --> 훨씬 의미가 명확하기 때문.

                    // const lastMsgInRoom = getedMsgInRoom.data[getedMsgInRoom.data.length - 1]; // 동적계산을 위해 length-1

                    const lastOtherMsgInRoom = [...getedMsgInRoom.data].reverse().find(msg => Number(msg.senderId) !== Number(userID));

                    if (lastOtherMsgInRoom !== undefined) {

                        wsRef.current.send(JSON.stringify({
                            requestId: crypto.randomUUID(),
                            wsType: "READ_MSG",
                            payload: {
                                roomId,
                                publicId: myPublicId,
                                lastReadMessageId: lastOtherMsgInRoom.messageId
                            }
                        }));
                    } // if-lastMsg
                }

            } catch (error) { // 추후 실패 처리 로직 설계를 위해. 
                console.error("메시지 조회 실패", error);

                if (error.response.status === 401) {
                    console.log(`401 error`);
                } else {
                    console.log(error);
                }
            } // try-catch
        }; // init-chatRoom

        initChatRoom();

        // RoomHandler function
        registerRoomHandler(roomId, (wsEvt) => {

            // 1. 전송한 메세지 화면에 띄우기
            if (wsEvt.wsType === "MSG_SENDED") {
                const newMsg = wsEvt.payload;
                setPrevChattings(prev => [...prev, newMsg]);

                if (Number(newMsg.senderId) !== Number(userID)) {
                    wsRef.current?.send(JSON.stringify({
                        requestId: crypto.randomUUID(),
                        wsType: "READ_MSG",
                        payload: {
                            roomId: roomId,
                            userId: Number(userID),
                            lastReadMessageId: newMsg.messageId
                        }
                    }));
                }
            }// if 1.

            // 2. 전송한 메세지 읽기 처리
            if (wsEvt.wsType === "MSG_READ") {
                const readInfo = wsEvt.payload;

                // if (Number(readInfo.userId) === Number(userID)) {
                //     return; // 내가 보낸 READ 이벤트는 내 화면에서 unreadCount를 줄이지 않게 막는 것.
                // } // 이 코드 때문에, a가 보낸 메시지를 d가 접속해서 확인해도 1 -> 0으로 바뀌질 않음.

                setPrevChattings(prev =>
                    prev.map(msg => {
                        // const isMyMsg = Number(msg.senderId) === Number(userID);
                        const isReadTarget = Number(msg.messageId) <= Number(readInfo.lastReadMessageId);
                        const isReaderOwnMessage = Number(msg.senderId) === Number(readInfo.userId);

                        // if (isMyMsg && isReadTarget) {
                        if (isReadTarget && !isReaderOwnMessage) {
                            return {
                                ...msg,
                                unreadCount: Math.max(Number(msg.unreadCount || 0) - 1, 0)
                            };
                        }

                        return msg;
                    })
                );
            }// if 2.

            // 3.
            if (wsEvt.wsType === "TYPING_START") {
                const typingInfo = wsEvt.payload;

                if (Number(typingInfo.userId) === Number(userID)) {
                    return;
                }

                setTypingUsers(prev => {
                    const alreadyExists = prev.some(
                        user => Number(user.userId) === Number(typingInfo.userId)
                    );

                    if (alreadyExists) {
                        return prev;
                    }

                    return [...prev, typingInfo];
                });
            }

            // 4.
            if (wsEvt.wsType === "TYPING_STOP") {
                const typingInfo = wsEvt.payload;

                setTypingUsers(prev =>
                    prev.filter(user => Number(user.userId) !== Number(typingInfo.userId))
                );
            }
        });

        return () => {
            unregisterRoomHandler(roomId);
        };

    }, []);

    // ================ 메세지 전송 (WebSocket) =========================================================== 
    function sendMessage() {
        console.log(`${myNickname} >> ${targetLoginID} Msg 전송`);
        console.log(`현재 wsRef : ${wsRef}`);


        if (!wsRef.current) {
            console.log("wsRef Null");
            return; // early Return
        }

        if (!isWsConnectedRef.current) {
            console.log("웹소켓 아직 연결중");
            return;
        } // ws객체 자체가 비었는지, 현재 연결중인지 구분하기 위해 if 분리.

        if (isTypingRef.current) {
            isTypingRef.current = false;
            sendTypingStop();
        }

        if (typingTimerRef.current) {
            clearTimeout(typingTimerRef.current);
            typingTimerRef.current = null;
        }

        wsRef.current.send(JSON.stringify({
            requestId: crypto.randomUUID(),
            wsType: "SEND_MSG",
            payload: {
                roomId: roomId,
                senderId: Number(userID),
                senderLoginId: loginID,
                msgText: chatMessage
            }

        }));

        setChatMessage('');
    }

    // ================ 채팅창 닫기 ===============================================
    const exitChat = () => {
        if (isTypingRef.current) {
            isTypingRef.current = false;
            sendTypingStop();
        }

        if (typingTimerRef.current) {
            clearTimeout(typingTimerRef.current);
            typingTimerRef.current = null;
        }

        wsRef.current.send(JSON.stringify({
            requestId: crypto.randomUUID(),
            wsType: "EXIT_ROOM",
            payload: {
                roomId: roomId,
                userId: Number(userID),
            }
        }));

        exitChatRoom(); // chatBox창 unmount.
    };

    // ================ 채팅 입력 start/stop ===============================================
    const sendTypingStart = () => {
        wsRef.current?.send(JSON.stringify({
            requestId: crypto.randomUUID(),
            wsType: "TYPING_START",
            payload: {
                roomId: roomId,
                userId: Number(userID),
                loginId: loginID
            }
        }));
    };

    const sendTypingStop = () => {
        wsRef.current?.send(JSON.stringify({
            requestId: crypto.randomUUID(),
            wsType: "TYPING_STOP",
            payload: {
                roomId: roomId,
                userId: Number(userID),
                loginId: loginID
            }
        }));
    };

    const handleChatMessageChange = (e) => {
        const nextValue = e.target.value;

        setChatMessage(nextValue);

        if (!wsRef.current) {
            return;
        }

        const isNowTyping = nextValue.length > 0;

        if (isNowTyping && !isTypingRef.current) {
            isTypingRef.current = true;
            sendTypingStart();
        }

        if (!isNowTyping && isTypingRef.current) {
            isTypingRef.current = false;
            sendTypingStop();
        }

        if (typingTimerRef.current) {
            clearTimeout(typingTimerRef.current);
        }

        if (isNowTyping) {
            typingTimerRef.current = setTimeout(() => {
                if (isTypingRef.current) {
                    isTypingRef.current = false;
                    sendTypingStop();
                }
            }, 90000);
        }
    };



    // ==============================================================================================================================
    return (
        <div
            className='chattingRoomSection'
            style={{
                left: x,
                top: y,
                zIndex
            }}
            onMouseDown={onFocus}
        >
            <div className='chatListTitle' onMouseDown={startDrag}>
                <span>{targetLoginID} - ({targetUserID})</span>
                <button
                    onMouseDown={(e) => e.stopPropagation()}
                    onClick={exitChat}
                >
                    닫기
                </button>
            </div>

            <div className='chattingBox'>
                {prevChattings && prevChattings.length > 0 ?
                    prevChattings.map((d, i) => (
                        <div
                            key={i}
                            className={`chatRow ${Number(d.senderId) === Number(userID) ? 'mine' : 'other'}`}
                        >
                            {Number(d.senderId) === Number(userID) && (
                                <div className='messageInfo'>
                                    <div className='unreadCount'>{d.unreadCount}</div>
                                    <div className='formatTime'>{formatTime(d.createdAt)}</div>
                                </div>
                            )}

                            <div className='messageWrap'>
                                <div className="messageText">{d.msgText}</div>
                            </div>

                            {Number(d.senderId) !== Number(userID) && (
                                <div className='messageInfo'>
                                    <div className='unreadCount'>{d.unreadCount}</div>
                                    <div className='formatTime'>{formatTime(d.createdAt)}</div>
                                </div>
                            )}
                        </div>
                    ))
                    :
                    <div>친구와 새로운 이야기를 시작해보세요.</div>
                }

                {/* 👇 이게 핵심 */}
                <div ref={chatEndRef} />
            </div>
            {typingUsers.length > 0 && (
                <div className="typingNotice">
                    {typingUsers.map(user => user.loginId).join(', ')}님이 입력 중...
                </div>
            )}

            <div className='inputChat'>
                <textarea
                    type="text"
                    value={chatMessage}
                    // onChange={(e) => setChatMessage(e.target.value)}
                    onChange={handleChatMessageChange}
                    placeholder='여기에 메세지 입력...'
                    onKeyDown={(e) => {
                        if (e.key === 'Enter' && !e.shiftKey) {
                            e.preventDefault(); // 줄바꿈 막기
                            sendMessage();
                        }
                    }} />
                <button onClick={sendMessage}>전송</button> {/** () => sendMessage() 굳이 안 써도 된당 */}
            </div>
        </div>
    );
}

export default ChatBox;

// ws연결 === 그러면 정확히는 컴퓨터 내에서 배포되어 있는(현재는 로컬테스팅중이지만) 내가 만든 springboot와 연결된다는거야? --> ㅇㅇ.
// 정확히는, WebSocket은 “브라우저(React)”가 로컬에서 실행 중인 Spring Boot 서버에 연결하는 것


// 1-1. 메시지 조회 ---> async/await 안의
// const getPrevMsg =
//     await axios
//         .get(`/chat/getMessages/${roomId}`)
//         .then((res) => {
//             setPrevChattings(res.data);
//             console.log(`메세지 조회 성공 : ${res.data}`);
//         }).catch((e) => {
//             console.log(`메세지 조회 실패`);
//         })

// ========== 이전 메시지 불러오기 ==============================================================================================
// useEffect(() => {
//     if (!roomId) return;

//     axios.get(`/chat/getMessages/${roomId}/${userID}`)
//         .then((res) => {
//             console.log("이전 메시지:", res.data);
//             setPrevChattings(res.data);

//             if (res.data.length > 0) {

//                 const lastMessage =
//                     res.data[res.data.length - 1];

//                 axios.post("/chat/updateLastRead", {
//                     roomId: roomId,
//                     userId: userID,
//                     lastReadMessageId: lastMessage.messageId
//                 });

//             }
//         })
//         .catch((err) => {
//             console.log(err);
//         });

// }, [roomId]);

// ==========================================================================================
// if (Number(newMessage.roomId) === Number(roomId)) {

//                 // 1. 화면에 메시지 추가
//                 setPrevChattings(prev => [...prev, newMessage]);

//                 // 2. "상대방 메시지"를 받은 경우만 읽음 처리
//                 if (Number(newMessage.senderId) !== Number(userID)) {

//                     await axios.post("/chat/updateLastRead", {
//                         roomId: roomId,
//                         userId: userID,
//                         lastReadMessageId: newMessage.messageId
//                     });

//                     // 3. unreadCount 다시 반영하려고 재조회
//                     const updated =
//                         await axios.get(
//                             `/chat/getMessages/${roomId}/${userID}`
//                         );

//                     setPrevChattings(updated.data);
//                 }
//             }

// ==============메시지 실시간 띄우기 함수 legacy===================================
// const handleMessage = (event) => {
//     const newMessage = JSON.parse(event.data);

//     if (Number(newMessage.roomId) !== Number(roomId)) {
//         return;
//     }

//     setPrevChattings(prev => [...prev, newMessage]);
// };

// ================ 채팅창 닫기 legacy ===============================================
// const closeChat = () => {
// const last = prevChattings.at(-1);

// if (last) {
//     axios.post("/chat/updateLastRead", {
//         roomId,
//         userId: userID,
//         lastReadMessageId: last.messageId
//     });
// }

// onClose();
// };