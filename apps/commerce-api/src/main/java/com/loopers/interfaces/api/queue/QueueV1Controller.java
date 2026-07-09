package com.loopers.interfaces.api.queue;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.loopers.application.queue.QueueApplicationService;
import com.loopers.domain.user.UserModel;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.interceptor.AuthInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/queue")
public class QueueV1Controller {

	private final QueueApplicationService queueApplicationService;

	@PostMapping("/enter")
	public ApiResponse<QueueV1Dto.EnterResponse> enter(HttpServletRequest httpRequest) {
		UserModel user = (UserModel) httpRequest.getAttribute(AuthInterceptor.AUTHENTICATED_USER);

		return ApiResponse.success(
			QueueV1Dto.EnterResponse.from(
				queueApplicationService.enter(user.getId())
			)
		);
	}

	@GetMapping("/position")
	public ApiResponse<QueueV1Dto.PositionResponse> position(HttpServletRequest httpRequest) {
		UserModel user = (UserModel) httpRequest.getAttribute(AuthInterceptor.AUTHENTICATED_USER);

		return ApiResponse.success(
			QueueV1Dto.PositionResponse.from(
				queueApplicationService.getPosition(user.getId())
			)
		);
	}
}
