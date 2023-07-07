import java.io.*;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

class IMAPClient {

    //prideti zip
    //pratimai
    public static void main(String[] args){

        String serverHostname = "imap.gmail.com";
        String email = "ciastiklainis@gmail.com";
        int serverPort = 993;
        String clientId = "59215509257-6km4vvdr0vc53ul1ed1m965flrc2d308.apps.googleusercontent.com";
        String clientSecret = "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";
        String accessToken = "ya29.a0AWY7Ckm9sq3c9mzjO2c2tnKbsQ-gS0CxtovV80XXlhJU9RpX_krGIKuDa0p4k9V0yP_WJqFu1BJF56uBbsaQBrgw6jZP2Mzyl9uphUkUJuexKNIMps97n7LFF6W0pvIKAQora4EaG6pwIhA_m0dADdfZQxFVaCgYKASISARISFQG1tDrpfdkxE-D2BqMBad_axpuaqQ0163";
        String refreshToken = "1//0c0gclepuR2zhCgYIARAAGAwSNwF-L9Irp17CaPVsQb4CQuu7kMpgkbReS4CPIw0RIeYA2EGfxPneakaHnez2tuE3KBSZNPK5rVY";
        String formattedToken;

        try {
////
//            Authentication auth = new Authentication(clientId, clientSecret);
//            auth.authenticate();
////
//            NewTokenGetter ntg = new NewTokenGetter(refreshToken, clientId, clientSecret);
//            ntg.getNewToken();

            SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();

            // Connect to IMAP server using SSL/TLS
            SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(serverHostname, serverPort);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            socket.startHandshake();

            formattedToken = "user=" + email + "\u0001auth=Bearer " + accessToken + "\u0001\u0001";
            formattedToken = Base64.getEncoder().encodeToString(formattedToken.getBytes());

            out.println("1 AUTHENTICATE XOAUTH2 " + formattedToken);
            printConnectionMessage(in, 1);

            printMenu(out, in);


            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public static void printServerMessage(BufferedReader in, int num, String message) throws IOException {
        while(true){
            String response = in.readLine();
            if(response.startsWith(""+ num)){
                if(response.contains("OK")){
                    System.out.println(message);
                }else{
                    System.out.println(response);
                }

                break;
            }

        }
    }

    public static void printFetchMessage(BufferedReader in, int num) throws IOException {
        while(true){
            String response = in.readLine();
            if(response.startsWith("" + num)){
                break;
            }

            if(response != null && !response.startsWith(")")){
                System.out.println(response);
            }

        }
    }

    public static void printFetchMessageText(BufferedReader in, int num) throws IOException {
        StringBuilder message = new StringBuilder();
        String response;
        while (true) {
            response = in.readLine();

            if(response.startsWith("" + num)){
                break;
            }

            if (response.startsWith("Content-Type: text/plain")) {
                while ((response = in.readLine()) != null && !response.startsWith("--")) {
                    message.append(response);
                    message.append("\n");
                }
            }

        }

        System.out.println("Message: " + message);
    }

    public static void fetch(PrintWriter out, BufferedReader in, String messageId, int num) throws IOException {
        out.println(num + " FETCH " + messageId + " (BODY[HEADER.FIELDS (FROM SUBJECT DATE)])");
        printFetchMessage(in, num);
        out.println(++num + " FETCH " + messageId + " (BODY[TEXT])");
        printFetchMessageText(in, num);

    }


    public static void printConnectionMessage(BufferedReader in, int num) throws IOException {
        while(true){
            String response = in.readLine();
            if(response != null){
                System.out.println(response);
            }
            if(response.startsWith(""+ num)){
                break;
            }
        }
    }

    public static void printMenu(PrintWriter out, BufferedReader in) throws IOException {
        System.out.println("Select action:\n" +
                "0 - show menu\n" +
                "1 - show all received emails\n" +
                "2 - delete an email\n" +
                "3 - find email\n" +
                "4 - exit");

        Scanner scanner = new Scanner(System.in);

        boolean continueMenu = true;
        while(continueMenu){
            int input = scanner.nextInt();
            switch (input){
                case 0:
                    printMenu(out, in);
                    break;
                case 1:
                    showAllReceivedEmails(out, in);
                    break;
                case 2:
                    deleteEmail(out, in, new Scanner(System.in));
                    break;
                case 3:
                    searchForAnEmail(out, in, new Scanner(System.in));
                    break;
                case 4:
                    continueMenu = false;
                    break;
                default:
                    printMenu(out, in);

            }
        }

    }

    public static void showAllReceivedEmails(PrintWriter out, BufferedReader in) throws IOException {
        out.println("2 SELECT INBOX");
        printServerMessage(in, 2, "INBOX selected");

        out.println("3 SEARCH ALL");
        String[] messageIds = getMessageIds(in);

        getAttachmentAndBodyText(out, in, messageIds);


    }

    public static void getAttachmentAndBodyText(PrintWriter out, BufferedReader in, String[] messageIds) throws IOException {

        if (messageIds != null) {
            int num = 4;
            for (String messageId : messageIds) {
                out.println(num + " FETCH " + messageId + " BODYSTRUCTURE");
                String bs = getBodyStructure(in, messageId, num);
                if (bs != null) {
                    if (bs.contains("ATTACHMENT")) {
                        String[] parts = bs.split("\\)");
                        for (String part : parts) {
                            if (part.contains("ATTACHMENT")) {
                                String[] fields = part.split(" ");
                                String type = fields[2].replaceAll("[()]", "");
                                String encoding = fields[3].replaceAll("[()]", "");
                                int size = Integer.parseInt(fields[4].replaceAll("[()]", ""));
                                String filename = fields[8].replaceAll("[()]", "");

                                filename = filename.substring(1, filename.length() - 1);

                                out.println(++num + " FETCH " + messageId + " BODY[2]");

                                String encodedAttachment = getEncodedAttachment(in, num);

                                fetch(out, in, messageId, ++num);

                                decodeAttachment(encodedAttachment, filename);

                                if (filename.contains("zip")) {
                                    zip(filename);
                                }

                            }
                        }
                    } else {
                        fetch(out, in, messageId, num++);

                    }

                }
                num += 2;
            }
        }
    }

    public static void zip(String filename) throws IOException {
        String filePath = "C:/Users/Erika/Desktop" + "/" + filename;
        String destDirectory = "C:/Users/Erika/Desktop" + "/" + filename.substring(0, filename.length()-4);
        byte[] buffer = new byte[1024];

        File destDir = new File(destDirectory);

        if (!destDir.exists()) {
            destDir.mkdir();
        }

        ZipInputStream zis = new ZipInputStream(new FileInputStream(filePath));

        ZipEntry zipEntry = zis.getNextEntry();
        while(zipEntry != null) {

            String fileName = zipEntry.getName();

            File newFile = new File(destDirectory + File.separator + fileName);
            System.out.println("Unzipping to "+newFile.getAbsolutePath());

            if (zipEntry.isDirectory()) {
                // create the directory if it doesn't exist
                newFile.mkdirs();
            } else {
                // create all non-existent directories for the file
                new File(newFile.getParent()).mkdirs();

                // write the file content to disk
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }

            zis.closeEntry();
            zipEntry = zis.getNextEntry();
        }

        // close the zip file
        zis.closeEntry();
        zis.close();
    }


    public static String getBodyStructure(BufferedReader in, String messageId, int num) throws IOException {
        String bs = null;
        while (true) {
            String response = in.readLine();
            if (response.contains("BODYSTRUCTURE")) {
                bs = response;
            }
            if (response.startsWith("" + num + " OK")) {
                break;
            }

        }

        return bs;
    }

    public static String getEncodedMessage(BufferedReader in, int num) throws IOException {
        String encodedMessage = "";
        while (true) {
            String response = in.readLine();
            if (response.startsWith(num + " OK")) {
                System.out.println(response);
                break;    
            }

            if (!response.contains("FETCH") && !response.contains(num + " OK")) {
                encodedMessage += response;
            }

        }
        return encodedMessage;
    }

    public static String getEncodedAttachment(BufferedReader in, int num) throws IOException {
        String encodedAttachment = getEncodedMessage(in, num);
        encodedAttachment = encodedAttachment.substring(0, encodedAttachment.length() - 1);

        return encodedAttachment;
    }

    public static void decodeAttachment(String encodedAttachment, String filename) throws IOException {
        byte[] decodedBytes = Base64.getDecoder().decode(encodedAttachment);
        String filePath = "C:/Users/Erika/Desktop" + "/" + filename;

        FileOutputStream fileOutputStream = new FileOutputStream(filePath);
        fileOutputStream.write(decodedBytes);
        fileOutputStream.close();

        System.out.println("Attachment saved to file: " + filePath);
    }

    public static void deleteEmail(PrintWriter out, BufferedReader in, Scanner scanner) throws IOException {
        String response;
        String[] messageIds = findEmailBySenderAndSubject(out, in, scanner);

        if(messageIds != null){
            for (String messageId : messageIds) {
                out.println("4 STORE " + messageId + " +FLAGS (\\Deleted)");
//                while ((response = in.readLine()) != null) {
//                    if (response.startsWith("OK")) {
//                        System.out.println("Deleted successfully...");
//                        break;
//                    }
                }
            }

            out.println("5 EXPUNGE");
//            while ((response = in.readLine()) != null) {
//                System.out.println("server expunge:" + response);
//                if (response.startsWith("OK")) {
//                    break;
        System.out.println("Deleted successfully");
    }


    public static void searchForAnEmail(PrintWriter out, BufferedReader in, Scanner scanner) throws IOException {
        String[] messageIds = findEmailBySenderAndSubject(out, in, scanner);

       getAttachmentAndBodyText(out, in, messageIds);
    }

    public static String[] findEmailBySenderAndSubject(PrintWriter out, BufferedReader in, Scanner scanner) throws IOException {
        System.out.println("Sender: ");
        String email = scanner.nextLine();
        System.out.println("Subject: ");
        String subject = scanner.nextLine();

        out.println("2 SELECT INBOX");
        printServerMessage(in, 2, "INBOX selected");

        String searchCriteria = "3 SEARCH SUBJECT \"" + subject + "\" FROM \"" + email +"\"";
        out.println(searchCriteria);

        String[] messageIds = getMessageIds(in);

        return  messageIds;
    }

    public static String[] getMessageIds(BufferedReader in) throws IOException {
        String response;
        String[] messageIds = null;

        while ((response = in.readLine()) != null) {
            if (response.startsWith("* SEARCH")) {
                try{
                    messageIds = response.substring("* SEARCH ".length()).split(" ");
                }catch (StringIndexOutOfBoundsException e){
                    System.out.println("No emails were found...");
                }
                break;

            }
        }

        if(messageIds != null){
            System.out.println(messageIds.length + " emails were found...");
        }

        return messageIds;
    }

//    public static void showAllSentEmails(PrintWriter out, BufferedReader in) throws IOException {
//        out.println("2 SELECT [Gmail]/ Sent Mail");
//        printServerMessage(in, 2, "SENT selected");
//
//        out.println("3 SEARCH ALL");
//        String[] messageIds = getMessageIds(in);
//
//        getAttachmentAndBodyText(out, in, messageIds);
//
//
//    }

    //    public static void sendEmail(PrintWriter out, BufferedReader in, Scanner scanner, SSLSocket socket) throws IOException {
//        out.println("2 SELECT INBOX");
//        print(in, 2);
//
//        System.out.println("Recipient's email address: ");
//        String recipientEmail = scanner.nextLine();
//        System.out.println("Subject: ");
//        String subject = scanner.nextLine();
//        System.out.println("Email content:");
//        String body = scanner.nextLine();
//        System.out.println("Add attachment?[Y/N]");
//        char isAttachment = scanner.next().charAt(0);
//        scanner.nextLine();
//        if(isAttachment == 'Y'){
//            System.out.println("Filepath:");
//            String attachmentPath = scanner.nextLine();
//
//            String attachmentType = Files.probeContentType(Paths.get(attachmentPath));
//            String attachmentName = new File(attachmentPath).getName();
//            System.out.println(attachmentType);
//            System.out.println(attachmentName);
//
//            String boundary = "boundary_" + System.currentTimeMillis();
//            String message = "To: " + recipientEmail + "\r\n" +
//                    "Subject: " + subject + "\r\n" +
//                    "MIME-Version: 1.0\r\n" +
//                    "Content-Type: multipart/mixed; boundary=" + boundary + "\r\n" +
//                    "\r\n" +
//                    "--" + boundary + "\r\n" +
//                    "Content-Type: text/plain; charset=utf-8\r\n" +
//                    "\r\n" +
//                    body + "\r\n" +
//                    "\r\n" +
//                    "--" + boundary + "\r\n" +
//                    "Content-Type: " + attachmentType + "\r\n" +
//                    "Content-Disposition: attachment; filename=\"" + attachmentName + "\"\r\n" +
//                    "Content-Transfer-Encoding: base64\r\n" +
//                    "\r\n";
//
//            byte[] attachmentBytes = Files.readAllBytes(Paths.get(attachmentPath));
//            byte[] encodedAttachmentBytes = Base64.getEncoder().encode(attachmentBytes);
//
//            message += new String(encodedAttachmentBytes) + "\r\n" +
//                    "\r\n" +
//                    "--" + boundary + "--" + "\r\n" +
//                    "\r\n" + "." + "\r\n";
//
//            byte[] encodedMessageBytes = Base64.getEncoder().encode(message.getBytes());
//
//            String append = "3 APPEND INBOX (\\Seen) {" + encodedMessageBytes.length + "}\r\n";
//            System.out.println(append);
//            out.print(append);
////            print(in, 3);
//
//            OutputStream outputStream = socket.getOutputStream();
//            outputStream.write(encodedMessageBytes);
//            outputStream.flush();
////            String response = in.readLine();
////            System.out.println(response);
//        }
//
//
//
//
//    }

}



