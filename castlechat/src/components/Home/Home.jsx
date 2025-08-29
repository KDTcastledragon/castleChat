import './Home.css';

import axios from 'axios';
import { useEffect, useState } from 'react';
import LogIn from '../LogIn/LogIn';

function Home() {
    const [sendChat, setSendChat] = useState('');
    const [myChatList, setMyChatList] = useState('');
    const loginID = sessionStorage.getItem('loginID');

    // =====[로그인]======================================================
    function login(id1, pw1) {
        sessionStorage.clear();


        const data = { id: id1, pw: pw1 }

        axios
            .post(`/user/login`, data)
            .then((res) => {
                sessionStorage.setItem('loginID', id1);
                alert(`${id1}`);
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
                            break;
                    }
                } else {
                    alert(`알 수 없는 오류`);
                }
            });

    }
    // =========================================================

    function sendMessage(data) {
        alert(`메시지 전송`);

        const sendData = {
            msg: data
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
        <div className='HomeContainer'>
            <div className='friendsListSection'>
                <div className='friendsListTitle'><span>친구 목록</span></div>
                <div className='friendsList'>
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
            <div className='chattingListSection'>
                <div className='chatListTitle'><span>채팅방 목록</span></div>
                <div className='chatList'>
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
                <div className='chatListTitle'><span>채팅방()</span></div>
                <div className='chattingBox'>
                    { }
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
                    <textarea type="text" value={sendChat} onChange={(e) => setSendChat(e.target.value)} placeholder='여기에 메세지 입력.....' />
                    <button onClick={() => sendMessage(sendChat)}>전송</button>
                </div>
            </div>
            <div className='test'>
                <div></div>
            </div>

            <div className='logTest'>
                <div>현재 : {loginID}</div>
                <button onClick={() => login(123, 123)}>123</button>
                <button onClick={() => login(456, 456)}>456</button>
                <button onClick={() => login(789, 789)}>789</button>
                <button onClick={() => login(321, 321)}>321</button>
                <button onClick={() => login(654, 654)}>654</button>
                <button onClick={() => login(987, 987)}>987</button>
            </div>
        </div>
    );
}

export default Home;