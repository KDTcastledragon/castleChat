import './Home.css';

import axios from 'axios';
import { useEffect, useState } from 'react';

function Home() {
    const [chatting, setChatting] = useState('');

    function sendChat(data) {
        alert(`메시지 전송`);

        axios
            .post(`/chat/sendMessage`, data)
            .then(() => {
                alert(`성공`);
            }).catch((e) => {
                console.log(e.message);
            })
    }



    return (
        <div className='HomeContainer'>
            <div className='friendsListSection'>
                <div className='chatListTitle'><span>친구 목록</span></div>
                <div className='chatRoomList'>
                    <div>친구1</div>
                    <div>친구2</div>
                    <div>3</div>
                    <div>4</div>
                    <div>4</div>
                    <div>4</div>
                    <div>4</div>
                    <div>4</div>
                    <div>4</div>
                    <div>4</div>
                    <div>4</div>
                </div>
            </div>
            <div className='chattingRoomListSection'>
                <div className='chatListTitle'><span>채팅방 목록</span></div>
                <div className='chatRoomList'>
                    <div>1</div>
                    <div>2</div>
                    <div>3</div>
                    <div>4</div>
                    <div>4</div>
                    <div>4</div>
                    <div>4</div>
                    <div>4</div>
                    <div>4</div>
                    <div>4</div>
                    <div>4</div>
                </div>
            </div>
            <div className='chattingRoomSection'>
                <div className='chatListTitle'><span>채팅방</span></div>
                <div className='chattingBox'>
                    <div>message1</div>
                    <div>message1</div>
                    <div>message1</div>
                    <div>message1</div>
                    <div>message1</div>
                    <div>message1</div>
                    <div>message1</div>
                    <div>message1</div>
                    <div>message1</div>
                    <div>message1</div>
                    <div>message1</div>
                    <div>message1</div>
                    <div>message1</div>
                    <div>message1</div>
                    <div>message1</div>
                </div>
                <div className='inputChat'>
                    <textarea type="text" value={chatting} onChange={(e) => setChatting(e.target.value)} placeholder='여기에 메세지 입력.....' />
                    <button onClick={() => sendChat(chatting)}>전송</button>
                </div>
            </div>
        </div>
    );
}

export default Home;