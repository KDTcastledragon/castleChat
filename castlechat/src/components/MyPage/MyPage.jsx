import './MyPage.css';

import { useRef, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useMe } from '../../hooks/useAuthUser';
import { changeMyPasswordApi, updateMyNicknameApi, updateMyProfileImageApi } from '../../api/userApi';
import { uploadImageApi } from '../../api/chatApi';
import { emitWsProfileUpdated } from '../../webSocket/wsClient';

const DEFAULT_PROFILE_IMAGE = '/images/mococo_question.png';

function getProfileImageFileName(profileImgUrl) {
    if (!profileImgUrl) return 'mococo_question.png';

    const urlWithoutQuery = profileImgUrl.split('?')[0].split('#')[0];
    const fileName = urlWithoutQuery.substring(urlWithoutQuery.lastIndexOf('/') + 1);

    try {
        return decodeURIComponent(fileName || 'mococo_question.png');
    } catch {
        return fileName || 'mococo_question.png';
    }
}

function MyPage() {
    const queryClient = useQueryClient();
    const { data: me, isLoading } = useMe();
    const profileImgInputRef = useRef(null);

    const [nickname, setNickname] = useState('');
    const [isNicknameEditing, setIsNicknameEditing] = useState(false);
    const [profileImg, setProfileImg] = useState('');
    const [profileImgFileName, setProfileImgFileName] = useState('');
    const [isUploadingProfileImg, setIsUploadingProfileImg] = useState(false);
    const [prevPw, setPrevPw] = useState('');
    const [newPw, setNewPw] = useState('');
    const [newPwConfirm, setNewPwConfirm] = useState('');
    const [isPasswordBoxOpen, setIsPasswordBoxOpen] = useState(false);

    const updateNicknameMutation = useMutation({
        mutationFn: updateMyNicknameApi,
        onSuccess: (updatedMe) => {
            queryClient.setQueryData(['me'], updatedMe);
            emitWsProfileUpdated();
        }
    });

    const updateProfileImageMutation = useMutation({
        mutationFn: updateMyProfileImageApi,
        onSuccess: (updatedMe) => {
            queryClient.setQueryData(['me'], updatedMe);
            emitWsProfileUpdated();
        }
    });

    const changePasswordMutation = useMutation({
        mutationFn: changeMyPasswordApi,
        onSuccess: () => {
            closePasswordModal();
            alert('비밀번호가 변경되었습니다.');
        },
        onError: (err) => {
            alert(err.response?.data ?? '비밀번호 변경 실패');
        }
    });

    if (isLoading) {
        return <div className="myPageContainer">내 정보 불러오는 중...</div>;
    }

    if (!me) {
        return <div className="myPageContainer">로그인이 필요합니다.</div>;
    }

    const nextProfileImg = profileImg || me.profileImg || DEFAULT_PROFILE_IMAGE;
    const currentProfileImageFileName = getProfileImageFileName(me.profileImg || DEFAULT_PROFILE_IMAGE);
    const selectedProfileImageFileName = profileImgFileName || getProfileImageFileName(nextProfileImg);

    // 닉네임/프로필 중 하나라도 실제로 바뀌어야 저장 버튼이 활성화된다.
    const isNicknameChanged = isNicknameEditing && nickname.trim().length > 0 && nickname.trim() !== me.nickname;
    const isProfileImgChanged = !!profileImg && profileImg !== (me.profileImg || DEFAULT_PROFILE_IMAGE);
    const isSavingProfile = updateNicknameMutation.isPending || updateProfileImageMutation.isPending;

    function toggleNicknameEditing() {
        setIsNicknameEditing(prev => {
            const next = !prev;
            setNickname(next ? me.nickname : '');
            return next;
        });
    }

    // 닉네임 + 프로필 이미지 통합 저장.
    async function saveMyProfile() {
        try {
            if (isNicknameChanged) {
                await updateNicknameMutation.mutateAsync(nickname.trim());
            }

            if (isProfileImgChanged) {
                await updateProfileImageMutation.mutateAsync(profileImg);
            }

            setIsNicknameEditing(false);
            setNickname('');
            setProfileImg('');
            setProfileImgFileName('');
            alert('프로필이 저장되었습니다.');
        } catch (err) {
            alert(err.response?.data ?? '프로필 저장 실패');
        }
    }

    function selectDefaultProfileImage() {
        setProfileImg(DEFAULT_PROFILE_IMAGE);
        setProfileImgFileName('mococo_question.png');
    }

    // 원래대로: 선택했던 이미지 초안을 버리고 기존 프로필 이미지로 되돌린다.
    function revertProfileImage() {
        setProfileImg('');
        setProfileImgFileName('');
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
        if (!prevPw || !newPw || !newPwConfirm) {
            alert('비밀번호 입력칸을 모두 작성해주세요.');
            return;
        }

        if (newPw !== newPwConfirm) {
            alert('변경 비밀번호가 일치하지 않습니다.');
            return;
        }

        changePasswordMutation.mutate({
            prevPw,
            newPw
        });
    }

    function closePasswordModal() {
        setPrevPw('');
        setNewPw('');
        setNewPwConfirm('');
        setIsPasswordBoxOpen(false);
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
                    </div>

                    <button
                        className="passwordToggleButton"
                        onClick={() => setIsPasswordBoxOpen(true)}
                    >
                        비밀번호 변경하기
                    </button>
                </div>

                <div className="myInfoGrid">
                    <span>닉네임</span>
                    <strong>{me.nickname}</strong>

                    <span>친구코드</span>
                    <strong>{me.friendCode}</strong>

                    <span>프로필 이미지</span>
                    <strong>{currentProfileImageFileName}</strong>
                </div>

                <div className="myEditSection">
                    <div className="myEditRow">
                        <label>
                            닉네임
                            <input
                                value={isNicknameEditing ? nickname : me.nickname}
                                onChange={(e) => setNickname(e.target.value)}
                                disabled={!isNicknameEditing}
                            />
                        </label>

                        <button
                            className="myPrimaryButton"
                            onClick={toggleNicknameEditing}
                        >
                            {isNicknameEditing ? '변경 취소' : '닉네임 변경하기'}
                        </button>
                    </div>

                    <div className="myEditRow profileImageEditRow">
                        <div className="profileImageEditBox">
                            <span>프로필 이미지</span>
                            <strong title={selectedProfileImageFileName}>{selectedProfileImageFileName}</strong>

                            <button
                                type="button"
                                onClick={() => profileImgInputRef.current?.click()}
                                disabled={isUploadingProfileImg}
                            >
                                {isUploadingProfileImg ? '업로드 중...' : '파일 선택'}
                            </button>

                            <button
                                type="button"
                                className="defaultProfileImageButton"
                                onClick={selectDefaultProfileImage}
                                disabled={isUploadingProfileImg}
                            >
                                기본 이미지
                            </button>

                            <button
                                type="button"
                                className="revertProfileImageButton"
                                onClick={revertProfileImage}
                                disabled={isUploadingProfileImg || !profileImg}
                            >
                                원래대로
                            </button>

                            <input
                                ref={profileImgInputRef}
                                type="file"
                                accept="image/*"
                                hidden
                                onChange={changeProfileImage}
                            />
                        </div>
                    </div>
                </div>

                <div className="mySaveSection">
                    <button
                        className="myPrimaryButton profileSaveButton"
                        onClick={saveMyProfile}
                        disabled={isSavingProfile || isUploadingProfileImg || (!isNicknameChanged && !isProfileImgChanged)}
                    >
                        {isSavingProfile ? '저장 중...' : '프로필 저장'}
                    </button>
                </div>

                <div className="passwordSection">
                    {isPasswordBoxOpen && (
                        <div className="passwordModalOverlay">
                            <div className="passwordEditBox" onMouseDown={(e) => e.stopPropagation()}>
                                <div className="passwordModalHeader">
                                    <strong>비밀번호 변경</strong>
                                    <button type="button" onClick={closePasswordModal}>×</button>
                                </div>

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

                                <input
                                    type="password"
                                    value={newPwConfirm}
                                    onChange={(e) => setNewPwConfirm(e.target.value)}
                                    placeholder="새 비밀번호 확인"
                                />

                                <div className="passwordModalActions">
                                    <button
                                        onClick={changePassword}
                                        disabled={changePasswordMutation.isPending}
                                    >
                                        변경
                                    </button>
                                    <button type="button" className="passwordCancelButton" onClick={closePasswordModal}>
                                        취소
                                    </button>
                                </div>
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}

export default MyPage;
