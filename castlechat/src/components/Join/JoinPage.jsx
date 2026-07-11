import './JoinPage.css';
import axios from 'axios';
import { useState, useEffect, useRef } from 'react';
import 'react-calendar/dist/Calendar.css';
import { useNavigate } from 'react-router-dom';
// import Calendar from 'react-calendar';
// import DatePicker from 'react-datepicker';
// import "react-datepicker/dist/react-datepicker.css";

function JoinPage() {
    const navigator = useNavigate();
    const profileImgInputRef = useRef(null);
    const [loginId, setLoginId] = useState('');
    const [validId, setValidId] = useState(false);


    const [password, setPassword] = useState('');
    const [vaildPw, setValidPw] = useState(false);
    const [showPw, setShowPw] = useState(false);
    const [confirmPw, setConfirmPw] = useState('');
    const [pwMsg, setPwMsg] = useState('');

    // const [name, setName] = useState('');
    // const [validName, setValidName] = useState(false);
    // const [nameMsg, setNameMsg] = useState('');

    const [nickname, setNickname] = useState('');
    const [validNickname, setValidNickname] = useState(false);
    const [nicknameMsg, setNicknameMsg] = useState('');
    const [profileImgFile, setProfileImgFile] = useState(null);

    // const [phoneNumber, setPhoneNumber] = useState('');
    // const [validPhoneNumber, setValidPhoneNumber] = useState(false);
    // const [phoneNumberMsg, setPhoneNumberMsg] = useState('');

    // const [year, setYear] = useState();
    // const [month, setMonth] = useState();
    // const [day, setDay] = useState();

    const [activeJoinButton, setActiveJoinButton] = useState(true);

    const loginIdRegex = /^[a-zA-Z0-9]*$/;
    const passwordRegex = /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[!@#$%^&*(),.?":{}|<>]).*$/;
    const noKorPwRegex = /^[^가-힣ㄱ-ㅎㅏ-ㅣ]*$/;
    const nicknameRegex = /^[가-힣a-zA-Z0-9]*$/;
    // const pwRegex = /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[!@#$%^&*(),.?":{}|<>]).{8,15}$/;
    // const pwRegex = /[!@#$%^&*(),.?":{}|<>]/;
    // const nameRegex = /^[가-힣a-zA-Z]*$/;
    // const phoneNumberRegex = /^010\d{7,8}$/;


    // ** 생년월일 select 생성
    // const currentYear = new Date().getFullYear();
    // const yearList = Array.from({ length: 130 }, (_, index) => currentYear - index);
    // const monthList = Array.from({ length: 12 }, (_, index) => index + 1);
    // const dayList = Array.from({ length: 31 }, (_, index) => index + 1)

    // ** 숫자가 1자리인 경우 앞에 0을 붙임
    // const formatDateNumber = (number) => {
    //     return String(number).padStart(2, '0');
    // };

    //======================
    function loginIdDuplicateCheck() {
        if (loginId.length < 5 || loginId.length > 10) {
            alert('아이디는 5자 이상, 10자 이하여야 합니다.');
            setValidId(false);
        } else if (!loginIdRegex.test(loginId)) {
            alert(`아이디는 영문과 숫자만 가능합니다.`);
            setValidId(false);
        } else {
            const data = { loginId: loginId }

            axios
                .post('/user/loginIdDuplicateCheck', data)
                .then((r) => {
                    alert(`사용가능한 아이디 입니다.`);
                    setValidId(true);

                }).catch((e) => {
                    setValidId(false);

                    if (e.response.status === 409) {
                        alert(`이미 존재하는 아이디 입니다.`);
                    } else {
                        alert(`오류`);
                    }
                })
        }
    }

    //======================
    function handleValidatePw() {
        if (password.length < 8 || password.length > 15) {
            setPwMsg('비밀번호는 8자 이상, 15자 이하여야 합니다.');
            setValidPw(false);
        } else if (!passwordRegex.test(password)) {
            setPwMsg('비밀번호에는 특수문자 , 숫자 , 영문이 모두 포함되어야 합니다.');
            setValidPw(false);
        } else if (!noKorPwRegex.test(password)) {
            setPwMsg('한글은 비밀번호에 포함될 수 없습니다.');
            setValidPw(false);
        } else {
            setPwMsg('validPw');
            setValidPw(true);
        }
    }

    // function handleValidateName() {
    //     if (name.length < 2) {
    //         setNameMsg('이름을 입력해주세요');
    //         setValidName(false);
    //     } else if (!nameRegex.test(name)) {
    //         setNameMsg('이름은 영문과 한글만 가능합니다');
    //         setValidName(false);
    //     } else {
    //         setValidName(true);
    //         setNameMsg('validName');
    //     }
    // }
    // ========================================
    function handleValidateNickname() {
        if (nickname.length <= 0) {
            setNicknameMsg('닉네임을 입력해주세요');
            setValidNickname(false);
        } else if (!nicknameRegex.test(nickname)) {
            setNicknameMsg('닉네임은 영문과 한글만 가능합니다');
            setValidNickname(false);
        } else {
            setValidNickname(true);
            setNicknameMsg('validNickname');
        }
    }

    // function handleValidatePhoneNumber() {
    //     if (phoneNumber.length < 11) {
    //         setPhoneNumberMsg('휴대폰번호 11자리를 모두 입력해주세요');
    //         setValidPhoneNumber(false);
    //     } else if (!phoneNumberRegex.test(phoneNumber)) {
    //         setPhoneNumberMsg('유효하지 않은 번호입니다');
    //         setValidPhoneNumber(false);
    //     } else {
    //         setPhoneNumberMsg('validPhoneNumber');
    //         setValidPhoneNumber(true);
    //     }
    // }

    function handleActivation() {
        // if (validId === true && vaildPw && pw === confirmPw && validName && year !== null && month !== null && day !== null && validPhoneNumber) {
        if (validId === true && vaildPw && password === confirmPw && validNickname) {
            setActiveJoinButton(false);
        } else {
            setActiveJoinButton(true);
        }
    }

    useEffect(() => {
        handleActivation();
        console.log(`===========================================activation UseEffect======================================`);
    }, [validId, vaildPw, validNickname]);
    // }, [validId, vaildPw, validName, year, month, day, validPhoneNumber]);


    //======================================================================================================================
    function join() {
        // console.log(`${year}-${month}-${day}`);

        // if (validId && vaildPw && pw === confirmPw && validName && year !== null && month !== null && day !== null && validPhoneNumber) {
        if (validId && vaildPw && password === confirmPw && validNickname) {

            let joinRequestData;

            if (profileImgFile) {
                const joinFormData = new FormData();

                joinFormData.append('loginId', loginId);
                joinFormData.append('password', password);
                joinFormData.append('nickname', nickname);
                joinFormData.append('profileImgFile', profileImgFile);

                joinRequestData = joinFormData;
            } else {
                joinRequestData = {
                    loginId,
                    password,
                    nickname
                };
            }

            axios
                .post(`/user/join`, joinRequestData)
                .then((r) => {
                    navigator('/');
                    alert(`회원가입 완료. 로그인 페이지로 이동합니다.`);
                }).catch((e) => {
                    if (e.response && e.response.status === 400) {
                        alert(`각 항목을 조건에 맞게 다시 입력해주세요`);
                    } else {
                        alert(`회원가입 서버 오류`);
                    }
                })

        } else if (validId === false) {
            alert(`아이디 중복체크 검사를 해야합니다`);
        } else {
            alert(`각 항목을 조건에 맞게 모두 입력해주세요`);
        }
    }// join

    function selectProfileImage(e) {
        const file = e.target.files?.[0];
        e.target.value = '';

        if (!file) return;

        if (!file.type.startsWith('image/')) {
            alert('이미지 파일만 선택할 수 있습니다.');
            return;
        }

        if (file.size > 10 * 1024 * 1024) {
            alert('프로필 이미지는 최대 10MB까지 가능합니다.');
            return;
        }

        setProfileImgFile(file);
    }


    //======================================================================================================================
    // console.log(id);
    // console.log(pw);
    // console.log(name);
    // console.log(`${year}-${month}-${day}`);
    // console.log(phoneNumber);

    //======< return >===============================================================================================================
    return (
        <div className='JoinPageContainer'>
            <div className='joinTitle'><span>회원가입</span></div>

            <div className='joinMemberData'>
                <div className='joinId'>
                    <span>아이디</span>
                    <input type="text" value={loginId} onKeyUp={handleActivation} onChange={(e) => {
                        setLoginId(e.target.value)
                        setValidId(false);
                    }}
                        required autoComplete='off' minLength={5} maxLength={10}
                    />
                    {validId ? <span className='validIdTrue'>사용가능</span>
                        : <button onClick={() => loginIdDuplicateCheck(loginId)}>중복체크</button>}

                </div>

                <div className='joinPw'>
                    <span>비밀번호</span>
                    <input type={showPw ? 'text' : "password"} value={password} onKeyUp={handleActivation}
                        onChange={(e) => {
                            setPassword(e.target.value)
                        }}
                        required autoComplete='off' minLength={8} maxLength={15} placeholder='특수문자 포함 8~15자리' onBlur={handleValidatePw}
                    />
                    {pwMsg === 'validPw' ? <span className='validPw'>적절한 비밀번호</span> : <span className='inValidPw'>{pwMsg}</span>}

                </div>

                <div className='joinConfirmPw'>
                    <span>비밀번호 확인</span>
                    <input type={showPw ? 'text' : "password"} value={confirmPw} onKeyUp={handleActivation}
                        onChange={(e) => { setConfirmPw(e.target.value) }}
                        required autoComplete='off' minLength={8} maxLength={15} placeholder='비밀번호 재입력'
                    />
                    {vaildPw === true && password !== null && confirmPw !== null && password === confirmPw ? <span className='confirmedPw'>비밀번호 일치</span>
                        : vaildPw === true && password !== null && confirmPw !== null && password !== confirmPw ? <span className='denyPw'>비밀번호 불일치</span>
                            : <span className='denyPw'>비밀번호 검사 필요</span>}
                    <button onClick={() => setShowPw(p => !p)}>
                        {showPw ?
                            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="currentColor" class="bi bi-eye-slash" viewBox="0 0 16 16">
                                <path d="M13.359 11.238C15.06 9.72 16 8 16 8s-3-5.5-8-5.5a7 7 0 0 0-2.79.588l.77.771A6 6 0 0 1 8 3.5c2.12 0 3.879 1.168 5.168 2.457A13 13 0 0 1 14.828 8q-.086.13-.195.288c-.335.48-.83 1.12-1.465 1.755q-.247.248-.517.486z" />
                                <path d="M11.297 9.176a3.5 3.5 0 0 0-4.474-4.474l.823.823a2.5 2.5 0 0 1 2.829 2.829zm-2.943 1.299.822.822a3.5 3.5 0 0 1-4.474-4.474l.823.823a2.5 2.5 0 0 0 2.829 2.829" />
                                <path d="M3.35 5.47q-.27.24-.518.487A13 13 0 0 0 1.172 8l.195.288c.335.48.83 1.12 1.465 1.755C4.121 11.332 5.881 12.5 8 12.5c.716 0 1.39-.133 2.02-.36l.77.772A7 7 0 0 1 8 13.5C3 13.5 0 8 0 8s.939-1.721 2.641-3.238l.708.709zm10.296 8.884-12-12 .708-.708 12 12z" />
                            </svg>

                            :
                            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="currentColor" class="bi bi-eye-fill" viewBox="0 0 16 16">
                                <path d="M10.5 8a2.5 2.5 0 1 1-5 0 2.5 2.5 0 0 1 5 0" />
                                <path d="M0 8s3-5.5 8-5.5S16 8 16 8s-3 5.5-8 5.5S0 8 0 8m8 3.5a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7" />
                            </svg>
                        }
                    </button>

                </div>

                {/* <div className='joinName'>
                    <span>이름</span>
                    <input type="text" value={name} onKeyUp={handleActivation}
                        onChange={(e) => {
                            setName(e.target.value)
                            setNameMsg('')
                        }}
                        required autoComplete='off' minLength={1} maxLength={30} onBlur={handleValidateName}
                    />
                    {nameMsg === 'validName' ? <span className='validName'>적절한 이름</span> : <span className='inValidName'>{nameMsg}</span>}
                </div> */}

                <div className='joinName'>
                    <span>닉네임</span>
                    <input type="text" value={nickname} onKeyUp={handleActivation}
                        onChange={(e) => {
                            setNickname(e.target.value)
                            setNicknameMsg('')
                        }}
                        required autoComplete='off' minLength={1} maxLength={20} onBlur={handleValidateNickname}
                    />
                    {nicknameMsg === 'validNickname' ? <span className='validName'>적절한 닉네임</span> : <span className='inValidName'>{nicknameMsg}</span>}
                </div>

                <div className='joinProfileImage'>
                    <span>프로필 이미지</span>
                    <strong title={profileImgFile?.name ?? 'mococo_question.png'}>
                        {profileImgFile?.name ?? 'mococo_question.png'}
                    </strong>
                    <button type="button" onClick={() => profileImgInputRef.current?.click()}>
                        파일 선택
                    </button>
                    <input
                        ref={profileImgInputRef}
                        type="file"
                        accept="image/*"
                        hidden
                        onChange={selectProfileImage}
                    />
                </div>

                {/* <div className='joinBirth'>
                    <span className=''>생년월일</span>
                    <select required onChange={(e) => setYear(e.target.value)}>
                        {yearList.map((year) => (
                            <option key={year} value={year}>
                                {year}
                            </option>
                        ))}
                    </select>
                    <span>년</span>

                    <select required onChange={(e) => setMonth(formatDateNumber(e.target.value))}>
                        {monthList.map((month) => (
                            <option key={month} value={formatDateNumber(month)}>{formatDateNumber(month)}</option>
                        ))}
                    </select>
                    <span>월</span>

                    <select required onChange={(e) => setDay(formatDateNumber(e.target.value))}>
                        {dayList.map((day) => (
                            <option key={day} value={formatDateNumber(day)}>{formatDateNumber(day)}</option>
                        ))}
                    </select>
                    <span>일</span>
                </div> */}

                {/* <div className='joinPhoneNumber'>
                    <span>휴대폰번호</span>
                    <input type="text" value={phoneNumber} onKeyUp={handleActivation}
                        onChange={(e) => {
                            setPhoneNumber(e.target.value)
                            setPhoneNumberMsg('');
                        }}
                        required autoComplete='off'
                        minLength={11} maxLength={11} placeholder={` ' - ' 없이 11자리 작성`} onBlur={handleValidatePhoneNumber}
                    />
                    {phoneNumberMsg === 'validPhoneNumber' ? <span className='validPhoneNumber'>적절한 번호</span>
                        : <span className='inValidPhoneNumber'>{phoneNumberMsg}</span>}
                </div> */}
            </div>

            <div className='joinButton'><button onClick={join} disabled={activeJoinButton}>가입하기</button></div>
        </div>
    );
}

export default JoinPage;
