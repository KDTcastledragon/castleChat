import axios from 'axios';

export const loginApi = async (data) => {
    const res = await axios.post('/user/login', data);
    return res.data;
};

export const logoutApi = async () => {
    await axios.post('/user/logout');
};

export const meApi = async () => {
    const res = await axios.get('/user/isMe');
    return res.data;
};

export const updateMyProfileApi = async (data) => {
    const res = await axios.post('/user/updateMyProfile', data);
    return res.data;
};

export const changeMyPasswordApi = async (data) => {
    const res = await axios.post('/user/changeMyPassword', data);
    return res.data;
};

export const allUsersApi = async () => {
    const res = await axios.get('/user/allUsers');
    return res.data;
};

export const searchUsersApi = async (searchWord) => {
    const res = await axios.get('/user/searchUsers', {
        params: { searchWord }
    });
    return res.data;
};

// api.js
// = 어디로, 어떤 HTTP 메서드로 보낼지
// = axios 요청
// = response.data 반환

// custom hook
// = 언제 요청할지
// = 성공하면 캐시를 어떻게 바꿀지
// = 실패하면 재시도할지
// = enabled 조건
// = queryKey를 뭘로 잡을지

// 조금 더 구체적으로 layer 설명----->

// api layer
// = transport layer
// = HTTP 통신 책임

// query hook layer
// = server state layer
// = 캐싱, 무효화, 재요청, 성공/실패 후 처리 책임

// component layer
// = UI layer
// = 입력, 클릭, 화면 이동, alert 책임

// --> 정리 : Api.js는 request/response만 담당하고, TanStack 커스텀 훅은 그 요청을 어떤 서버 상태로 관리할지 결정한다.

// api layer
// - 어디로 요청할지
// - 어떤 method인지
// - request/response data

// hook/component layer
// - 언제 호출할지
// - 성공하면 뭘 할지
// - 실패하면 뭘 보여줄지
