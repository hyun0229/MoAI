package com.foureyes.moai.backend.commons.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final com.foureyes.moai.backend.commons.mail.EmailTemplateProvider templateProvider;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * 입력: String to, EmailType type, String code
     * 출력: void
     * 기능: 인증 메일(회원가입/비밀번호 재설정) 발송
     **/
    public void sendVerificationEmail(String to, EmailType type, String code) {
        if (to == null || to.isBlank() || code == null || code.isBlank()) {
            log.error("이메일 주소 또는 코드가 비어 있습니다. to={}, code={}", to, code);
            throw new IllegalArgumentException("이메일 주소와 코드가 필요합니다.");
        }
        try {
            String subject = templateProvider.getSubject(type);

            String linkPath = switch (type) {
                case VERIFY -> "/users/verify-email";
                case PASSWORD_RESET -> "/users/reset-password/verify";
            };
            String verificationLink = String.format(
                "%s%s?email=%s&code=%s", baseUrl, linkPath, to, code
            );

            String body = templateProvider.getBody(type, verificationLink);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);

            mailSender.send(message);
            log.info("이메일 전송 성공: to={}, type={}, link={}", to, type, verificationLink);
        }catch (Exception e) {
            log.error("이메일 전송 실패 (로컬 데모라 무시하고 진행): to={}, type={}, error={}", to, type, e.getMessage());
        }

//        } catch (MessagingException e) {
//            log.error("이메일 전송 실패: to={}, type={}, error={}", to, type, e.getMessage());
//            throw new RuntimeException("이메일 전송에 실패했습니다.", e);
//        }
    }
}
