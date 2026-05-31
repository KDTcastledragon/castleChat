import { useMutation, useQueryClient } from '@tanstack/react-query';
import { logoutApi } from '../api/userApi';

export function useLogout() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: logoutApi,
        onSuccess: () => {
            queryClient.setQueryData(['me'], null);
            queryClient.removeQueries({ queryKey: ['users'] });
        },
    });
}