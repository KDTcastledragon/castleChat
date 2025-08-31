import './ChatBox.css';

import axios from 'axios';
import { useEffect, useState } from 'react';

function ChatBox({ friendIdForChat, setIsOpenChatBox }) {
    const [chatMessage, setChatMessage] = useState('');
    const [myChattings, setMyChattings] = useState([]);
    const loginID = sessionStorage.getItem('loginID');

    useEffect(() => {
        const data = {
            user_id: loginID,
            fri_id: friendIdForChat
        }

        axios
            .post(`/chat/getChattingWithFriend`, data)
            .then((r) => {
                setMyChattings(r.data);
            }).catch((e) => {
                console.log(e);
                alert(`채팅불러오기실패.`);
            })
    }, [])


    function sendMessage(messageData) {
        const sendData = {
            user_id: loginID,
            fri_id: friendIdForChat,
            msg: messageData
        }

        axios
            .post(`/chat/sendMessage`, sendData)
            .then(() => {
                alert(`성공`);
            }).catch((e) => {
                console.log(e.message);
            })
    }


    return (
        <div className='chattingRoomSection'>
            <div className='chatListTitle'><span>채팅방 - {friendIdForChat}</span> <button onClick={() => setIsOpenChatBox(false)}>닫기</button></div>
            <div className='chattingBox'>
                {myChattings && myChattings.length > 0 ?
                    myChattings.map((d, i) => (
                        <div>{d.message}</div>
                    ))
                    :
                    <div>아직 채팅을 안 해봣군요?ㅋㅋ</div>
                }
            </div>
            <div className='inputChat'>
                <textarea type="text" value={chatMessage} onChange={(e) => setChatMessage(e.target.value)} placeholder='여기에 메세지 입력.....' />
                <button onClick={() => sendMessage(chatMessage)}>전송</button>
            </div>
        </div>
    );
}

export default ChatBox;