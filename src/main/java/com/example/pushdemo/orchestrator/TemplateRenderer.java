package com.example.pushdemo.orchestrator;

import com.example.pushdemo.common.NotificationType;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 알림 타입 + 언어 → (title, body) 렌더링.
 *
 * <p>실서비스라면 i18n 메시지 번들 또는 Mustache/Thymeleaf 같은 템플릿 엔진을 쓰는 게 보통.
 * 여기서는 학습용으로 switch + 문자열 결합. 의도적으로 단순.
 */
@Component
public class TemplateRenderer {

    public record Rendered(String title, String body) {}

    public Rendered render(NotificationType type, String language, Map<String, String> data) {
        Map<String, String> safe = data == null ? Map.of() : data;
        boolean ko = !"en".equalsIgnoreCase(language);
        return switch (type) {
            case PAYMENT_COMPLETED -> ko
                    ? new Rendered("결제 완료", "주문 " + safe.getOrDefault("orderId", "?") + " 결제가 완료되었습니다.")
                    : new Rendered("Payment complete", "Order " + safe.getOrDefault("orderId", "?") + " has been paid.");
            case FRIEND_REQUEST -> ko
                    ? new Rendered("친구 요청", safe.getOrDefault("fromName", "누군가") + "님이 친구 요청을 보냈어요.")
                    : new Rendered("Friend request", safe.getOrDefault("fromName", "Someone") + " sent you a friend request.");
            case MARKETING_PROMO -> ko
                    ? new Rendered("프로모션", "오늘만 특가! " + safe.getOrDefault("promo", "할인 행사 진행중"))
                    : new Rendered("Promo", "Today only! " + safe.getOrDefault("promo", "Sale running now"));
        };
    }
}
