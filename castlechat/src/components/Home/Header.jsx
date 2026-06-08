import './Header.css';

import { useNavigate } from 'react-router-dom';
import { useMe } from '../../hooks/useMe';
import { useLogout } from '../../hooks/useLogout';

function Header() {

    const nav = useNavigate();  //  --> useNavigate안에 ('') 할 필요는 없다. 없는게 정석.

    const { data: me } = useMe();
    const logoutMutation = useLogout();

    // ===== 로그아웃 ===========================================================================================
    // function logout() {
    //     if (wsRef.current) {
    //         wsRef.current.close();
    //         wsRef.current = null;
    //         console.log(`로그아웃 및 ws 연결종료`);
    //     }

    //     isWsConnectedRef.current = false;

    //     logoutMutation.mutate(null, {
    //         onSuccess: () => {
    //             navigator('/login');
    //         }
    //     });
    // }

    // ======< return >=======================================================================================================
    return (
        <div className='HeaderContainer'>
            <div className='headerUserInfo'>
                {me ?
                    <div className='loginedHeader'>
                        <div>{me.profileImg ? me.profileImg : '프사 없음'}</div>
                        <div>{me.nickname}</div>
                        <div>{me.friendCode}</div>
                        {/* <div><button onClick={logout}>로그아웃</button></div> */}
                        <div><button onClick={() => alert(`logout`)}>로그아웃</button></div>

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