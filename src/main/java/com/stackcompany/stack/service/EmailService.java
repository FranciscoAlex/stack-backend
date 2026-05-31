package com.stackcompany.stack.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${app.url:https://stakesomosholdrs.com}")
    private String appUrl;
    
    @Value("${spring.mail.from:stakeholders921@gmail.com}")
    private String fromEmail;
    
    @Value("${spring.mail.from.name:StackSupport}")
    private String fromName;
    
    public void sendActivationEmail(String toEmail, String firstName, String activationToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Bem-vindo! Ative sua conta");
            
            // URL encode the token to handle special characters, though UUID shouldn't have any
            String encodedToken = java.net.URLEncoder.encode(activationToken, "UTF-8");
            String activationUrl = appUrl + "/activate?token=" + encodedToken;
            String emailBody = buildActivationEmailTemplate(firstName, activationUrl);
            
            helper.setText(emailBody, true);
            mailSender.send(message);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to send activation email: " + e.getMessage(), e);
        }
    }
    
    public void sendPasswordResetEmail(String toEmail, String firstName, String resetToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Solicitação de Redefinição de Senha");
            
            String resetUrl = appUrl + "/reset-password?token=" + resetToken;
            String emailBody = buildPasswordResetEmailTemplate(firstName, resetUrl);
            
            helper.setText(emailBody, true);
            mailSender.send(message);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to send password reset email: " + e.getMessage(), e);
        }
    }
    
    public void sendWelcomeEmail(String toEmail, String firstName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Bem-vindo ao Stack!");
            
            String emailBody = buildWelcomeEmailTemplate(firstName);
            
            helper.setText(emailBody, true);
            mailSender.send(message);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to send welcome email: " + e.getMessage(), e);
        }
    }
    
    private String buildActivationEmailTemplate(String firstName, String activationUrl) {
        String name = firstName != null && !firstName.isEmpty() ? firstName : "Usuário";
        String logoUrl = appUrl + "/Icon.png";
        
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; background-color: #f4f4f4; }" +
                ".container { max-width: 600px; margin: 0 auto; background-color: #ffffff; padding: 20px; }" +
                ".header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }" +
                ".logo { max-width: 120px; height: auto; margin-bottom: 15px; }" +
                ".content { padding: 30px 20px; }" +
                ".button { display: inline-block; padding: 12px 30px; background-color: #4CAF50; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }" +
                ".button:hover { background-color: #45a049; }" +
                ".footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<img src='" + logoUrl + "' alt='Stack Logo' class='logo' />" +
                "<h1>Bem-vindo ao Stack!</h1>" +
                "</div>" +
                "<div class='content'>" +
                "<h2>Olá " + name + "!</h2>" +
                "<p>Obrigado por criar uma conta conosco. Estamos animados em tê-lo a bordo!</p>" +
                "<p>Para completar seu registro e ativar sua conta, clique no botão abaixo:</p>" +
                "<div style='text-align: center;'>" +
                "<a href='" + activationUrl + "' class='button'>Ativar Conta</a>" +
                "</div>" +
                "<p>Ou copie e cole este link no seu navegador:</p>" +
                "<p style='word-break: break-all; color: #4CAF50;'>" + activationUrl + "</p>" +
                "<p>Este link de ativação expirará em 24 horas.</p>" +
                "<p>Se você não criou esta conta, por favor ignore este email.</p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>Atenciosamente,<br>Equipe Stack</p>" +
                "<p>Este é um email automatizado, por favor não responda.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
    
    private String buildPasswordResetEmailTemplate(String firstName, String resetUrl) {
        String name = firstName != null && !firstName.isEmpty() ? firstName : "Usuário";
        String logoUrl = appUrl + "/Icon.png";
        
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; background-color: #f4f4f4; }" +
                ".container { max-width: 600px; margin: 0 auto; background-color: #ffffff; padding: 20px; }" +
                ".header { background-color: #2196F3; color: white; padding: 20px; text-align: center; }" +
                ".logo { max-width: 120px; height: auto; margin-bottom: 15px; }" +
                ".content { padding: 30px 20px; }" +
                ".button { display: inline-block; padding: 12px 30px; background-color: #2196F3; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }" +
                ".button:hover { background-color: #1976D2; }" +
                ".footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }" +
                ".warning { background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 10px; margin: 20px 0; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<img src='" + logoUrl + "' alt='Stack Logo' class='logo' />" +
                "<h1>Solicitação de Redefinição de Senha</h1>" +
                "</div>" +
                "<div class='content'>" +
                "<h2>Olá " + name + "!</h2>" +
                "<p>Recebemos uma solicitação para redefinir sua senha. Se você fez esta solicitação, clique no botão abaixo para redefinir sua senha:</p>" +
                "<div style='text-align: center;'>" +
                "<a href='" + resetUrl + "' class='button'>Redefinir Senha</a>" +
                "</div>" +
                "<p>Ou copie e cole este link no seu navegador:</p>" +
                "<p style='word-break: break-all; color: #2196F3;'>" + resetUrl + "</p>" +
                "<div class='warning'>" +
                "<p><strong>Importante:</strong> Este link de redefinição de senha expirará em 1 hora.</p>" +
                "</div>" +
                "<p>Se você não solicitou uma redefinição de senha, por favor ignore este email. Sua senha permanecerá inalterada.</p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>Atenciosamente,<br>Equipe Stack</p>" +
                "<p>Este é um email automatizado, por favor não responda.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
    
    private String buildWelcomeEmailTemplate(String firstName) {
        String name = firstName != null && !firstName.isEmpty() ? firstName : "Usuário";
        String logoUrl = appUrl + "/Icon.png";
        
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; background-color: #f4f4f4; }" +
                ".container { max-width: 600px; margin: 0 auto; background-color: #ffffff; padding: 20px; }" +
                ".header { background-color: #0A2463; color: white; padding: 20px; text-align: center; }" +
                ".logo { max-width: 120px; height: auto; margin-bottom: 15px; }" +
                ".content { padding: 30px 20px; }" +
                ".button { display: inline-block; padding: 12px 30px; background-color: #0A2463; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }" +
                ".button:hover { background-color: #061842; }" +
                ".footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<img src='" + logoUrl + "' alt='Stack Logo' class='logo' />" +
                "<h1>Bem-vindo ao Stack!</h1>" +
                "</div>" +
                "<div class='content'>" +
                "<h2>Olá " + name + "!</h2>" +
                "<p>Obrigado por se juntar ao Stack! Estamos animados em tê-lo como parte da nossa comunidade.</p>" +
                "<p>Sua conta foi criada e ativada com sucesso. Agora você pode:</p>" +
                "<ul>" +
                "<li>Compartilhar posts e atualizações com a comunidade</li>" +
                "<li>Conectar-se com outros profissionais</li>" +
                "<li>Descobrir insights e conteúdo valiosos</li>" +
                "<li>Construir sua rede profissional</li>" +
                "</ul>" +
                "<div style='text-align: center;'>" +
                "<a href='" + appUrl + "' class='button'>Começar</a>" +
                "</div>" +
                "<p>Se você tiver alguma dúvida ou precisar de assistência, sinta-se à vontade para entrar em contato com nossa equipe de suporte.</p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>Atenciosamente,<br>Equipe Stack</p>" +
                "<p>Este é um email automatizado, por favor não responda.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
    
    public void sendPostRejectionEmail(String toEmail, String firstName, String postContent, String reason) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Seu Post Foi Rejeitado");
            
            String emailBody = buildPostRejectionEmailTemplate(firstName, postContent, reason);
            
            helper.setText(emailBody, true);
            mailSender.send(message);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to send post rejection email: " + e.getMessage(), e);
        }
    }
    
    private String buildPostRejectionEmailTemplate(String firstName, String postContent, String reason) {
        String name = firstName != null && !firstName.isEmpty() ? firstName : "Usuário";
        String displayReason = reason != null && !reason.isEmpty() ? reason : "Não atende às nossas diretrizes da comunidade.";
        String logoUrl = appUrl + "/Icon.png";
        
        // Truncate post content if too long
        String displayContent = postContent != null && postContent.length() > 200 
            ? postContent.substring(0, 200) + "..." 
            : postContent;
        
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; background-color: #f4f4f4; }" +
                ".container { max-width: 600px; margin: 0 auto; background-color: #ffffff; padding: 20px; }" +
                ".header { background-color: #d32f2f; color: white; padding: 20px; text-align: center; }" +
                ".logo { max-width: 120px; height: auto; margin-bottom: 15px; }" +
                ".content { padding: 30px 20px; }" +
                ".post-content { background-color: #f5f5f5; padding: 15px; border-left: 4px solid #d32f2f; margin: 20px 0; }" +
                ".reason-box { background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }" +
                ".button { display: inline-block; padding: 12px 30px; background-color: #0A2463; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }" +
                ".button:hover { background-color: #061842; }" +
                ".footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<img src='" + logoUrl + "' alt='Stack Logo' class='logo' />" +
                "<h1>Aviso de Rejeição de Post</h1>" +
                "</div>" +
                "<div class='content'>" +
                "<h2>Olá " + name + "!</h2>" +
                "<p>Lamentamos informar que seu post recente foi rejeitado pela nossa equipe de moderação.</p>" +
                "<div class='post-content'>" +
                "<p><strong>Seu Post:</strong></p>" +
                "<p>" + (displayContent != null ? displayContent.replace("\n", "<br>") : "N/A") + "</p>" +
                "</div>" +
                "<div class='reason-box'>" +
                "<p><strong>Motivo da Rejeição:</strong></p>" +
                "<p>" + displayReason.replace("\n", "<br>") + "</p>" +
                "</div>" +
                "<p>Encorajamos você a revisar nossas diretrizes da comunidade e enviar um novo post que esteja em conformidade com nossos padrões.</p>" +
                "<div style='text-align: center;'>" +
                "<a href='" + appUrl + "' class='button'>Visitar Stack</a>" +
                "</div>" +
                "<p>Se você tiver alguma dúvida ou acreditar que isso foi um erro, entre em contato com nossa equipe de suporte.</p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>Atenciosamente,<br>Equipe Stack</p>" +
                "<p>Este é um email automatizado, por favor não responda.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
    
    /**
     * Send event ticket confirmation email
     */
    public void sendEventTicketEmail(String toEmail, String firstName, String eventTitle, 
                                      String eventDate, String eventLocation, String ticketCode,
                                      boolean isOnline, String onlineLink, String eventPrice) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("🎫 Confirmação de Inscrição - " + eventTitle);
            
            String emailBody = buildEventTicketEmailTemplate(firstName, eventTitle, eventDate, 
                                                              eventLocation, ticketCode, isOnline, 
                                                              onlineLink, eventPrice);
            
            helper.setText(emailBody, true);
            mailSender.send(message);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to send event ticket email: " + e.getMessage(), e);
        }
    }
    
    private String buildEventTicketEmailTemplate(String firstName, String eventTitle, String eventDate,
                                                  String eventLocation, String ticketCode, boolean isOnline,
                                                  String onlineLink, String eventPrice) {
        String name = firstName != null && !firstName.isEmpty() ? firstName : "Participante";
        String logoUrl = appUrl + "/Icon.png";
        String locationInfo = isOnline ? "Evento Online" : (eventLocation != null ? eventLocation : "A confirmar");
        String priceInfo = eventPrice != null && !eventPrice.equals("0") ? eventPrice + " Kz" : "Gratuito";
        
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<style>" +
                "body { font-family: 'Segoe UI', Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; background-color: #f0f2f5; }" +
                ".container { max-width: 600px; margin: 20px auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.1); }" +
                ".header { background: linear-gradient(135deg, #0A2463 0%, #1E3A8A 100%); color: white; padding: 30px 20px; text-align: center; }" +
                ".header-icon { font-size: 48px; margin-bottom: 10px; }" +
                ".logo { max-width: 100px; height: auto; margin-bottom: 15px; }" +
                ".content { padding: 30px 25px; }" +
                ".ticket-container { background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%); border-radius: 12px; padding: 25px; margin: 20px 0; border: 2px dashed #0A2463; position: relative; }" +
                ".ticket-header { text-align: center; border-bottom: 1px solid #cbd5e1; padding-bottom: 15px; margin-bottom: 15px; }" +
                ".ticket-code { font-size: 32px; font-weight: 800; color: #0A2463; letter-spacing: 3px; font-family: 'Courier New', monospace; background: #fff; padding: 12px 20px; border-radius: 8px; display: inline-block; margin: 10px 0; box-shadow: 0 2px 8px rgba(10, 36, 99, 0.15); }" +
                ".ticket-label { font-size: 12px; color: #64748b; text-transform: uppercase; letter-spacing: 1px; font-weight: 600; }" +
                ".event-details { margin: 20px 0; }" +
                ".detail-row { display: flex; align-items: flex-start; margin: 12px 0; padding: 10px 0; border-bottom: 1px solid #e2e8f0; }" +
                ".detail-icon { width: 24px; color: #0A2463; margin-right: 12px; font-size: 18px; }" +
                ".detail-content { flex: 1; }" +
                ".detail-label { font-size: 11px; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 2px; }" +
                ".detail-value { font-size: 15px; color: #1e293b; font-weight: 500; }" +
                ".highlight-box { background: linear-gradient(135deg, #dcfce7 0%, #bbf7d0 100%); border-left: 4px solid #22c55e; padding: 15px; margin: 20px 0; border-radius: 0 8px 8px 0; }" +
                ".online-link { background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%); border-left: 4px solid #3b82f6; padding: 15px; margin: 20px 0; border-radius: 0 8px 8px 0; }" +
                ".button { display: inline-block; padding: 14px 35px; background: linear-gradient(135deg, #0A2463 0%, #1E3A8A 100%); color: white; text-decoration: none; border-radius: 8px; margin: 20px 0; font-weight: 600; font-size: 15px; box-shadow: 0 4px 12px rgba(10, 36, 99, 0.3); }" +
                ".button:hover { background: linear-gradient(135deg, #1E3A8A 0%, #0A2463 100%); }" +
                ".qr-placeholder { text-align: center; margin: 20px 0; padding: 20px; background: #fff; border-radius: 8px; }" +
                ".instructions { background: #fffbeb; border-left: 4px solid #f59e0b; padding: 15px; margin: 20px 0; border-radius: 0 8px 8px 0; }" +
                ".instructions h4 { margin: 0 0 10px 0; color: #92400e; font-size: 14px; }" +
                ".instructions ul { margin: 0; padding-left: 20px; color: #78350f; font-size: 13px; }" +
                ".instructions li { margin: 5px 0; }" +
                ".footer { text-align: center; padding: 25px; color: #64748b; font-size: 12px; background: #f8fafc; }" +
                ".social-links { margin: 15px 0; }" +
                ".divider { height: 1px; background: #e2e8f0; margin: 20px 0; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<img src='" + logoUrl + "' alt='Stack Logo' class='logo' />" +
                "<div class='header-icon'>🎫</div>" +
                "<h1 style='margin: 0; font-size: 24px;'>Inscrição Confirmada!</h1>" +
                "<p style='margin: 10px 0 0 0; opacity: 0.9; font-size: 14px;'>Seu bilhete para o evento</p>" +
                "</div>" +
                "<div class='content'>" +
                "<h2 style='color: #0A2463; margin-bottom: 5px;'>Olá " + name + "!</h2>" +
                "<p style='color: #64748b; margin-top: 0;'>A sua inscrição foi realizada com sucesso. Guarde este email - ele é o seu bilhete de entrada!</p>" +
                
                "<div class='ticket-container'>" +
                "<div class='ticket-header'>" +
                "<p class='ticket-label'>Código do Bilhete</p>" +
                "<div class='ticket-code'>" + ticketCode + "</div>" +
                "<p style='font-size: 12px; color: #64748b; margin: 5px 0 0 0;'>Apresente este código na entrada do evento</p>" +
                "</div>" +
                
                "<h3 style='color: #0A2463; margin: 15px 0 10px 0; font-size: 18px;'>" + eventTitle + "</h3>" +
                
                "<div class='event-details'>" +
                "<div class='detail-row'>" +
                "<span class='detail-icon'>📅</span>" +
                "<div class='detail-content'>" +
                "<div class='detail-label'>Data e Hora</div>" +
                "<div class='detail-value'>" + eventDate + "</div>" +
                "</div>" +
                "</div>" +
                
                "<div class='detail-row'>" +
                "<span class='detail-icon'>📍</span>" +
                "<div class='detail-content'>" +
                "<div class='detail-label'>Localização</div>" +
                "<div class='detail-value'>" + locationInfo + "</div>" +
                "</div>" +
                "</div>" +
                
                "<div class='detail-row' style='border-bottom: none;'>" +
                "<span class='detail-icon'>💰</span>" +
                "<div class='detail-content'>" +
                "<div class='detail-label'>Valor</div>" +
                "<div class='detail-value'>" + priceInfo + "</div>" +
                "</div>" +
                "</div>" +
                "</div>" +
                "</div>" +
                
                (isOnline && onlineLink != null && !onlineLink.isEmpty() ? 
                "<div class='online-link'>" +
                "<p style='margin: 0 0 10px 0; font-weight: 600; color: #1e40af;'>🔗 Link de Acesso ao Evento Online</p>" +
                "<p style='margin: 0; font-size: 13px; color: #1e40af;'>O link será disponibilizado mais próximo da data do evento ou pode acessar directamente:</p>" +
                "<a href='" + onlineLink + "' style='color: #2563eb; word-break: break-all;'>" + onlineLink + "</a>" +
                "</div>" : "") +
                
                "<div class='highlight-box'>" +
                "<p style='margin: 0; font-weight: 600; color: #166534;'>✅ Inscrição Confirmada</p>" +
                "<p style='margin: 5px 0 0 0; font-size: 13px; color: #166534;'>O seu lugar está garantido! Não se esqueça de chegar com antecedência.</p>" +
                "</div>" +
                
                "<div class='instructions'>" +
                "<h4>📋 Instruções Importantes:</h4>" +
                "<ul>" +
                "<li>Apresente este email ou o código do bilhete na entrada</li>" +
                "<li>Chegue pelo menos 15 minutos antes do início</li>" +
                "<li>Traga um documento de identificação válido</li>" +
                "<li>Em caso de cancelamento, informe-nos com 24h de antecedência</li>" +
                "</ul>" +
                "</div>" +
                
                "<div class='divider'></div>" +
                
                "<div style='text-align: center;'>" +
                "<p style='color: #64748b; font-size: 14px;'>Tem alguma questão sobre o evento?</p>" +
                "<a href='" + appUrl + "/events' class='button'>Ver Detalhes do Evento</a>" +
                "</div>" +
                
                "</div>" +
                "<div class='footer'>" +
                "<p style='margin: 0 0 10px 0;'><strong>Stack</strong> - A sua plataforma de networking</p>" +
                "<p style='margin: 0 0 10px 0;'>Este é um email automático. Por favor, não responda directamente.</p>" +
                "<p style='margin: 0; font-size: 11px; color: #94a3b8;'>© 2024 Stack. Todos os direitos reservados.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
}

