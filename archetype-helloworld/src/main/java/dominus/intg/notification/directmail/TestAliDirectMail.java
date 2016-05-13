package dominus.intg.notification.directmail;


import dominus.framework.junit.DominusJUnit4TestBase;
import org.junit.Test;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * 使用javamail通过smtp协议发信
 */
public class TestAliDirectMail extends DominusJUnit4TestBase {
    private static final String ALIDM_SMTP_HOST = "smtpdm.aliyun.com";
    private static final int ALIDM_SMTP_PORT = 25;

    String mailUser;
    String mailPassword;
    String mailTo;

    @Override
    protected void doSetUp() throws Exception {
        super.doSetUp();
        mailUser = properties.getProperty("mail.user");
        mailPassword = properties.getProperty("mail.password");
        mailTo = properties.getProperty("mail.to");
    }

    @Test
    public void testSendMail() throws MessagingException {

        // 配置发送邮件的环境属性
        final Properties props = new Properties();
        // 表示SMTP发送邮件，需要进行身份验证
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", ALIDM_SMTP_HOST);
        props.put("mail.smtp.port", ALIDM_SMTP_PORT);
        // 如果使用ssl，则去掉使用25端口的配置，进行如下配置,
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.port", "465");

        // 发件人的账号
        props.put("mail.user", mailUser);
        // 访问SMTP服务时需要提供的密码
        props.put("mail.password", mailPassword);

        // 构建授权信息，用于进行SMTP进行身份验证
        Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                // 用户名、密码
                String userName = props.getProperty("mail.user");
                String password = props.getProperty("mail.password");
                return new PasswordAuthentication(userName, password);
            }
        };
        // 使用环境属性和授权信息，创建邮件会话
        Session mailSession = Session.getInstance(props, authenticator);
        // 创建邮件消息
        MimeMessage message = new MimeMessage(mailSession);
        // 设置发件人
        InternetAddress form = new InternetAddress(
                props.getProperty("mail.user"));
        message.setFrom(form);

        // 设置收件人
        InternetAddress to = new InternetAddress(mailTo);
        message.setRecipient(MimeMessage.RecipientType.TO, to);

        // 设置邮件标题
        message.setSubject("测试邮件");
        // 设置邮件的内容体
        message.setContent("测试的HTML邮件", "text/html;charset=UTF-8");
        // 发送邮件
        Transport.send(message);
    }

    @Override
    protected void doTearDown() throws Exception {
        super.doTearDown();
    }
}
