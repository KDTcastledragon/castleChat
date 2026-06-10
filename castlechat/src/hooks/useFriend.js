import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getFriendListApi, addFriendApi, getReceivedFriendRequestsApi, respondFriendRequestApi } from '../api/friendApi';

// ====== 친구 목록 =============================================================================================================
export function useFriendList(enabled) {
    return useQuery({
        queryKey: ['friends'],
        queryFn: getFriendListApi,
        enabled,
        retry: false
    });
}

// ====== 친구 추가 =============================================================================================================
export function useAddFriend() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: addFriendApi,
        onSuccess: () => {
            // queryClient.invalidateQueries({ queryKey: ['friends'] });
            queryClient.invalidateQueries({ queryKey: ['searchUsers'] });
        },
    });
}

// ====== 받은 친구 요청 목록 =============================================================================================================
export function useReceivedFriendRequests(enabled) {
    return useQuery({
        queryKey: ['receivedFriendRequests'],
        queryFn: getReceivedFriendRequestsApi,
        enabled,
        retry: false
    });
}

// ====== 친구요청 응답 하기 =============================================================================================================
export function useRespondFriendRequest() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: respondFriendRequestApi,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['friends'] });
            queryClient.invalidateQueries({ queryKey: ['receivedFriendRequests'] });
            queryClient.invalidateQueries({ queryKey: ['searchUsers'] });
        },
    });
}