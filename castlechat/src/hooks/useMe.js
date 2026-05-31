import { useQuery } from '@tanstack/react-query';
import { meApi } from '../api/userApi';

export function useMe() {
    return useQuery({
        queryKey: ['me'],
        queryFn: meApi,
        retry: false,
    });
}