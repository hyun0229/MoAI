package com.foureyes.moai.backend.domain.user.service;

import com.foureyes.moai.backend.domain.user.dto.request.PasswordChangeRequestDto;
import com.foureyes.moai.backend.domain.user.dto.request.UserLoginRequestDto;
import com.foureyes.moai.backend.domain.user.dto.request.UserProfileUpdateRequestDto;
import com.foureyes.moai.backend.domain.user.dto.request.UserSignupRequestDto;
import com.foureyes.moai.backend.domain.user.dto.response.UserLoginResponseDto;
import com.foureyes.moai.backend.domain.user.dto.response.UserProfileResponseDto;
import com.foureyes.moai.backend.domain.user.dto.response.UserSignupResponseDto;


public interface UserService {

    /**
     * 입력: UserSignupRequestDto request
     * 출력: UserSignupResponseDto
     * 기능: 회원가입 처리 후 이메일 인증 코드 생성 및 발송
     */
    UserSignupResponseDto signup(UserSignupRequestDto request);

    /**
     * 입력: UserLoginRequestDto request
     * 출력: UserLoginResponseDto(accessToken, refreshToken)
     * 기능: 로그인 처리(비밀번호 검증, 이메일 인증 여부 확인, 토큰 발급/저장)
     */
    UserLoginResponseDto login(UserLoginRequestDto request);

    /**
     * 입력: Integer userId
     * 출력: void
     * 기능: 로그아웃 처리(Refresh Token 제거)
     */
    void logout(Integer userId);

    /**
     * 입력: Integer userId
     * 출력: UserProfileResponseDto
     * 기능: 회원 정보 조회
     */
    UserProfileResponseDto getProfile(Integer userId);

    /**
     * 입력: String email
     * 출력: void
     * 기능: 이메일 인증 완료 처리(verified=true)
     */
    void markEmailAsVerified(String email);

    /**
     * 입력: String email
     * 출력: void
     * 기능: 비밀번호 재설정 요청(토큰 생성 후 이메일 발송)
     */
    void requestPasswordReset(String email);

    /**
     * 입력: String email, String token
     * 출력: void
     * 기능: 비밀번호 재설정 토큰 검증(리셋 페이지 접근 판단)
     */
    void verifyPasswordResetToken(String email, String token);

    /**
     * 입력: String email, String token, String newPassword
     * 출력: void
     * 기능: 이메일 인증 후 비밀번호 재설정(토큰 1회성 사용 보장)
     */
    void resetPassword(String email, String token, String newPassword);

    /**
     * 입력: Integer userId, PasswordChangeRequestDto request
     * 출력: UserLoginResponseDto(newAccess, newRefresh)
     * 기능: 로그인 상태에서 비밀번호 변경 및 토큰 재발급
     */
    UserLoginResponseDto changePassword(Integer userId, PasswordChangeRequestDto request);

    /**
     * 입력: Integer userId
     * 출력: void
     * 기능: 회원 탈퇴(물리 삭제)
     */
    void deleteUserById(Integer userId);

    /**
     * 입력: Integer userId, UserProfileUpdateRequestDto request
     * 출력: UserProfileResponseDto
     * 기능: 회원 정보 수정(이름/프로필 이미지)
     */
    UserProfileResponseDto updateUserProfile(Integer userId, UserProfileUpdateRequestDto request);

    /**
     * 입력: String refreshToken
     * 출력: UserLoginResponseDto
     * 기능: 유효한 Refresh Token으로 Access/Refresh Token을 재발급
     */
    UserLoginResponseDto refresh(String refreshToken);
}
