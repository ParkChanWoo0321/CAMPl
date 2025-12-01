package com.example.cample.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from; // NAVER 계정과 동일하게 설정

    /**
     * html 파라미터에 HTML 문자열(템플릿)을 넣어서 호출하면 됨.
     */
    public void send(String to, String subject, String html) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true); // true = HTML 모드

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("메일 전송 중 오류가 발생했습니다.", e);
        }
    }
}
