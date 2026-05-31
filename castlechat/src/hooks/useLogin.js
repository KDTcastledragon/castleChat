import { useMutation, useQueryClient } from '@tanstack/react-query';
import { loginApi } from '../api/userApi';

export function useLogin() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: loginApi,
        onSuccess: (me) => {
            queryClient.setQueryData(['me'], me);
        },
    });
}