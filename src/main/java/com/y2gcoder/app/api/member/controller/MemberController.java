package com.y2gcoder.app.api.member.controller;

import com.y2gcoder.app.domain.member.service.MemberService;
import com.y2gcoder.app.global.resolver.signinmember.SignInMember;
import com.y2gcoder.app.global.resolver.signinmember.SignInMemberDto;
import com.y2gcoder.app.global.util.RefreshTokenCookieUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/members")
@RestController
public class MemberController {

	private final MemberService memberService;
	private final RefreshTokenCookieUtils refreshTokenCookieUtils;

	@PreAuthorize("@memberGuard.check(#id)")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> withdrawMember(@PathVariable Long id, @SignInMember SignInMemberDto signInMemberDto) {
		//회원 삭제
		memberService.withdrawMember(id);

		//본인이라면 refresh token cookie도 삭제
		if (isOwnerMember(id, signInMemberDto.getMemberId())) {
			ResponseCookie signOutCookie = refreshTokenCookieUtils.generateSignOutCookie();
			return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, signOutCookie.toString()).build();
		}

		return ResponseEntity.ok().build();
	}

	private boolean isOwnerMember(Long memberId, Long signInMemberId) {
		return signInMemberId.equals(memberId);
	}

}
