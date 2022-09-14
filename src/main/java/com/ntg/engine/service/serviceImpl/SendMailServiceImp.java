package com.ntg.engine.service.serviceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.Document;
import com.itextpdf.text.html.simpleparser.HTMLWorker;
import com.itextpdf.text.pdf.PdfWriter;
import com.ntg.engine.entites.SendMail;
import com.ntg.engine.entites.ToDoList;
import com.ntg.engine.jobs.JobUtil;
import com.ntg.engine.jobs.SaveThread;
import com.ntg.engine.jobs.SaveThreadTransactionInformation;
import com.ntg.engine.jobs.service.CommonCachingFunction;
import com.ntg.engine.repository.customImpl.SqlHelperDaoImpl;
import com.ntg.engine.service.SendMailService;
import com.ntg.engine.util.EmailTemplate;
import com.ntg.engine.util.Utils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.Message.RecipientType;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@SuppressWarnings("deprecation")
@Service
public class SendMailServiceImp implements SendMailService {

    @Value("${mailSettings.mailServer}")
    private String mailServer;

    @Value("${mailSettings.serverPort}")
    private String portNumber;

    @Value("${mailSettings.userName}")
    private String mailUserName;

    @Value("${mailSettings.password}")
    private String mailPassword;

    @Value("${mailSettings.socketFactoryPort}")
    private String socketFactoryPort;

    @Value("${mailSettings.socketFactoryClass}")
    private String socketFactoryClass;

    @Value("${mailSettings.fromMailAddress}")
    private String fromMailAddress;

    @Value("${mailSettings.auth}")
    private String mailAuth;

    @Value("${mailSettings.starttls}")
    private String starttls;

    @Value("${mailSettings.ssl}")
    private String ssl;

    @Autowired
    SqlHelperDaoImpl sqlHelperDao;

    private Pattern pattern = Pattern.compile("\\{\\{(.*?)\\}\\}");

    private Matcher matcher;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${wf_GetEmail}")
    String wf_GetEmail;

    @Value("${wf_GetGroupEmail}")
    String wf_GetGroupEmail;

    @Autowired
    private CommonCachingFunction commonCachingFun;

    @Override
    public void sendMail(SendMail sendMailData , byte[] attachments)
            throws MessagingException {
        Properties props = System.getProperties();
        props.put("mail.smtp.host", mailServer);
        props.put("mail.smtp.auth", mailAuth); // Enabling SMTP Authentication
        props.put("mail.smtp.port", portNumber);
        props.setProperty("mail.smtp.starttls.enable", starttls);
        props.setProperty("mail.smtp.ssl.enable", ssl);


        // creates session with authentication
        Session session = Session.getInstance(props, new Authenticator() {

            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new javax.mail.PasswordAuthentication(mailUserName, mailPassword);
            }
        });
        // session.setDebug(true);

