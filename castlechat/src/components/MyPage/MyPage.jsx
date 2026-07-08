import './MyPage.css';

import { useRef, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useMe } from '../../hooks/useAuthUser';
import { changeMyPasswordApi, updateMyProfileApi } from '../../api/userApi';
import { uploadImageApi } from '../../api/chatApi';

function MyPage() {
    const queryClient = useQueryClient();
    const { data: me, isLoading } = useMe();
    const profileImgInputRef = useRef(null);

    const [nickname, setNickname] = useState('');
    const [profileImg, setProfileImg] = useState('');
    const [profileImgFileName, setProfileImgFileName] = useState('');
    const [isUploadingProfileImg, setIsUploadingProfileImg] = useState(false);
    const [prevPw, setPrevPw] = useState('');
    const [newPw, setNewPw] = useState('');
    const [isPasswordBoxOpen, setIsPasswordBoxOpen] = useState(false);

    const updateProfileMutation = useMutation({
        mutationFn: updateMyProfileApi,
        onSuccess: (updatedMe) => {
            queryClient.setQueryData(['me'], updatedMe);
            setNickname('');
            setProfileImg('');
            setProfileImgFileName('');
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

    async function changeProfileImage(e) {
        const file = e.target.files?.[0];
        e.target.value = '';

        if (!file) {
            return;
        }

        if (!file.type.startsWith('image/')) {
            alert('이미지 파일만 선택할 수 있습니다.');
            return;
        }

        try {
            setIsUploadingProfileImg(true);
            const uploadedImageUrl = await uploadImageApi(file, 'PROFILE_IMAGE');

            if (!uploadedImageUrl) {
                throw new Error('업로드된 이미지 주소가 없습니다.');
            }

            setProfileImg(uploadedImageUrl);
            setProfileImgFileName(file.name);
        } catch (err) {
            alert(err.message || '프로필 이미지 업로드 실패');
        } finally {
            setIsUploadingProfileImg(false);
        }
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
            <div className="myPageCard">
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

                    <div className="profileImageEditBox">
                        <span>프로필 이미지</span>
                        <strong>{profileImgFileName || (profileImg ? '새 이미지 선택됨' : '현재 이미지 유지')}</strong>

                        <button
                            type="button"
                            onClick={() => profileImgInputRef.current?.click()}
                            disabled={isUploadingProfileImg}
                        >
                            {isUploadingProfileImg ? '업로드 중...' : '파일 선택'}
                        </button>

                        <input
                            ref={profileImgInputRef}
                            type="file"
                            accept="image/*"
                            hidden
                            onChange={changeProfileImage}
                        />
                    </div>

                    <button
                        className="myPrimaryButton"
                        onClick={saveMyProfile}
                        disabled={updateProfileMutation.isPending || isUploadingProfileImg}
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
            </div>
        </div>
    );
}

export default MyPage;
