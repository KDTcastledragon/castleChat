import { useQuery } from '@tanstack/react-query';
import { getFriendListApi } from '../api/friendApi';

export function useFriendList(enabled) {
    return useQuery({
        queryKey: ['friends'],
        queryFn: getFriendListApi,
        enabled,
        retry: false
    });
}