import { useQuery } from '@tanstack/react-query';
import { getReceivedFriendRequestsApi } from '../api/friendApi';

export function useReceivedFriendRequests(enabled) {
    return useQuery({
        queryKey: ['receivedFriendRequests'],
        queryFn: getReceivedFriendRequestsApi,
        enabled,
    });
}