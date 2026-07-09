package com.loopers.support.interceptor;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.loopers.domain.queue.QueueRepository;
import com.loopers.domain.user.UserModel;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 주문 생성(POST /api/v1/orders) 진입 시 입장 토큰을 검증하고,
 * 주문이 정상 완료되면 토큰을 삭제(소진)한다.
 * 조회(GET) 요청은 검증 대상이 아니다.
 */
@RequiredArgsConstructor
@Component
public class QueueTokenInterceptor implements HandlerInterceptor {

	public static final String QUEUE_TOKEN_HEADER = "X-Queue-Token";

	private final QueueRepository queueRepository;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (!HttpMethod.POST.matches(request.getMethod())) {
			return true;
		}

		UserModel user = (UserModel) request.getAttribute(AuthInterceptor.AUTHENTICATED_USER);
		if (user == null) {
			throw new CoreException(ErrorType.UNAUTHORIZED);
		}

		String token = request.getHeader(QUEUE_TOKEN_HEADER);
		if (!queueRepository.validateAdmissionToken(user.getId(), token)) {
			throw new CoreException(ErrorType.FORBIDDEN, "유효한 입장 토큰이 없습니다. 대기열을 통해 입장해주세요.");
		}

		return true;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
		if (!HttpMethod.POST.matches(request.getMethod())) {
			return;
		}
		if (ex != null || response.getStatus() != HttpStatus.OK.value()) {
			return;
		}

		UserModel user = (UserModel) request.getAttribute(AuthInterceptor.AUTHENTICATED_USER);
		if (user != null) {
			queueRepository.deleteAdmissionToken(user.getId());
		}
	}
}
