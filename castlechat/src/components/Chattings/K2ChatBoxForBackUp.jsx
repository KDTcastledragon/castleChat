import './ChatBox.css';

import axios from 'axios';
import { useEffect, useState, useRef } from 'react';

function K2ChatBoxForBackUp({ roomId, targetUserID, targetLoginID, setIsChattingOpen }) {
    const userID = sessionStorage.getItem('userID');
    const loginID = sessionStorage.getItem('loginID');

    const [chatMessage, setChatMessage] = useState('');
    const [prevChattings, setPrevChattings] = useState([]);

    // const [ws, setWs] = useState(null);
    const isWsConnectedRef = useRef(false);
    const chatEndRef = useRef(null);
    const wsRef = useRef(null);

    const [lastReadMessageId, setLastReadMessageId] = useState(0);

    const formatTime = (isoString) => {
        const date = new Date(isoString);
        return date.toTimeString().split(" ")[0];
    };
    // ==============메시지 실시간 띄우기 함수===================================
    const handleMessage = (event) => {
        const newMessage = JSON.parse(event.data);

        if (Number(newMessage.roomId) !== Number(roomId)) {
            return;
        }

        setPrevChattings(prev => [...prev, newMessage]);
    };

    // ========== 이전 메시지 불러오기 ==============================================================================================
    useEffect(() => {

        if (!roomId || !userID) return; // roomId,userID 가 존재하지 않을 시  mount 차단. "채팅방 준비 완료 전 실행 방지"

        // ======== WebSocket 연결======= ※ useEffect쓰는 이유? "컴포넌트가 화면에 등장했을 때" 웹소켓 연결하려고. 처음 렌더링될 때만 딱! 한! 번! 실행되어야한다.
        // 만약 new Ws를 바깥으로 뺀다면? --> React 생명주기랑 충돌해서 터짐. 컴포넌트 랜더링 될때마다 연결함.
        const webSocket = new WebSocket(`ws://localhost:8080/ws/chat?roomId=${roomId}&userId=${userID}`);
        wsRef.current = webSocket;
        // onopen = FUNCTION_NAME 식으로 function저장을 해도 되지만,,,? 어차피 onopen때 딱 한!번! 쓰고 말것이기 때문에 굳이 바깥으로 function으로 빼지 않는다.
        webSocket.onopen = async () => { // async라서 useEffect안쪽에 callback함수 못 넣는다. useEffect는 cleaup function을 return해야 할수도있다. 바깥으로 빼면, parameter전달필요 , stale closure 위험, 의존성 증가 등이 생김.
            isWsConnectedRef.current = true;
            console.log(`webSocket연결 완료.`);  // --> onopen 호출하면 연결된다 (x)  / 연결이 성공하면 onopen에 저장된 함수가 "자동으로 실행된다" (o). 현재는 익명함수
            // wsRef.current = webSocket; // 연결후에 집어넣을 경우, onopen전에 sendMsg할수도있어서 위험함. 그래서 new Ws하자마자 바로 ㄱㄱ.
            try {
                // 1. 메시지 조회
                const getedMsgInRoom = await axios.get(`/chat/getMessages/${roomId}`); // 가독성과 추후 재사용 가능성 때문에 변수에 저장 사용.  초기 데이터 로딩은 HTTP가 더 적합

                setPrevChattings(getedMsgInRoom.data); // await 못 쓰는 이유? --> setPrevChattings는 Promise를 반환하지 않기 때문. "React야 state 변경 예약해줘"라고 요청만 한다.

                // 2. 마지막 메시지 읽음 처리
                if (getedMsgInRoom.data.length > 0 && webSocket.readyState === WebSocket.OPEN) {
                    // ws.readyState : 현재 ws 연결상태. 0:connecting , 1:open , 2:closed / WebSocket.OPEN : "연결 성공 상태를 의미하는 고정 상수"
                    // 굳이 '1'을 안쓰고 readyState === OPEN 쓰는 이유는?? --> 훨씬 의미가 명확하기 때문.

                    const lastMsgInRoom = getedMsgInRoom.data[getedMsgInRoom.data.length - 1]; // 동적계산을 위해 length-1

                    webSocket.send(JSON.stringify({
                        type: "READ",
                        roomId: roomId,
                        userId: userID,
                        lastReadMessageId: lastMsgInRoom.messageId
                    }));
                }

            } catch (error) { // 추후 실패 처리 로직 설계를 위해. 
                console.error("메시지 조회 실패", error);

                if (error.response.status === 401) {
                    console.log(`401 error`);
                } else {
                    console.log(error);
                }
            }
        };

        webSocket.onmessage = handleMessage;

        return () => {
            webSocket.close();
            wsRef.current = null;
            isWsConnectedRef.current = false;
        }
    }, []);

    // ================ 메세지 전송 (WebSocket) =========================================================== 
    function sendMessage() {
        console.log(`${loginID} >> ${targetLoginID} Msg 전송`);
        console.log(`현재 wsRef : ${wsRef}`);

        if (!wsRef.current) {
            console.log("wsRef Null");
            return; // early Return
        }

        if (!isWsConnectedRef.current) {
            console.log("웹소켓 아직 연결중");
            return;
        } // ws객체 자체가 비었는지, 현재 연결중인지 구분하기 위해 if 분리.

        const sendData = {
            type: 'SEND',
            senderId: userID,
            senderLoginId: loginID,
            roomId: roomId,
            msgText: chatMessage
        };

        wsRef.current.send(JSON.stringify(sendData));

        setChatMessage('');

        // ws.onclose(); reconnect
        // ws.onerror(); reconnect
    }

    // ================ 최신 메세지 기준으로 보기. 스크롤 항상 최하단 ===============================================
    useEffect(() => {
        chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [prevChattings]);


    // ================ 채팅창 닫기 ===============================================
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

                                <div className="messageText">{d.msgText}</div>

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

export default K2ChatBoxForBackUp;

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

