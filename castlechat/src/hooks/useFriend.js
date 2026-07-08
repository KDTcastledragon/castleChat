import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getFriendListApi, addFriendApi, getReceivedFriendRequestsApi, respondFriendRequestApi } from '../api/friendApi';

export function addReceivedFriendRequestToCache(queryClient, payload) {
    if (!payload?.requesterPublicId) {
        return;
    }

    const requestUser = {
        publicId: payload.requesterPublicId,
        nickname: payload.requesterNickname || payload.requesterPublicId,
        friendCode: payload.requesterFriendCode || '',
        profileImg: payload.requesterProfileImg || null
    };

    queryClient.setQueryData(['receivedFriendRequests'], (prev = []) => {
        const prevList = Array.isArray(prev) ? prev : [];
        const alreadyExists = prevList.some(user => user.publicId === requestUser.publicId);

        if (alreadyExists) {
            return prevList.map(user => user.publicId === requestUser.publicId ? { ...user, ...requestUser } : user);
        }

        return [requestUser, ...prevList];
    });
}

export function removeReceivedFriendRequestFromCache(queryClient, payload) {
    const requesterPublicId = typeof payload === 'string' ? payload : payload?.requesterPublicId;

    if (!requesterPublicId) {
        return;
    }

    queryClient.setQueryData(['receivedFriendRequests'], (prev = []) => {
        const prevList = Array.isArray(prev) ? prev : [];
        return prevList.filter(user => user.publicId !== requesterPublicId);
    });
}

export function addAcceptedFriendToCache(queryClient, payload, myPublicId) {
    if (payload?.friendStatus !== 'ACCEPTED') {
        return;
    }

    let friendProfile = null;

    if (myPublicId === payload.requesterPublicId) {
        friendProfile = {
            publicId: payload.targetPublicId,
            nickname: payload.targetNickname || payload.targetPublicId,
            friendCode: payload.targetFriendCode || '',
            profileImg: payload.targetProfileImg || null
        };
    }

    if (myPublicId === payload.targetPublicId) {
        friendProfile = {
            publicId: payload.requesterPublicId,
            nickname: payload.requesterNickname || payload.requesterPublicId,
            friendCode: payload.requesterFriendCode || '',
            profileImg: payload.requesterProfileImg || null
        };
    }

    if (!friendProfile?.publicId) {
        return;
    }

    queryClient.setQueryData(['friends'], (prev = []) => {
        const prevList = Array.isArray(prev) ? prev : [];
        const alreadyExists = prevList.some(friend => friend.publicId === friendProfile.publicId);

        if (alreadyExists) {
            return prevList.map(friend => friend.publicId === friendProfile.publicId ? { ...friend, ...friendProfile } : friend);
        }

        return [friendProfile, ...prevList];
    });
}

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
            queryClient.invalidateQueries({ queryKey: ['receivedFriendRequests'] });
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
            queryClient.invalidateQueries({ queryKey: ['myAllRooms'] });
        },
    });
}
