import './LoginPage.css';

// import axios from 'axios'; // axios 안 쓰고 있넹...?!
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { useLogin } from '../../hooks/useAuthUser';

// import { useQueryClient } from '@tanstack/react-query';

function LoginPage() {
    const navigator = useNavigate();
    const loginMutation = useLogin();
    // const queryClient = useQueryClient();

    // ==============================================

    const [loginId, setLoginId] = useState('');
    const [password, setPassword] = useState('');


    // =====[로그인 함수]======================================================

    function login(enteredLoginID, enteredPassword) {
        loginMutation.mutate(
            { loginId: enteredLoginID, password: enteredPassword },
            {
                onSuccess: () => {
                    navigator('/');
                },
                onError: (e) => {
                    if (e.response?.status === 401) {
                        alert('아이디 또는 비밀번호가 틀립니다.');
                    } else if (e.response?.status === 403) {
                        alert('이용이 제한된 사용자입니다.');
                    } else {
                        alert('로그인 오류');
                        console.log(e);
                    }
                }
            }
        );
    }



    // ======< return >================================================================================================
    return (
        <div className='LogInPageContainer'
            onKeyDown={(e) => {
                if (e.key === 'Enter' && !loginMutation.isPending) {
                    e.preventDefault();
                    login(loginId, password);
                }
            }}
        >
            <div className='loginTitle'><span>로그인</span></div>
            <div className='loginBox'>
                <div className='loginIdPw'>
                    <div className='loginId'>
                        <span>아이디</span>
                        <input type="text" value={loginId}
                            onChange={(e) => setLoginId(e.target.value)} minLength={5} />
                    </div>
                    <div className='loginPw'>
                        <span>비밀번호</span>
                        <input type="password" value={password}
                            onChange={(e) => setPassword(e.target.value)} minLength={7} />
                    </div>

                    <div className='findAndJoinBox'>
                        <button onClick={() => navigator('/')}>아이디/비밀번호 찾기</button>
                        <button onClick={() => navigator('/join')}>회원가입</button>
                    </div>
                </div>

                <div className='loginButton'>
                    <button
                        onClick={() => login(loginId, password)}
                        disabled={loginMutation.isPending}
                    >
                        {loginMutation.isPending ? '로그인 중...' : '로그인'}
                    </button>
                    {/* <button onClick={() => login(loginId, password)}>로그인</button> */}
                </div>
            </div>

        </div>
    );
}

export default LoginPage;


// function login(enteredLoginID, enteredPassword) {
//     const data = { loginId: enteredLoginID, password: enteredPassword }

//     axios
//         .post(`/user/login`, data)
//         .then((res) => {
//             queryClient.setQueryData(['me'], res.data);
//             navigator('/');

//         }).catch((e) => {
//             if (e.response.status) {
//                 switch (e.response.status) {
//                     case 401:
//                         alert('아이디 또는 비밀번호가 틀립니다.');
//                         break;

//                     case 403:
//                         alert('이용이 제한된 사용자입니다.');
//                         break;

//                     default:
//                         alert(`로그인 오류`);
//                         console.log(e);
//                         break;
//                 }
//             } else {
//                 alert(`알 수 없는 오류`);
//             }
//         });

// }