import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import axios from 'axios';

export function useAuthErrorInterceptor() {
    const navigator = useNavigate();
    const queryClient = useQueryClient();

    useEffect(() => {
        const interceptorId = axios.interceptors.response.use(
            (response) => response,
            (error) => {
                if (error.response?.status === 401) {
                    queryClient.setQueryData(['me'], null);

                    queryClient.removeQueries({ queryKey: ['friends'] });
                    queryClient.removeQueries({ queryKey: ['receivedFriendRequests'] });
                    queryClient.removeQueries({ queryKey: ['searchUsers'] });
                    queryClient.removeQueries({ queryKey: ['users'] });

                    navigator('/login', { replace: true });
                }

                return Promise.reject(error);
            }
        );

        return () => {
            axios.interceptors.response.eject(interceptorId);
        };
    }, [navigator, queryClient]);
}