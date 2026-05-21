import './ChatBox.css';

import axios from 'axios';
import { useEffect, useState, useRef } from 'react';

function ChatBox({ roomId, targetUserID, targetLoginID, setIsChattingOpen }) {
    const userID = sessionStorage.getItem('userID');
    const loginID = sessionStorage.getItem('loginID');

    const [chatMessage, setChatMessage] = useState('');
    const [prevChattings, setPrevChattings] = useState([]);

    const [socket, setSocket] = useState(null);
    const chatEndRef = useRef(null);

    const [lastReadMessageId, setLastReadMessageId] = useState(0);

    const formatTime = (isoString) => {
        const date = new Date(isoString);
        return date.toTimeString().split(" ")[0];
    };

    // ========== 이전 메시지 불러오기 ==================================
    useEffect(() => {
        if (!roomId) return;

        axios.get(`/chat/getMessages/${roomId}/${userID}`)
            .then((res) => {
                console.log("이전 메시지:", res.data);
                setPrevChattings(res.data);
            })
            .catch((err) => {
                console.log(err);
            });

    }, [roomId]);

    // ======== WebSocket 연결======= ※ useEffect쓰는 이유? "컴포넌트가 화면에 등장했을 때" 웹소켓 연결하려고. 처음 렌더링될 때만 딱! 한! 번! 실행되어야한다.
    useEffect(() => {
        // const ws = new WebSocket("ws://localhost:8080/ws/chat"); // 브라우저 ↔ Spring Boot 실시간 연결 생성. 전화에 비유하면, http:한마디하고 끊음. ws:연결계속유지
        // const ws = new WebSocket(`ws://localhost:8080/ws/chat?roomId=${roomId}`); //legacy

        const ws = new WebSocket(`ws://localhost:8080/ws/chat?roomId=${roomId}&userId=${userID}`);
        ws.onopen = () => {
            console.log("WebSocket 연결됐어용!");
        };

        ws.onmessage = (event) => {
            const newMessage = JSON.parse(event.data); // 서버에서 온 메세지 객체로 변환.

            if (Number(newMessage.roomId) === Number(roomId)) { // Number로 감싸줌으로써, Long타입으로 비교 가능.
                setPrevChattings(prev => [...prev, newMessage]);

                axios.post("/chat/updateLastRead", {
                    roomId: roomId,
                    userId: userID,
                    lastReadMessageId: newMessage.messageId
                });

            }
        };

        ws.onclose = () => {
            console.log("webSocket 종료.");
        };

        setSocket(ws); // WebSocket 채우기.

        return () => ws.close(); // ws연결종료 안 시키면, 과부하 온다. 필수 작성.

    }, [userID, targetUserID]);


    // ================ 메세지 전송 (WebSocket) =========================================================== 
    function sendMessage() {
        console.log(`현재 ${loginID} >> ${targetLoginID} 전송중...`);
        console.log(socket);

        if (!socket) {
            console.log("웹소켓 연결 안됨");
            return;
        }

        if (!chatMessage.trim()) {
            console.log(`trim이 안된다고?`);
            return;
        }

        const sendData = {
            senderId: userID,
            senderLoginId: loginID,
            roomId: roomId,
            msgText: chatMessage
        };

        socket.send(JSON.stringify(sendData));

        setChatMessage('');
    }

    // ================ 최신 메세지 기준으로 보기. 스크롤 항상 최하단 ===============================================
    useEffect(() => {
        chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [prevChattings]);


    // ================ 최신 메세지 기준으로 보기. 스크롤 항상 최하단 ===============================================
    const closeChat = () => {
        const last = prevChattings.at(-1);

        if (last) {
            axios.post("/chat/updateLastRead", {
                roomId,
                userId: userID,
                lastReadMessageId: last.messageId
            });
        }

        setIsChattingOpen(false);
    };

    // ==============================================================================================================================
    return (
        <div className='chattingRoomSection'>
            <div className='chatListTitle'><span>{targetLoginID} - ({targetUserID})</span>&nbsp;&nbsp;&nbsp;&nbsp;
                <button onClick={closeChat}>닫기</button></div>
            <div className='chattingBox'>
                {prevChattings && prevChattings.length > 0 ?
                    prevChattings.map((d, i) => (
                        <div
                            key={i}
                            className={`chatRow ${Number(d.senderId) === Number(userID) ? 'mine' : 'other'}`}
                        >
                            <div className='messageWrap'>

                                <div>{d.msgText}</div>

                                {
                                    Number(d.unreadCount) > 0
                                    &&
                                    <span className='unreadOne'>
                                        {d.unreadCount}

                                    </span>
                                }
                            </div>
                            <div className='formatTime'>{formatTime(d.createdAt)}</div>
                        </div>
                    ))
                    :
                    <div>친구와 새로운 이야기를 시작해보세요.</div>
                }

                {/* 👇 이게 핵심 */}
                <div ref={chatEndRef} />
            </div>
            <div className='inputChat'>
                <textarea
                    type="text"
                    value={chatMessage}
                    onChange={(e) => setChatMessage(e.target.value)}
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