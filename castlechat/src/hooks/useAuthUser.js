import { useMutation, useQueryClient, useQuery } from '@tanstack/react-query';
import { loginApi, logoutApi, meApi } from '../api/userApi';
import { disconnectWs } from '../webSocket/wsClient';
import { useNavigate } from 'react-router-dom';

// ====== 로그인 =============================================================================================================
export function useLogin() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: loginApi,
        onSuccess: (me) => {
            queryClient.setQueryData(['me'], me);
        },
    });
}

// ====== 로그아웃 =============================================================================================================
export function useLogout() {
    const queryClient = useQueryClient();
    const nav = useNavigate();

    return useMutation({
        // mutationFn: logoutApi,
        mutationFn: async () => {
            disconnectWs('logoutByUser'); // WebSocket.close()는 Promise를 반환하지 않음. 동기함수임. 즉 기다릴 수 있는 함수가 아님. 그래서 await 안 붙임.
            // 나중에 서버에서 “로그아웃 중인데 WS가 살아있어서 꼬임” 같은 문제가 실제로 생기면 그때 disWs()를 Promise+timeout 버전으로 강화하면 됨.
            await logoutApi(); // logoutApi()가 끝날 때까지 기다린다는 뜻이야. 로그아웃 API 실패했는데도 화면은 로그인으로 가버리는 대참사를 막을 수 있음.
        },
        onSuccess: () => {
            // queryClient.setQueryData(['me'], null);
            // queryClient.removeQueries({ queryKey: ['users'] });
            // clear()가 위의 두줄포함 싹싹 비워줌.
            queryClient.clear();

            nav('/login');
        },
    });
}

// mutationFn , onSuccess : 기본 설정이야. 즉 이 mutation이 성공할 때마다 기본적으로 실행됨.
// --> 실제 사용 components : mutate(variables, options)
// 첫 번째 인자 = mutationFn에 전달할 data.
// 두 번째 인자 = 이번 mutate 호출에서만 사용할 추가 옵션.
// logoutMutation.mutate(null, --> 여기서 null은, 'mutationFn에 넘길 데이터가 없다'라는 뜻이다. 로그아웃은 보낼 데이터가 없잖아.

// ====== 본인 인증 =============================================================================================================
export function useMe() {
    return useQuery({
        queryKey: ['me'],
        queryFn: meApi,
        retry: false,
    });
}