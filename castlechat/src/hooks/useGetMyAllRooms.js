import { useQuery } from '@tanstack/react-query';
import { getMyAllRoomsApi } from '../api/chatApi';

export function useGetMyAllRooms(enabled) {
    return useQuery({
        queryKey: ['myAllRooms'],
        queryFn: getMyAllRoomsApi,
        enabled,
        retry: false
    });
}