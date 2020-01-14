package com.company;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;

/**
 * The type Main.
 */
public class Main {

    private static void setProperties(Properties properties) {
        /*
        Sets properties for this environment, this is meant for gmail users
        */
        properties.put("mail.smtp.port", 587);
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
    }

    private static List<String[]> readRecipients() throws FileNotFoundException {
        /*
        Parses the text in the recipients file by new lines and commas and puts them in an array with the first
        element being the name and second the email
         */
        List<String[]> recipients = new ArrayList<>();
        String fileName = System.getProperty("user.dir") + "\\recipients.txt";
        File file = new File(fileName);
        Scanner fileScanner = new Scanner(file).useDelimiter("\n");

        while (fileScanner.hasNext()) {
            String[] values = fileScanner.next().split(",");
            recipients.add(values);
        }

        return recipients;
    }

    private static List<String> readAttachments() throws FileNotFoundException {
        /*
        Gets the path names of all the files in the attachments folder, and then creates paths to each of them and
        returns a list of path names as strings
         */
        List<String> items = new ArrayList<>();
        String dir = System.getProperty("user.dir") + "\\attachments";  // ADD PATH TO ATTACHMENTS FOLDER HERE
        try (Stream<Path> walk = Files.walk(Paths.get(dir))) {

            return walk.filter(Files::isRegularFile).map(Path::toString).collect(Collectors.toList());

        } catch (IOException e) {
            e.printStackTrace();
        }
        return items;
    }

    private static void sendMessage(Session session, String username, List<String[]> recipients,
                                    StringBuilder stringBuilder, List<String> paths, String subject)
            throws MessagingException {
        /*
        Puts message together by adding contents from input.txt and adding a greeting "Hello <NAME>," and proceeds to
        add attachments from the attachments folder if there are any
         */

        Multipart multipart = new MimeMultipart();
        Message message = new MimeMessage(session);

        message.setSubject(subject);
        message.setFrom(new InternetAddress(username));
        String body = stringBuilder.toString();
        BodyPart bodyPart;

        for (String[] recipient : recipients) {
            bodyPart = new MimeBodyPart();
            bodyPart.setText("Hello " + recipient[0] + " ," + " \n" + body);
            multipart.addBodyPart(bodyPart);

            for (String attachment : paths) {
                bodyPart = new MimeBodyPart();
                DataSource source = new FileDataSource(attachment);
                bodyPart.setDataHandler(new DataHandler(source));
                bodyPart.setFileName(attachment.substring(attachment.lastIndexOf("\\") + 1).trim());
                multipart.addBodyPart(bodyPart);
            }

            message.setContent(multipart);
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipient[1]));
            Transport.send(message);
            System.out.println("message sent to " + recipient[0] + " to the address " + recipient[1]);
        }
    }

    private static StringBuilder getBody() throws FileNotFoundException {
        /*
        getBody reads the input file and puts the text separated by newline characters into a string builder, this gives
        the user a change to modify the contents in the program.
         */

        List<String> paragraphs = new ArrayList<>();
        String fileName = System.getProperty("user.dir") + "\\input.txt";                // ADD FILEPATH FOR EMAIL BODY
        File file = new File(fileName);
        Scanner fileScanner = new Scanner(file).useDelimiter("\n");
        StringBuilder stringBuilder = new StringBuilder();

        while (fileScanner.hasNext()) {
            paragraphs.add(fileScanner.next());
        }

        for (String item : paragraphs) {
            String space = item + " \n";
            stringBuilder.append(space);
        }
        return stringBuilder;
    }

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     * @throws IOException        the io exception
     * @throws MessagingException the messaging exception
     */
    public static void main(String[] args) throws IOException, MessagingException {

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        Properties properties = new Properties();
        String finalUsername = ""; // Your email
        String finalPassword = ""; // Your password
        String subject = "";       // Subject of your email

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                return new javax.mail.PasswordAuthentication(finalUsername, finalPassword);
            }
        });

        setProperties(properties); // set properties for email
        List<String> paths = readAttachments(); // get paths for attachments
        StringBuilder body = getBody(); // get the body paragraphs as string builder
        List<String[]> recipients = readRecipients(); // get recipients (names and emails)
        sendMessage(session, finalUsername, recipients, body, paths, subject); // send email
    }
}
