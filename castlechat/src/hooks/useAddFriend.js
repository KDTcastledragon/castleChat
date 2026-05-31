import { useMutation, useQueryClient } from '@tanstack/react-query';
import { addFriendApi } from '../api/friendApi';

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