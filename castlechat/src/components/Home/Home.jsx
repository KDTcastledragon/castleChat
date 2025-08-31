import './Home.css';

import axios from 'axios';
import { useEffect, useState } from 'react';
import LogIn from '../LogIn/LogIn';
import ChatBox from '../Chattings/ChatBox';

function Home() {
    const [myChatList, setMyChatList] = useState('');
    const loginID = sessionStorage.getItem('loginID');
    const [friList, setFriList] = useState([]);
    const [isOpenChatBox, setIsOpenChatBox] = useState(false);
    const [friendIdForChat, setFriendIdForChat] = useState('');

    // =====[로그인]======================================================
    useEffect(() => {
        const data2 = {
            user_id: loginID
        }

        axios
            .post('/user/friendList', data2)
            .then((r) => {
                setFriList(r.data);
                console.log(r.data);
                // alert(`성공`)
            }).catch((e) => {
                console.log(e);
                alert(`실패`);
            })
    }, [])

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
                            console.log(e);
                            break;
                    }
                } else {
                    alert(`알 수 없는 오류`);
                }
            });

    }

    function openChatBoxWithFriend(friId) {
        setFriendIdForChat(friId);
        setIsOpenChatBox(true);

    }

    // =========================================================
    return (
        <div className='HomeContainer'>
            <div className='friendsListSection'>
                <div className='friendsListTitle'><span>친구 목록</span></div>
                <div className='friendsList'>
                    {friList && friList.length > 0 ?
                        friList.map((d, i) => (
                            <>
                                <div className='friend' onClick={() => openChatBoxWithFriend(d.user_id)}>
                                    <div>{i + 1}번째 친구</div>
                                    <div>ID : {d.user_id}</div>
                                    <div>이름 : {d.user_name}</div>
                                    <hr />
                                </div>
                            </>
                        ))
                        :
                        <>
                            <div>나는 개똥벌레,, 친구가 없네....</div>
                            <div>어서 친추 ㄱㄱ</div>
                        </>
                    }
                </div>
            </div>
            {/* <div className='chattingListSection'>
                <div className='chatListTitle'><span>채팅방 목록</span></div>
                <div className='chatList'>
                </div>
            </div> */}

            <div className='logTest'>
                <div>현재 : {loginID}</div>
                <button onClick={() => login('123', '123')}>123</button>
                <button onClick={() => login('456', '456')}>456</button>
                <button onClick={() => login('789', '789')}>789</button>
                <button onClick={() => login('321', '321')}>321</button>
                <button onClick={() => login('654', '654')}>654</button>
                <button onClick={() => login('987', '987')}>987</button>
            </div>

            {isOpenChatBox &&
                <ChatBox
                    setIsOpenChatBox={setIsOpenChatBox}
                    friendIdForChat={friendIdForChat}
                />}

        </div>
    );
}

export default Home;