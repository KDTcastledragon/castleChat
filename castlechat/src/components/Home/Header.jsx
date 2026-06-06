import './Header.css';

import { useNavigate } from 'react-router-dom';
import { useMe } from '../../hooks/useMe';

function Header() {

    const nav = useNavigate('');

    // ======< return >=======================================================================================================
    return (
        <div className='HeaderContainer'>
            <div className='HeaderButton'>
                <button onClick={() => nav('/')}>메인홈</button>
                <button onClick={() => nav('/friendList')}>친구목록</button>
                <button onClick={() => nav('/chatList')}>채팅</button>
            </div>
        </div>
    );
}

export default Header;