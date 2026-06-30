import './ChatBox.css';

import axios from 'axios';
import { useEffect, useState, useRef } from 'react';
import { emitWsExitRoom, emitWsLeftRoom, emitWsReadMessage, emitWsSendMessage, emitWsTypingStart, emitWsTypingStop, registerRoomHandler, unregisterRoomHandler } from '../../webSocket/wsClient';
import { useMe } from '../../hooks/useAuthUser';
import { useQueryClient } from '@tanstack/react-query';
import { leftRoomApi } from '../../api/chatApi';

function ChatBox({ roomId, roomType, roomName, memberList, x, y, zIndex, exitChatRoom, onMove, onFocus }) {

    const [chatMessage, setChatMessage] = useState('');
    const [prevChattings, setPrevChattings] = useState([]);
    const [typingUsers, setTypingUsers] = useState([]);
    const queryClient = useQueryClient();
    const { data: me } = useMe(); // { date : me} 아님. 오타 주의

    const { publicId: myPublicId, nickname: myNickname, friendCode: myFriendCode, profileImg: myProfileImg } = me || {}; // me가 null일경우, undefined상태로 만듦.

    const chatEndRef = useRef(null);

    const isTypingRef = useRef(false);
    const typingTimerRef = useRef(null);

    const pendingReadMessageIdRef = useRef(null);
    const readDebounceTimerRef = useRef(null);

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
                // 1. '이미 전송처리된' 이전 메시지 조회
                const loadedMessagesInRoom = await axios.get(`/room/loadMessagesInRoom/${roomId}`); // 가독성과 추후 재사용 가능성 때문에 변수에 저장 사용.  초기 데이터 로딩은 HTTP가 더 적합

                // console.log(`getPrevMsg --> ${JSON.stringify(prevMsgInThisRoom.data, null, 2)}`); // null, 2 가 들여쓰기 해줌.
                console.log(`${JSON.stringify(loadedMessagesInRoom.data)}`);

                setPrevChattings(loadedMessagesInRoom.data); // await 못 쓰는 이유? --> setPrevChattings는 Promise를 반환하지 않기 때문. "React야 state 변경 예약해줘"라고 요청만 한다.

                // 2. 마지막 메시지의 id 기준으로 읽음 처리
                if (loadedMessagesInRoom.data.length) {

                    const lastOtherMsgInRoom = [...loadedMessagesInRoom.data].reverse().find(msg => msg.senderPublicId !== myPublicId);

                    if (lastOtherMsgInRoom !== undefined) {
                        scheduleReadMessage(lastOtherMsgInRoom.messageId);
                        // emitWsReadMessage(roomId, lastOtherMsgInRoom.messageId); 
                        // lastOtherMsgInRoom전체를 보내면, 너무 커지고 책임도 이상해짐. payload가 두꺼워져.
                        // 근데, 이거조차도 db query를 탄다. 대용량과 맞지 않아서. debounce로 변경 적용.

                    }
                }

            } catch (e) { // 추후 실패 처리 로직 설계를 위해. 
                console.error("메시지 조회 실패", e);

                if (e.response?.status === 401) { // error.response 자체가 없으면 또 터짐.
                    console.log(`401 error`);
                } else {
                    console.log(e);
                }
            } // try-catch
        }; // init-chatRoom

        initChatRoom();

        // RoomHandler function
        registerRoomHandler(roomId, (wsResponse) => {

            // 1. '본인/타인'이 전송한 메세지 '실시간' 화면에 띄우기
            if (wsResponse.wsType === "MSG_CREATED") {
                const newMsg = wsResponse.payload; // “방금 생성된 새 메시지”를 꺼내는 거야.
                setPrevChattings(prev => [...prev, newMsg]); // 실시간으로 받은 새 메시지를 화면 아래에 추가하는 코드야. 
                // newMsg를 prevChattings배열의 맨 끝에 새롭게 추가해주는거야. ...prev는 이전의 메세지들이지.

                if (newMsg.senderPublicId !== myPublicId) {
                    // 새 메시지를 보낸 사람이 내가 아니면, 나는 지금 이 방을 보고 있으니까 그 메시지를 읽은 것으로 서버에 알려라.
                    scheduleReadMessage(newMsg.messageId);
                    // emitWsReadMessage(roomId, newMsg.messageId); // 서버에게 READ_MSG 전송.
                    // debounce로 변경. legacy 처리.

                }
            }// if 1.

            // 2. 서버가 다시 보내주는 read 이벤트를 처리. 읽음 요청의 결과를 화면에 반영하는 곳. unreadCount 컨트롤
            if (wsResponse.wsType === "MSG_READ") {
                const wsPayload = wsResponse.payload;

                if (!wsPayload) { return; }

                const updatedMessages = wsPayload.updatedMessages || []; //  이번 읽음 처리로 unreadCount가 갱신되어야 하는 메시지 목록

                if (updatedMessages.length === 0) { return; }

                setPrevChattings(prev =>
                    prev.map(msg => {
                        const updated = updatedMessages.find(
                            item => Number(item.messageId) === Number(msg.messageId)
                        );

                        if (!updated) {
                            return msg;
                        }

                        return {
                            ...msg,
                            unreadCount: Math.min(Number(msg.unreadCount ?? updated.unreadCount), Number(updated.unreadCount))
                            // unreadCount: updated.unreadCount // 서버에서 2가 먼저 오고, 늦게 3이 도착하면 화면 숫자가 다시 증가할 수 있음. 읽음 숫자는 같은 메시지 기준으로 줄어들 수는 있어도 다시 늘어나면 이상함.
                        };
                    })
                );
            }// if.2

            // 3.
            if (wsResponse.wsType === "TYPING_START") {
                const typingInfo = wsResponse.payload;

                if (typingInfo.publicId === myPublicId) {
                    return;
                }

                setTypingUsers(prev => {
                    const alreadyExists = prev.some(
                        user => user.publicId === typingInfo.publicId
                    );

                    if (alreadyExists) {
                        return prev;
                    }

                    return [...prev, typingInfo];
                });
            }

            // 4.
            if (wsResponse.wsType === "TYPING_STOP") {
                const typingInfo = wsResponse.payload;

                setTypingUsers(prev =>
                    prev.filter(user => user.publicId !== typingInfo.publicId)
                );
            }//if4.

            // 5. 채팅방 나가기 ws 처리.
            if (wsResponse.wsType === "ROOM_NOTICE") {
                const notice = wsResponse.payload;

                setPrevChattings(prev => [
                    ...prev,
                    {
                        messageId: `notice-${crypto.randomUUID()}`,
                        roomId: notice.roomId,
                        messageText: notice.message,
                        messageType: 'SYSTEM',
                        createdAt: notice.createdAt
                    }
                ]);
            }
        });

        return () => {
            flushPendingReadMessage(); // debounce 적용.

            unregisterRoomHandler(roomId);
        };

    }, [roomId, myPublicId]); // 내부에서 roomId, myPublicId를 씀. 최소한 이렇게 가는 게 맞아. 이렇게 해야 roomId/myPublicId 준비된 뒤 정상 등록됨.

    // ================ 메세지 전송 (WebSocket) =========================================================== 
    function sendChatMessage() {
        if (isTypingRef.current) {
            isTypingRef.current = false;
            // sendWs("TYPING_STOP", { roomId: roomId });
            emitWsTypingStop(roomId);
        }

        if (typingTimerRef.current) {
            clearTimeout(typingTimerRef.current);
            typingTimerRef.current = null;
        }

        // const sent = sendWs("SEND_MSG", { roomId: roomId, messageText: chatMessage }); // WebSocket 전송 시도가 성공했는지”를 담는 값이야.
        const isEmitted = emitWsSendMessage(roomId, chatMessage); // WebSocket 전송 시도가 성공했는지”를 담는 값이야.

        if (!isEmitted) {
            console.log(`!isEmitted`);
            return;
        }

        setChatMessage('');
    }

    // ================ 채팅 메시지 핸들러 ===============================================
    const handleChatMessageChange = (e) => {
        const nextValue = e.target.value;

        setChatMessage(nextValue);

        const isNowTyping = nextValue.length > 0;

        if (isNowTyping && !isTypingRef.current) {
            isTypingRef.current = true;
            emitWsTypingStart(roomId);
        }

        if (!isNowTyping && isTypingRef.current) {
            isTypingRef.current = false;
            emitWsTypingStop(roomId);
        }

        if (typingTimerRef.current) {
            clearTimeout(typingTimerRef.current);
        }

        if (isNowTyping) {
            typingTimerRef.current = setTimeout(() => {
                if (isTypingRef.current) {
                    isTypingRef.current = false;
                    emitWsTypingStop(roomId);
                }
            }, 90000);
        }
    };
    // ====== 메시지 읽기 요청 Debounce 처리 ===============================================
    function scheduleReadMessage(messageId) {
        if (!messageId) return;

        pendingReadMessageIdRef.current = Math.max(
            Number(pendingReadMessageIdRef.current ?? 0),
            Number(messageId)
        );

        if (readDebounceTimerRef.current) {
            clearTimeout(readDebounceTimerRef.current);
        }

        readDebounceTimerRef.current = setTimeout(() => {
            const lastReadMessageId = pendingReadMessageIdRef.current;

            if (lastReadMessageId) {
                emitWsReadMessage(roomId, lastReadMessageId);
            }

            pendingReadMessageIdRef.current = null;
            readDebounceTimerRef.current = null;
        }, 300);
    }
    function flushPendingReadMessage() {
        const lastReadMessageId = pendingReadMessageIdRef.current;

        if (readDebounceTimerRef.current) {
            clearTimeout(readDebounceTimerRef.current);
            readDebounceTimerRef.current = null;
        }

        if (lastReadMessageId) {
            emitWsReadMessage(roomId, lastReadMessageId);
        }

        pendingReadMessageIdRef.current = null;
    }

    // ================ 채팅창 닫기 ===============================================
    const closeChatAndExitRoom = () => {
        flushPendingReadMessage();


        if (isTypingRef.current) {
            isTypingRef.current = false;
            emitWsTypingStop(roomId);
        }

        if (typingTimerRef.current) {
            clearTimeout(typingTimerRef.current);
            typingTimerRef.current = null;
        }

        emitWsExitRoom(roomId);
        exitChatRoom(); // chatBox창 unmount.
    };

    async function leftRoom() {
        try {
            flushPendingReadMessage();

            await leftRoomApi(roomId);

            emitWsLeftRoom(roomId);
            // emitWsExitRoom(roomId);
            exitChatRoom();

            queryClient.invalidateQueries({ queryKey: ['myAllRooms'] }); // ChatList rerendering은 바로 이 줄에서 일어남.
        } catch (e) {
            console.error('방 나가기 실패', e);
            alert('방 나가기 실패');
        }
    }

    // ======< return >=======================================================================================================
    return (
        <div className='chatBoxContainer'>
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
                    <span>{roomName}</span>
                    {/* <span>{roomType}</span> */}
                    &nbsp;&nbsp;
                    <button
                        onMouseDown={(e) => e.stopPropagation()}
                        onClick={closeChatAndExitRoom}
                    >
                        닫기
                    </button>
                    <button
                        onMouseDown={(e) => e.stopPropagation()}
                        onClick={leftRoom}
                    >
                        방 나가기
                    </button>
                </div>

                <div className='chattingBox'>
                    {/* {prevChattings && prevChattings.length > 0 ?
                        prevChattings.map((d, i) => (

                            <div
                                key={d.messageId}
                                className={`chatRow ${d.senderPublicId === myPublicId ? 'mine' : 'other'}`}
                            >
                                {d.senderPublicId === myPublicId && (
                                    <div className='messageInfo'>
                                        <div className='unreadCount'>{d.unreadCount}</div>
                                        <div className='formatTime'>{formatTime(d.createdAt)}</div>
                                    </div>
                                )}

                                <div className='messageWrap'>
                                    <div className="messageText">{d.messageText}</div>
                                </div>

                                {d.senderPublicId !== myPublicId && (
                                    <div className='messageInfo'>
                                        <div className='unreadCount'>{d.unreadCount}</div>
                                        <div className='formatTime'>{formatTime(d.createdAt)}</div>
                                    </div>
                                )}
                            </div>
                        ))
                        :
                        <div>친구와 새로운 이야기를 시작해보세요.</div>
                    } */}

                    {prevChattings && prevChattings.length > 0 ?
                        prevChattings.map((d) => {
                            if (d.messageType === 'SYSTEM') {
                                return (
                                    <div key={d.messageId} className="systemMessage">
                                        {d.messageText}
                                    </div>
                                );
                            }

                            return (
                                <div
                                    key={d.messageId}
                                    className={`chatRow ${d.senderPublicId === myPublicId ? 'mine' : 'other'}`}
                                >
                                    {d.senderPublicId === myPublicId && (
                                        <div className='messageInfo'>
                                            <div className='unreadCount'>{d.unreadCount}</div>
                                            <div className='formatTime'>{formatTime(d.createdAt)}</div>
                                        </div>
                                    )}

                                    <div className='messageWrap'>
                                        <div className="messageText">{d.messageText}</div>
                                    </div>

                                    {d.senderPublicId !== myPublicId && (
                                        <div className='messageInfo'>
                                            <div className='unreadCount'>{d.unreadCount}</div>
                                            <div className='formatTime'>{formatTime(d.createdAt)}</div>
                                        </div>
                                    )}
                                </div>
                            );
                        })
                        :
                        <div>친구와 새로운 이야기를 시작해보세요.</div>
                    }

                    {/** 자동으로 스크롤다운 */}
                    <div ref={chatEndRef} />
                </div>
                {typingUsers.length > 0 && (
                    <div className="typingNotice">
                        {typingUsers.map(typingUser => typingUser.nickname).join(', ')}님이 입력 중...
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
                                sendChatMessage();
                            }
                        }} />
                    <button onClick={sendChatMessage}>전송</button> {/** () => sendChatMessage() 굳이 안 써도 된당 */}
                </div>
            </div>

        </div>);
}

export default ChatBox;


// // 2. 전송한 메세지 읽기 처리
// if (wsEvt.wsType === "MSG_READ") {
//     const readInfo = wsEvt.payload;

//     setPrevChattings(prev =>
//         prev.map(msg => {
//             // const isMyMsg = Number(msg.senderId) === Number(userID);
//             const isReadTarget = Number(msg.messageId) <= Number(readInfo.lastReadMessageId); // lastReadMessageId까지 읽었으니까, 그 이후 메시지들은 읽음 처리 대상.
//             const isReaderOwnMessage = msg.senderPublicId === readInfo.readerPublicId;

//             // if (isMyMsg && isReadTarget) {
//             if (isReadTarget && !isReaderOwnMessage) {
//                 return {
//                     ...msg,
//                     unreadCount: Math.max(Number(msg.unreadCount || 0) - 1, 0) // “READ 이벤트 하나 오면 무조건 1 줄인다”는 뜻이야.
//                     // 근데 같은 사람이 같은 메시지까지 여러 번 읽음 이벤트를 보내면 계속 줄어들어. 그래서 문제 생김.
//                 };
//             }

//             return msg;
//         })
//     );
// }// if 2.