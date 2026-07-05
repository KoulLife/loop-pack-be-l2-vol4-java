package com.loopers.domain.like;

public record LikeChangedEvent(
	Long userId,
	Long productId,
	Type type
) {
	public enum Type {
		LIKED,
		UNLIKED
	}
}
