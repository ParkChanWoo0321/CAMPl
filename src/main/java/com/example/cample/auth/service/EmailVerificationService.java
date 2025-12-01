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
        // 학교 메일 도메인 제한
        if (schoolEmailDomain != null && !schoolEmailDomain.isBlank()) {
            String dom = "@" + schoolEmailDomain.toLowerCase();
            if (!email.toLowerCase().endsWith(dom)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "학교 이메일만 사용 가능합니다.");
            }
        }

        // 6자리 코드 생성
        String code = String.format("%06d", new Random().nextInt(1_000_000));

        // DB 저장
        EmailVerification ev = EmailVerification.builder()
                .email(email)
                .code(code)
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(TTL_MINUTES))
                .build();
        repo.save(ev);

        // HTML 이메일 발송 (MailService가 HTML로 보내도록 구현되어 있어야 함)
        String subject = "[Cample] 이메일 인증코드 안내";
        String htmlBody = buildVerificationEmailHtml(code);

        // MailService.send(...)가 MimeMessageHelper#setText(htmlBody, true)로 구현되어 있어야 예쁘게 렌더링됨
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

    // 인증 메일 HTML 템플릿
    private String buildVerificationEmailHtml(String code) {
        return """
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8" />
  <title>Cample 이메일 인증</title>
  <style>
    body {
      margin: 0;
      padding: 0;
      background-color: #f4f4f5;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", system-ui, sans-serif;
    }
    .wrapper {
      padding: 32px 0;
    }
    .container {
      width: 480px;
      margin: 0 auto;
      background-color: #ffffff;
      border-radius: 16px;
      box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
      overflow: hidden;
    }
    .header {
      padding: 20px 24px;
      background: linear-gradient(135deg, #2563eb, #1d4ed8);
      color: #ffffff;
      font-weight: 700;
      font-size: 18px;
    }
    .header span.logo-mark {
      display: inline-block;
      margin-right: 8px;
      padding: 4px 10px;
      border-radius: 999px;
      background-color: rgba(15, 23, 42, 0.25);
      font-size: 12px;
      letter-spacing: 0.08em;
      text-transform: uppercase;
    }
    .content {
      padding: 24px 24px 8px 24px;
      color: #111827;
      font-size: 14px;
      line-height: 1.6;
    }
    .title {
      font-size: 18px;
      font-weight: 600;
      margin-bottom: 8px;
    }
    .subtitle {
      font-size: 13px;
      color: #6b7280;
      margin-bottom: 20px;
    }
    .code-box {
      margin: 16px 0 20px 0;
      padding: 16px 20px;
      background-color: #eff6ff;
      border-radius: 12px;
      border: 1px solid #bfdbfe;
      text-align: center;
    }
    .code-label {
      font-size: 11px;
      letter-spacing: 0.18em;
      text-transform: uppercase;
      color: #60a5fa;
      margin-bottom: 6px;
    }
    .code-value {
      display: inline-block;
      padding: 8px 18px;
      border-radius: 999px;
      background-color: #1d4ed8;
      color: #ffffff;
      font-size: 22px;
      letter-spacing: 0.35em;
      font-weight: 700;
    }
    .hint {
      font-size: 12px;
      color: #6b7280;
      margin-bottom: 18px;
    }
    .divider {
      border-top: 1px solid #e5e7eb;
      margin: 0 24px;
    }
    .footer {
      padding: 14px 24px 18px 24px;
      font-size: 11px;
      color: #9ca3af;
      line-height: 1.5;
    }
    .footer a {
      color: #6b7280;
      text-decoration: underline;
    }
  </style>
</head>
<body>
  <div class="wrapper">
    <div class="container">
      <div class="header">
        <span class="logo-mark">CAMPLE</span>
        <span>이메일 인증 안내</span>
      </div>
      <div class="content">
        <div class="title">이메일 주소 확인을 완료해 주세요.</div>
        <div class="subtitle">
          Cample 가입을 계속하려면 아래 인증 코드를 입력해 주세요.
        </div>

        <div class="code-box">
          <div class="code-label">verification code</div>
          <div class="code-value">%s</div>
        </div>

        <div class="hint">
          · 이 코드는 발송 시점 기준 약 %d분 동안만 유효합니다.<br/>
          · 인증 창에 위 숫자를 그대로 입력해 주세요.<br/>
          · 본인이 요청한 인증이 아니라면, 이 메일은 무시하셔도 됩니다.
        </div>
      </div>

      <div class="divider"></div>

      <div class="footer">
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
