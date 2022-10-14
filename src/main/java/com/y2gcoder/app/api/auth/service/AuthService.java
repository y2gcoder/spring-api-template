package com.y2gcoder.app.api.auth.service;

import com.y2gcoder.app.api.auth.service.dto.SignInDto;
import com.y2gcoder.app.api.auth.service.dto.SignUpRequest;
import com.y2gcoder.app.domain.member.constant.AuthProvider;
import com.y2gcoder.app.domain.member.constant.MemberRole;
import com.y2gcoder.app.domain.member.entity.Member;
import com.y2gcoder.app.domain.member.service.MemberService;
import com.y2gcoder.app.global.config.jwt.dto.JwtTokenDto;
import com.y2gcoder.app.global.config.jwt.service.JwtTokenProvider;
import com.y2gcoder.app.global.error.ErrorCode;
import com.y2gcoder.app.global.error.exception.AuthenticationException;
import com.y2gcoder.app.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class AuthService {
	private final JwtTokenProvider jwtTokenProvider;
	private final MemberService memberService;
	private final PasswordEncoder passwordEncoder;

	@Transactional
	public JwtTokenDto refreshToken(String refreshToken) {

		validateRefreshToken(refreshToken);
		Member member = memberService.findMemberByRefreshToken(refreshToken);

		JwtTokenDto jwtTokenDto = jwtTokenProvider.createJwtToken(String.valueOf(member.getId()), member.getRole());

		member.updateRefreshToken(jwtTokenDto.getRefreshToken(), jwtTokenDto.getRefreshTokenExpireTime());

		return jwtTokenDto;
	}

	@Transactional
	public void signOut(String refreshToken) {

		validateRefreshToken(refreshToken);
		Member member = memberService.findMemberByRefreshToken(refreshToken);

		member.updateRefreshToken("", LocalDateTime.now());
	}

	private void validateRefreshToken(String refreshToken) {
		boolean validateToken = jwtTokenProvider.validateToken(refreshToken);
		if (!validateToken) {
			throw new AuthenticationException(ErrorCode.INVALID_REFRESH_TOKEN);
		}
	}

	@Transactional
	public void signUp(SignUpRequest request) {
		validateSignUpInfo(request);
		Member member = Member.builder()
				.email(request.getEmail())
				.password(passwordEncoder.encode(request.getPassword()))
				.role(MemberRole.USER)
				.provider(AuthProvider.local)
				.build();
		memberService.registerMember(member);
	}

	private void validateSignUpInfo(SignUpRequest request) {
		if (memberService.existsMemberByEmail(request.getEmail())) {
			throw new BusinessException(ErrorCode.ALREADY_REGISTERED_MEMBER);
		}
	}

	@Transactional
	public JwtTokenDto signIn(SignInDto.Request request) {
		Member member = memberService.findMemberByEmail(request.getEmail());
		validatePassword(request.getPassword(), member.getPassword());
		// 토큰 만들기(access, refresh)
		JwtTokenDto jwtTokenDto = jwtTokenProvider.createJwtToken(String.valueOf(member.getId()), member.getRole());
		// refresh token 저장 (DB)
		memberService.updateRefreshToken(
				member.getId(),
				jwtTokenDto.getRefreshToken(),
				jwtTokenDto.getRefreshTokenExpireTime()
		);

		return jwtTokenDto;
	}

	private void validatePassword(String requestPassword, String memberPassword) {
		if (!passwordEncoder.matches(requestPassword, memberPassword)) {
			throw new AuthenticationException(ErrorCode.MISMATCH_PASSWORD);
		}
	}
}