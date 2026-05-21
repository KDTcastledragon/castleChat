import './Home.css';

import axios from 'axios';
import { useEffect, useState } from 'react';
import LogIn from '../LogIn/LogIn';
import ChatBox from '../Chattings/ChatBox';

function Home() {
    const userID = sessionStorage.getItem('userID');
    const loginID = sessionStorage.getItem('loginID');

    const [userList, setUserList] = useState([]);
    const [enteredID, setEnteredID] = useState('');
    const [targetUserID, setTargetUserID] = useState('');
    const [targetLoginID, setTargetLoginID] = useState('');
    const [roomId, setRoomId] = useState(null);
    const [friList, setFriList] = useState([]);
    const [isChattingOpen, setIsChattingOpen] = useState(false);

    const [chatRooms, setChatRooms] = useState([]);


    // =====[유저 목록 전체]======================================================
    useEffect(() => {
        axios
            .get(`/user/allUsers`)
            .then((res) => {
                console.log(`모든유저`);
                setUserList(res.data);
                console.log(res.data);
            }).catch((e) => {
                console.log(e.message);
            });
    }, [])

    // =====[로그인/로그아웃 함수]======================================================
    function login(id1, pw1) {
        sessionStorage.clear();
        const data = { id: id1, pw: pw1 }

        axios
            .post(`/user/login`, data)
            .then((res) => {
                sessionStorage.setItem('userID', res.data.userId); // res안의 data안에 정보가 있다.
                sessionStorage.setItem('loginID', res.data.loginId);
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

    function logout() {
        sessionStorage.clear();
        // alert(`로그아웃 성공`);
        window.location.reload();
    }

    // ==== 2. 내 채팅방 목록 불러오기 ===================================================
    useEffect(() => {

        if (!userID) return;

        axios.get(`/chat/myRooms/${userID}`)
            .then((res) => {
                setChatRooms(res.data);
                console.log("내 채팅방 목록", res.data);
            })
            .catch((err) => {
                console.log(err);
            });

    }, [userID]);

    // ====채팅방 오픈 함수 ===================================================
    const openChattingRoom = async (targetUser) => {
        try {
            const res = await axios.post(`/chat/enterRoom`,
                {
                    senderId: userID,
                    targetUserId: targetUser.userId
                });

            setRoomId(res.data.roomId);
            setTargetLoginID(targetUser.loginId);

            setIsChattingOpen(true);
            console.log(`${targetUser.loginId}한테 대화 요청!`);
        } catch (e) {
            console.log(e);
        }
    }

    // =========================================================
    return (
        <div className='HomeContainer'>

            {/**============== 로그인 구역==================== */}
            <div className='loginSection'>
                {loginID ?
                    <>
                        <div className='loginForm'> {loginID} 님 -- ({userID})</div>

                        <button onClick={() => logout()}>로그아웃</button>
                    </>
                    :
                    <>
                        <div className='loginForm'>
                            ID : <input
                                type="text"
                                value={enteredID}
                                onChange={(e) => setEnteredID(e.target.value)}
                            />
                        </div>
                        <button onClick={() => login(enteredID)}>로그인</button>
                    </>
                }

            </div>

            {/**========= 유저 목록 및 채팅 오픈 버튼=================== */}
            <div>
                {userList.length > 0 ? userList.map((d, i) => (
                    <>
                        <button
                            key={d.userId}
                            onClick={() => openChattingRoom(d)}>
                            {d.loginId}-({d.userId})
                        </button><span>&nbsp;&nbsp;&nbsp;</span>
                    </>
                ))
                    :
                    <div>유저없음</div>
                }
            </div>




            {/**========= 채팅창 =================== */}
            {
                isChattingOpen &&
                <ChatBox
                    setIsChattingOpen={setIsChattingOpen}
                    targetUserID={targetUserID}
                    targetLoginID={targetLoginID}
                    roomId={roomId}
                />
            }

            {/* <div className='friendsListSection'>
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
            </div> */}

        </div >
    );
}

export default Home;