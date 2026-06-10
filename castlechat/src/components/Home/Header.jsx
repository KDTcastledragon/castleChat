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
            <div className='headerUserInfo'>
                {me ?
                    <div className='loginedHeader'>
                        <div>{me.profileImg ? me.profileImg : '프사 없음'}</div>
                        <div>{me.nickname}</div>
                        <div>{me.friendCode}</div>
                        <div>
                            <button
                                onClick={logout}
                                disabled={logoutMutation.isPending}
                            >
                                로그아웃
                            </button>
                        </div>

                        <div className='headerButton'>
                            <button onClick={() => nav('/')}>친구목록</button>
                            <button onClick={() => nav('/chatList')}>채팅</button>
                            <button onClick={() => nav('/settings')}>설정</button>
                        </div>

                    </div>
                    :
                    <div className='logoutedHeader'>
                        <span>CastleChat</span>
                        <div className='headerButton'>
                            <button onClick={() => nav('/')}>친구목록</button>
                            <button onClick={() => nav('/chatList')}>채팅</button>
                            <button onClick={() => nav('/settings')}>설정</button>
                        </div>
                    </div>
                }
            </div>

        </div>
    );
}

export default Header;