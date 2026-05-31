import { useQuery } from '@tanstack/react-query';
import { allUsersApi } from '../api/userApi';

export function useUsers(enabled) {
    return useQuery({
        queryKey: ['users'],
        queryFn: allUsersApi,
        enabled,
    });
}