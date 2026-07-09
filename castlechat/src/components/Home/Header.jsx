import './Header.css';

import { useNavigate } from 'react-router-dom';
import { useMe, useLogout } from '../../hooks/useAuthUser';

function Header() {

    const nav = useNavigate();  //  --> useNavigate안에 ('') 할 필요는 없다. 없는게 정석.

    const { data: me } = useMe();
    const logoutMutation = useLogout();

    // ====== 로그아웃 ===========================================================================================
    function logout() {
        logoutMutation.mutate();
    }

    // ======< return >=======================================================================================================
    return (
        <div className='HeaderContainer'>
            {me ?
                <div className='loginedHeader'>
                    <div className='loginedUserProfileBox'>
                        <div className='loginedUserProfileImg'>
                            <img className="headerProfileImg"
                                src={me.profileImg || "/images/mococo_question.png"}
                                alt={me.nickname} />
                        </div>
                        <div className='loginedUserNickname'><span>{me.nickname}</span></div>
                        <div className='loginedUserFriendCode'><span>{me.friendCode}</span></div>
                        <div className='loginedUserLogoutButton'>
                            <button onClick={logout}>로그아웃</button>
                        </div>
                    </div>

                    <div className='headerButtonSection'>
                        <button onClick={() => nav('/')}>친구목록</button>
                        <button onClick={() => nav('/chatList')}>채팅</button>
                        <button onClick={() => nav('/myPage')}>내정보</button>
                        <button onClick={() => nav('/settings')}>설정</button>
                    </div>

                </div>
                :
                <div className='loginedHeader'>
                    caslteChat &nbsp;&nbsp;
                </div>
            }

            {/* <div className='loginedHeader'>
                <div className='loginedUserProfileBox'>
                    <div className='loginedUserProfileImg'>
                        <img className="headerProfileImg"
                            src="/images/mococo_question.png"
                            alt="-_-" />
                    </div>
                    <div className='loginedUserNickname'><span>공성전차</span></div>
                    <div className='loginedUserFriendCode'><span>#8475621</span></div>
                    <div className='loginedUserLogoutButton'>
                        <button onClick={() => alert(`로그아웃`)}>로그아웃</button>
                    </div>
                </div>

                <div className='headerButtonSection'>
                    <button onClick={() => nav('/')}>친구목록</button>
                    <button onClick={() => nav('/chatList')}>채팅</button>
                    <button onClick={() => nav('/settings')}>설정</button>
                </div>

            </div> */}

        </div>
    );
}

export default Header;