        Message message;
        message = getMessage(sendMailData, session , attachments);
        Transport.send(message);
    }

    private Message getMessage(SendMail mail, Session session  , byte[] attachments)
            throws MessagingException {
        Message message = new MimeMessage(session);

        message.setFrom(new InternetAddress(fromMailAddress));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(mail.getSendTo()));
        // message.setRecipients(RecipientType.TO, tos);
        if (mail.getCCList() != null) {
            InternetAddress CCs[] = InternetAddress.parse(StringUtils.join(mail.getCCList(), ",").replace(";" , ","));
            message.setRecipients(RecipientType.CC, CCs);
        }

        // use the true flag to indicate you need a multipart message
        MimeMessageHelper helper = new MimeMessageHelper((MimeMessage) message, true, StandardCharsets.UTF_8.toString());
        helper.setSubject(mail.getSubject());
        helper.setText(mail.getBody(), true);
        if (mail.getCIDImageData() != null && mail.getBase64ImageData() != null) {
            for (int i = 0; i < mail.getCIDImageData().size(); i++) {
                byte[] bytes = DatatypeConverter.parseBase64Binary(mail.getBase64ImageData().get(i).toString());
                helper.addAttachment(mail.getCIDImageData().get(i).toString(), new ByteArrayResource(bytes));
            }

        }
        if (Utils.isNotEmpty(attachments)) {
            ZipEntry localFileHeader;
            int readLen;
            byte[] readBuffer = new byte[4096];

            InputStream is = new ByteArrayInputStream(attachments);
            try (ZipInputStream zipInputStream = new ZipInputStream(is)) {
                while ((localFileHeader = zipInputStream.getNextEntry()) != null) {
                    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                        while ((readLen = zipInputStream.read(readBuffer)) != -1) {
                            outputStream.write(readBuffer, 0, readLen);
                        }
                        helper.addAttachment(localFileHeader.getName(), new ByteArrayResource(outputStream.toByteArray()));
                    }
                }
            } catch (IOException ignored) {}
        }

        return message;
    }

    // =======added by Abdulrahman Helal to handle Email Template=========
    public void sendMailTemplate(EmailTemplate emailTemplate, String body, String filename, byte[] attachments) throws MessagingException {
        // ==== mail configuration =====
        Properties props = System.getProperties();

        props.put("mail.smtp.host", mailServer);
        props.put("mail.smtp.auth", mailAuth); // Enabling SMTP Authentication
        props.put("mail.smtp.port", portNumber);
        props.setProperty("mail.smtp.ssl.enable", ssl);


        if (!StringUtils.isEmpty(this.starttls)) {
            props.put("mail.smtp.starttls.enable", starttls);
        }
        if (!StringUtils.isEmpty(socketFactoryPort) && !StringUtils.isEmpty(socketFactoryClass)) {
            props.put("mail.smtp.socketFactory.port", socketFactoryPort);
            props.put("mail.smtp.socketFactory.class", socketFactoryClass);

        }

        // ==== open session for from mail address to send mail ====
        Session session = Session.getInstance(props, new Authenticator() {

            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new javax.mail.PasswordAuthentication(mailUserName, mailPassword);
            }
        });
        // session.setDebug(true);

        // ====handle create message ===
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(fromMailAddress));
        message.setRecipients(Message.RecipientType.TO,
                InternetAddress.parse(emailTemplate.getToEmail()));
        // message.setRecipients(RecipientType.TO, tos);
        if (emailTemplate.getCcEmail() != null) {
            String[] ccMails = emailTemplate.getCcEmail().replace(";" , ",").split(",");
            InternetAddress CCs[] = InternetAddress.parse(StringUtils.join(ccMails, ","));
            message.setRecipients(RecipientType.CC, CCs);
        }
        // ===== Set Subject: header field ====
        message.setSubject(emailTemplate.getEmailSubject());
        // ===== Create the message part =======
        BodyPart messageBodyPart = new MimeBodyPart();
        // ====Now set the actual message ===
        messageBodyPart.setText(body);
        // ==Create a multipart message ===
        Multipart multipart = new MimeMultipart();

        // ==Set text message part==
        multipart.addBodyPart(messageBodyPart);

        // === add attachment tm mail body ===
        messageBodyPart = new MimeBodyPart();
        DataSource source = new FileDataSource(filename);
        messageBodyPart.setDataHandler(new DataHandler(source));
        messageBodyPart.setFileName(filename);
        multipart.addBodyPart(messageBodyPart);

        addAttachments(attachments, multipart);

        // Send the complete message parts
        message.setContent(multipart);
        Transport.send(message);

    }

    private void addAttachments(byte[] attachments, Multipart multipart) throws MessagingException {
        if (Utils.isNotEmpty(attachments)) {
            ZipEntry localFileHeader;
            int readLen;
            byte[] readBuffer = new byte[4096];

            InputStream is = new ByteArrayInputStream(attachments);
            try (ZipInputStream zipInputStream = new ZipInputStream(is)) {
                while ((localFileHeader = zipInputStream.getNextEntry()) != null) {
                    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                        while ((readLen = zipInputStream.read(readBuffer)) != -1) {
                            outputStream.write(readBuffer, 0, readLen);
                        }
                        ByteArrayDataSource bds = new ByteArrayDataSource(outputStream.toByteArray(),"application/octet-stream");
                        MimeBodyPart mimeBodyPart = new MimeBodyPart();
                        mimeBodyPart.setDisposition("attachment");
                        mimeBodyPart.setFileName(MimeUtility.encodeText(localFileHeader.getName()));
                        mimeBodyPart.setDataHandler(new DataHandler(bds));
                        multipart.addBodyPart(mimeBodyPart);
                    }
                }
            } catch (IOException ignored) {}
        }
    }

    // ====added by Abdulrahman Helal To convert from html text to pdf file ===
    public String convertFromHtmlTextToPDFFile(EmailTemplate emailTemplate) throws Exception {

        String homePath = System.getProperty("user.home");
        File theDir = new File(homePath + "/Email Template");
        if (!theDir.exists())
            theDir.mkdir();
        String fileName = homePath + "/Email Template/" + emailTemplate.getName() + System.currentTimeMillis()
                + ".pdf";
        FileOutputStream file = new FileOutputStream(fileName);
        Document document = new Document();
        PdfWriter.getInstance(document, file);
        document.open();
        // debrecated ,, we can use xmlworker
        HTMLWorker htmlWorker = new HTMLWorker(document);
        htmlWorker.parse(new StringReader(emailTemplate.getTemplateBody().replaceAll("''(?=\\d'')", "'").replaceAll("''(?<=\\d'')", "'")));
        document.close();
        file.close();
        return fileName;

    }

    // Update handle email template for email task By Mahmoud Atef
    @SuppressWarnings("unchecked")
    @Transactional
    public String handleSendingEmailTemplate(ToDoList row, Long emailTemplateId, Long typeId, Long object_id,
                                             String toEmail, Boolean notEscalation, String companyName, String teneatSchema, boolean isActionableMail ,List<String> ccList) throws Exception {
        String emailResponse = null;


        SendMail sendMailData = null;
        Map<String, Object> EmailTemplateMap = null;
        if (Utils.isNotEmpty(emailTemplateId)) {
            EmailTemplateMap = commonCachingFun.getTaskEmailTemplate(emailTemplateId, companyName);
        }


        if (Utils.isNotEmpty(EmailTemplateMap)) {
            EmailTemplate EmailTemplate = new EmailTemplate();

            String emailTemplateBody = null, to_email = null, emailSubject = null;
            Map<String, Object> map = null;
            Map ObjectOfProcess = null;

            if (Utils.isNotEmpty(EmailTemplateMap.get("templateBody"))) {
                ObjectMapper objectMapper = new ObjectMapper();
                map = objectMapper.convertValue(row, Map.class);
                ObjectOfProcess = commonCachingFun.loadCrmObject(map, typeId, object_id, companyName);
                String description = commonCachingFun.resolveDescription(map, EmailTemplateMap.get("templateBody").toString(), typeId, object_id, ObjectOfProcess, Utils.isNotEmpty(emailTemplateId) ? true : false, companyName, teneatSchema);
                emailTemplateBody = description;
            }


            if (Utils.isNotEmpty(toEmail)) {
                to_email = toEmail;
            }

            if (Utils.isNotEmpty(EmailTemplateMap.get("emailSubject"))) {

                if (map == null) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    map = objectMapper.convertValue(row, Map.class);
                }
                if (ObjectOfProcess == null) {
                    ObjectOfProcess = commonCachingFun.loadCrmObject(map, typeId, object_id, companyName);
                }
                emailSubject = commonCachingFun.resolveDescription(map, EmailTemplateMap.get("emailSubject").toString(),
                        typeId, object_id, ObjectOfProcess, Utils.isNotEmpty(emailTemplateId) ? true : false,
                        companyName, teneatSchema);
                EmailTemplate.setEmailSubject(emailSubject);
            }

            Boolean sendAttachemnt = JobUtil.getBooleanValue(EmailTemplateMap.get("sendContentAsAttachment"));

            String body = null;
            String resolvedDesc = null;

            // load object
            Map obj = commonCachingFun.loadCrmObject(null, typeId, object_id, companyName);
            // resolve description
            if (Utils.isNotEmpty(emailTemplateBody) && emailTemplateBody.contains("{{")) {
                sendMailData = commonCachingFun.replaceDescriptionWithValuesInMail(emailTemplateBody, obj, true, companyName, teneatSchema, row, isActionableMail ,typeId,object_id );
                resolvedDesc = sendMailData.getBody();
            } else {
                sendMailData = new SendMail();
                resolvedDesc = emailTemplateBody;
                sendMailData.setBody(emailTemplateBody);
            }
            byte[] attachments = commonCachingFun.loadEmailAttachments(emailTemplateId
                    , object_id, companyName);
            if (sendAttachemnt == true) {
                if (Utils.isNotEmpty(EmailTemplateMap.get("noteForAttachmentContent"))) {
                    body = EmailTemplateMap.get("noteForAttachmentContent").toString();
                }
                if (Utils.isNotEmpty(resolvedDesc)) {
                    EmailTemplate.setTemplateBody(resolvedDesc);
                }
                if (Utils.isNotEmpty(toEmail)) {
                    EmailTemplate.setToEmail(toEmail);
                }
                String filename = convertFromHtmlTextToPDFFile(EmailTemplate);


                sendMailTemplate(EmailTemplate, body, filename , attachments);
                if (Utils.isNotEmpty(row) && row.taskTypeId == 10 && notEscalation) {
                    handleEmailTaskRoute(row, null);
                } else {
                    // added to handle escalation email trace
                    emailResponse = "Email Send Succssfully";
                }

            } else {
                // == for normal content not as attachment
                if (Utils.isNotEmpty(resolvedDesc)) {
                    EmailTemplate.setTemplateBody(resolvedDesc);
                }
                body = EmailTemplate.getTemplateBody();


                sendMailData.setSendTo(to_email);
                sendMailData.setCCList(ccList);
                sendMailData.setSubject(emailSubject);
                sendMailData.setBody(body);

                sendMail(sendMailData , attachments);


                if (Utils.isNotEmpty(row) && row.taskTypeId == 10 && notEscalation) {
                    handleEmailTaskRoute(row, null);
                } else {
                    // added to handle escalation email trace
                    emailResponse = "Email Send Succssfully";
                }

            }
        }

        return emailResponse;
    }

    // handle END and routes Of Email Task by Abdulrahman updated By Mahmoud
    public void handleEmailTaskRoute(ToDoList row, Exception exp) {

        List<SaveThreadTransactionInformation> txs = new ArrayList<SaveThreadTransactionInformation>();
        List<Map<String, Object>> taskRouting = commonCachingFun.getTaskRouting(row.taskID);
        if (Utils.isNotEmpty(taskRouting)) {
            List<Object> pramRoutes = new ArrayList<>();
            String updateRoutes = "Update " + row.udaName + " SET status_id = 2  where taskid in (";
            int i = taskRouting.size();
            for (Map<String, Object> taskRoute : taskRouting) {
                Long toTaskId = JobUtil.convertBigDecimalToLong(taskRoute.get("to_taskid"));
                updateRoutes += "? ";
                i--;
                if (i <= 0)
                    updateRoutes += ")";
                else
                    updateRoutes += ",";

                pramRoutes.add(toTaskId);
            }
            SaveThreadTransactionInformation txRoues = new SaveThreadTransactionInformation(updateRoutes,
                    SaveThreadTransactionInformation.TxOperaiotns.ExecuteUpdateSql, pramRoutes);
            txs.add(txRoues);

        }
        // ===handle end email task
        List<Object> pram = new ArrayList<>();
        String Sql;
        // added to handle escalation email trace

        //                    emailResponse = "Email Send Failed " + exp.getMessage();
        if (exp == null)
            Sql = "Update " + row.udaName + " SET status_id = 4 , ACTUALENDDATE =localtimestamp where RECID = ?";
        else
            Sql = "Update " + row.udaName + " SET status_id = 20 , ACTUALENDDATE =localtimestamp where RECID = ?";
        pram.add(row.Incom_RecID);
        SaveThreadTransactionInformation tx = new SaveThreadTransactionInformation(Sql,
                SaveThreadTransactionInformation.TxOperaiotns.ExecuteUpdateSql, pram);
        txs.add(tx);
        SaveThread.AddSaveThreadTransaction(txs);

    }
}
