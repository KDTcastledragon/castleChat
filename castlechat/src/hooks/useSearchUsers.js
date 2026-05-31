import { useQuery } from '@tanstack/react-query';
import { searchUsersApi } from '../api/userApi';

export function useSearchUsers(searchWord) {
    return useQuery({
        queryKey: ['searchUsers', searchWord],
        queryFn: () => searchUsersApi(searchWord),
        enabled: searchWord.trim().length > 0,
    });
}