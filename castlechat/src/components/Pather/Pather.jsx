// import './Pather.css';

import Header from '../Home/Header';
import RouteBody from '../Home/RouteBody';
import LogIn from '../LogIn/LogIn';

function Pather() {
    const adminCode = sessionStorage.getItem('adminCode');

    return (
        <>
            {/* {adminCode === 'admin' ?
                <>
                    <Header></Header>
                    <RouteBody></RouteBody>
                </>
                :
                <>
                    <LogIn></LogIn>
                </>
            } */}
        </>

    )
}

export default Pather;