import { useMutation, useQueryClient } from '@tanstack/react-query';
import { respondFriendRequestApi } from '../api/friendApi';

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