package com.example.cample.auth.service;

import com.example.cample.auth.domain.EmailVerification;
import com.example.cample.auth.domain.VerificationPurpose;
import com.example.cample.auth.repo.EmailVerificationRepository;
import com.example.cample.common.exception.ApiException;
import com.example.cample.mail.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final int TTL_MINUTES = 10;

    private final EmailVerificationRepository repo;
    private final MailService mail;

    @Value("${app.school-email-domain:}")
    private String schoolEmailDomain;

    public void sendCode(String email, VerificationPurpose purpose) {
        if (schoolEmailDomain != null && !schoolEmailDomain.isBlank()) {
            String dom = "@" + schoolEmailDomain.toLowerCase();
            if (!email.toLowerCase().endsWith(dom)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "학교 이메일만 사용 가능합니다.");
            }
        }

        String code = String.format("%06d", new Random().nextInt(1_000_000));

        EmailVerification ev = EmailVerification.builder()
                .email(email)
                .code(code)
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(TTL_MINUTES))
                .build();
        repo.save(ev);

        String subject = "[Cample] 이메일 인증코드 안내";
        String htmlBody = buildVerificationEmailHtml(code);

        mail.send(email, subject, htmlBody);
    }

    public void verify(String email, VerificationPurpose purpose, String code) {
        EmailVerification ev = repo.findTopByEmailAndPurposeOrderByIdDesc(email, purpose)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "코드를 먼저 발급하세요."));
        if (ev.getVerifiedAt() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "이미 사용된 코드입니다.");
        }
        if (LocalDateTime.now().isAfter(ev.getExpiresAt())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "코드가 만료되었습니다.");
        }
        if (!ev.getCode().equals(code)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "코드가 일치하지 않습니다.");
        }
        ev.setVerifiedAt(LocalDateTime.now());
        repo.save(ev);
    }

    // 파란색 → 보라색, VERIFICATION CODE 제거, 코드 pill 정렬 수정
    private String buildVerificationEmailHtml(String code) {
        return """
<!DOCTYPE html>
<html lang="ko">
  <body style="margin:0;padding:0;background-color:#f4f4f5;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',system-ui,sans-serif;">
    <div style="padding:32px 0;">
      <div style="max-width:480px;width:100%%;margin:0 auto;background-color:#ffffff;border-radius:16px;box-shadow:0 10px 30px rgba(15,23,42,0.08);overflow:hidden;">
        
        <!-- 헤더 (보라색 그라데이션) -->
        <div style="padding:20px 24px;background:linear-gradient(135deg,#c084fc,#a855f7);color:#ffffff;font-weight:700;font-size:18px;">
          <span style="display:inline-block;margin-right:8px;padding:4px 10px;border-radius:999px;background-color:rgba(15,23,42,0.25);font-size:12px;letter-spacing:0.08em;text-transform:uppercase;">
            CAMPLE
          </span>
          <span>이메일 인증 안내</span>
        </div>

        <!-- 본문 -->
        <div style="padding:24px 24px 8px 24px;color:#111827;font-size:14px;line-height:1.6;">
          <div style="font-size:18px;font-weight:600;margin-bottom:8px;">
            이메일 주소 확인을 완료해 주세요.
          </div>
          <div style="font-size:13px;color:#6b7280;margin-bottom:20px;">
            Cample 가입을 계속하려면 아래 인증 코드를 입력해 주세요.
          </div>

          <!-- 코드 박스 (보라색 톤) -->
          <div style="margin:16px 0 20px 0;padding:16px 20px;background-color:#f5efff;border-radius:12px;border:1px solid #ddd6fe;text-align:center;">
            <div style="display:inline-block;min-width:220px;padding:10px 24px;border-radius:999px;background-color:#a855f7;color:#ffffff;font-size:22px;font-weight:700;text-align:center;">
              %s
            </div>
          </div>

          <!-- 안내 문구 -->
          <div style="font-size:12px;color:#6b7280;margin-bottom:18px;">
            · 이 코드는 발송 시점 기준 약 %d분 동안만 유효합니다.<br/>
            · 인증 창에 위 숫자를 그대로 입력해 주세요.<br/>
            · 본인이 요청한 인증이 아니라면, 이 메일은 무시하셔도 됩니다.
          </div>
        </div>

        <!-- 구분선 -->
        <div style="border-top:1px solid #e5e7eb;margin:0 24px;"></div>

        <!-- 푸터 -->
        <div style="padding:14px 24px 18px 24px;font-size:11px;color:#9ca3af;line-height:1.5;">
          본 메일은 발신 전용입니다. 궁금한 점이 있다면 Cample 내 문의 기능을 이용해 주세요.<br/>
          &copy; Cample. All rights reserved.
        </div>
      </div>
    </div>
  </body>
</html>
""".formatted(code, TTL_MINUTES);
    }
}
