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
            setNickname('');
            alert('닉네임이 변경되었습니다.');
        },
        onError: (err) => {
            alert(err.response?.data ?? '닉네임 변경 실패');
        }
    });

    const updateProfileImageMutation = useMutation({
        mutationFn: updateMyProfileImageApi,
        onSuccess: (updatedMe) => {
            queryClient.setQueryData(['me'], updatedMe);
            emitWsProfileUpdated();
            setProfileImg('');
            setProfileImgFileName('');
            alert('프로필 이미지가 변경되었습니다.');
        },
        onError: (err) => {
            alert(err.response?.data ?? '프로필 이미지 변경 실패');
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

    function saveMyNickname() {
        const nextNickname = nickname.trim();

        if (!nextNickname) {
            alert('변경할 닉네임을 입력해주세요.');
            return;
        }

        updateNicknameMutation.mutate(nextNickname);
    }

    function saveMyProfileImage() {
        if (!profileImg) {
            alert('변경할 프로필 이미지를 선택해주세요.');
            return;
        }

        updateProfileImageMutation.mutate(profileImg);
    }

    function selectDefaultProfileImage() {
        setProfileImg(DEFAULT_PROFILE_IMAGE);
        setProfileImgFileName('mococo_question.png');
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
                            닉네임 변경
                            <input
                                value={nickname}
                                onChange={(e) => setNickname(e.target.value)}
                                placeholder={me.nickname}
                            />
                        </label>

                        <button
                            className="myPrimaryButton"
                            onClick={saveMyNickname}
                            disabled={updateNicknameMutation.isPending}
                        >
                            닉네임 변경
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
                            onClick={saveMyProfileImage}
                            disabled={updateProfileImageMutation.isPending || isUploadingProfileImg || !profileImg}
                        >
                            프로필 이미지 변경
                        </button>
                    </div>
                </div>

                <div className="passwordSection">
                    <button
                        className="passwordToggleButton"
                        onClick={() => setIsPasswordBoxOpen(true)}
                    >
                        비밀번호 변경하기
                    </button>

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
                                    <button type="button" className="passwordCancelButton" onClick={closePasswordModal}>
                                        취소
                                    </button>
                                    <button
                                        onClick={changePassword}
                                        disabled={changePasswordMutation.isPending}
                                    >
                                        변경
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
