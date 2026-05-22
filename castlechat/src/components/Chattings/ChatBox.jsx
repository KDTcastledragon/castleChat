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

    // ========== 이전 메시지 불러오기 ==============================================================================================
    useEffect(() => {

        if (!roomId) return;

        const loadMessages = async () => {

            try {

                // 1. 메시지 조회
                const res =
                    await axios.get(
                        `/chat/getMessages/${roomId}/${userID}`
                    );

                setPrevChattings(res.data);

                // 2. 마지막 메시지 읽음 처리
                if (res.data.length > 0) {

                    const lastMessage =
                        res.data[res.data.length - 1];

                    await axios.post(
                        "/chat/updateLastRead",
                        {
                            roomId,
                            userId: userID,
                            lastReadMessageId:
                                lastMessage.messageId
                        }
                    );

                    // 3. unreadCount 재조회
                    const updated =
                        await axios.get(
                            `/chat/getMessages/${roomId}/${userID}`
                        );

                    setPrevChattings(updated.data);
                }

            } catch (err) {
                console.log(err);
            }
        };

        loadMessages();

    }, [roomId, userID]);

    // ======== WebSocket 연결======= ※ useEffect쓰는 이유? "컴포넌트가 화면에 등장했을 때" 웹소켓 연결하려고. 처음 렌더링될 때만 딱! 한! 번! 실행되어야한다.
    useEffect(() => {
        // const ws = new WebSocket("ws://localhost:8080/ws/chat"); // 브라우저 ↔ Spring Boot 실시간 연결 생성. 전화에 비유하면, http:한마디하고 끊음. ws:연결계속유지
        // const ws = new WebSocket(`ws://localhost:8080/ws/chat?roomId=${roomId}`); //legacy

        const ws = new WebSocket(`ws://localhost:8080/ws/chat?roomId=${roomId}&userId=${userID}`); // new WS부터 연결 시작한다.

        console.log(`ws.readyState : ${ws.readyState}`);
        ws.onopen = () => {
            console.log("WebSocket 연결됐어용!"); // --> onopen 호출하면 연결된다 (x)  / 연결이 성공하면 onopen이 "자동으로 실행된다" (o) 
        };

        ws.onmessage = async (event) => {

            const newMessage = JSON.parse(event.data);

            if (Number(newMessage.roomId) !== Number(roomId)) {
                return;
            }

            // 상대 메시지인 경우
            if (Number(newMessage.senderId) !== Number(userID)) {

                // 1. 먼저 읽음 처리
                await axios.post("/chat/updateLastRead", {
                    roomId: roomId,
                    userId: userID,
                    lastReadMessageId: newMessage.messageId
                });

                // 2. unreadCount 반영된 최신 데이터 조회
                const updated =
                    await axios.get(
                        `/chat/getMessages/${roomId}/${userID}`
                    );

                // 3. 최신 상태로 교체
                setPrevChattings(updated.data);

            } else {

                // 내가 보낸 메시지는 그냥 추가
                setPrevChattings(prev => [...prev, newMessage]);
            }
        };


        console.log(`ws.readyState : ${ws.readyState}`);
        ws.onclose = () => {
            console.log("webSocket 종료.");
        };
        console.log(`ws.readyState : ${ws.readyState}`);
        setSocket(ws); // WebSocket 채우기.
        console.log(`ws.readyState : ${ws.readyState}`);
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
            type: 'SEND',
            senderId: userID,
            senderLoginId: loginID,
            roomId: roomId,
            msgText: chatMessage
        };

        socket.send(JSON.stringify(sendData));

        setChatMessage('');

        // ws.onclose(); reconnect
        // ws.onerror(); reconnect
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
                <button onClick={closeChat}>닫기</button>
            </div>
            <div className='chattingBox'>
                {prevChattings && prevChattings.length > 0 ?
                    prevChattings.map((d, i) => (
                        <div
                            key={i}
                            className={`chatRow ${Number(d.senderId) === Number(userID) ? 'mine' : 'other'}`}
                        >
                            <div className='messageWrap'>

                                <div>{d.msgText}</div>

                                <span className='unreadOne'>
                                    {d.unreadCount}

                                </span>
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

// ws연결 === 그러면 정확히는 컴퓨터 내에서 배포되어 있는(현재는 로컬테스팅중이지만) 내가 만든 springboot와 연결된다는거야? --> ㅇㅇ.
// 정확히는, WebSocket은 “브라우저(React)”가 로컬에서 실행 중인 Spring Boot 서버에 연결하는 것


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