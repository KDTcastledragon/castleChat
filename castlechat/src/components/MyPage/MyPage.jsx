import './MyPage.css';

import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useMe } from '../../hooks/useAuthUser';
import { changeMyPasswordApi, updateMyProfileApi } from '../../api/userApi';

function MyPage() {
    const queryClient = useQueryClient();
    const { data: me, isLoading } = useMe();

    const [nickname, setNickname] = useState('');
    const [profileImg, setProfileImg] = useState('');
    const [prevPw, setPrevPw] = useState('');
    const [newPw, setNewPw] = useState('');
    const [isPasswordBoxOpen, setIsPasswordBoxOpen] = useState(false);

    const updateProfileMutation = useMutation({
        mutationFn: updateMyProfileApi,
        onSuccess: (updatedMe) => {
            queryClient.setQueryData(['me'], updatedMe);
            setNickname('');
            setProfileImg('');
            alert('내 정보가 변경되었습니다.');
        }
    });

    const changePasswordMutation = useMutation({
        mutationFn: changeMyPasswordApi,
        onSuccess: () => {
            setPrevPw('');
            setNewPw('');
            setIsPasswordBoxOpen(false);
            alert('비밀번호가 변경되었습니다.');
        }
    });

    if (isLoading) {
        return <div className="myPageContainer">내 정보 불러오는 중...</div>;
    }

    if (!me) {
        return <div className="myPageContainer">로그인이 필요합니다.</div>;
    }

    const nextNickname = nickname.trim() || me.nickname;
    const nextProfileImg = profileImg.trim() || me.profileImg;

    function saveMyProfile() {
        updateProfileMutation.mutate({
            nickname: nextNickname,
            profileImg: nextProfileImg
        });
    }

    function changePassword() {
        if (!prevPw || !newPw) {
            alert('현재 비밀번호와 새 비밀번호를 입력해주세요.');
            return;
        }

        changePasswordMutation.mutate({
            prevPw,
            newPw
        });
    }

    return (
        <div className="myPageContainer">
            <section className="myPageCard">
                <div className="myPageHeader">
                    <img
                        className="myPageProfileImg"
                        src={nextProfileImg || '/images/mococo_question.png'}
                        alt={me.nickname}
                    />

                    <div className="myPageTitleBox">
                        <h2>내정보</h2>
                        <p>프로필과 기본 정보를 여기서 관리합니다.</p>
                    </div>
                </div>

                <div className="myInfoGrid">
                    <span>publicId</span>
                    <strong>{me.publicId}</strong>

                    <span>닉네임</span>
                    <strong>{me.nickname}</strong>

                    <span>친구코드</span>
                    <strong>{me.friendCode}</strong>

                    <span>프로필 이미지</span>
                    <strong>{me.profileImg || '기본 이미지'}</strong>
                </div>

                <div className="myEditSection">
                    <label>
                        닉네임 변경
                        <input
                            value={nickname}
                            onChange={(e) => setNickname(e.target.value)}
                            placeholder={me.nickname}
                        />
                    </label>

                    <label>
                        프로필 이미지 URL
                        <input
                            value={profileImg}
                            onChange={(e) => setProfileImg(e.target.value)}
                            placeholder={me.profileImg || '/images/mococo_question.png'}
                        />
                    </label>

                    <button
                        className="myPrimaryButton"
                        onClick={saveMyProfile}
                        disabled={updateProfileMutation.isPending}
                    >
                        프로필 저장
                    </button>
                </div>

                <div className="passwordSection">
                    <button
                        className="passwordToggleButton"
                        onClick={() => setIsPasswordBoxOpen(prev => !prev)}
                    >
                        비밀번호 변경하기
                    </button>

                    {isPasswordBoxOpen && (
                        <div className="passwordEditBox">
                            <input
                                type="password"
                                value={prevPw}
                                onChange={(e) => setPrevPw(e.target.value)}
                                placeholder="현재 비밀번호"
                            />

                            <input
                                type="password"
                                value={newPw}
                                onChange={(e) => setNewPw(e.target.value)}
                                placeholder="새 비밀번호"
                            />

                            <button
                                onClick={changePassword}
                                disabled={changePasswordMutation.isPending}
                            >
                                변경
                            </button>
                        </div>
                    )}
                </div>
            </section>
        </div>
    );
}

export default MyPage;
