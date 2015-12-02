/*
 * Copyright 2015 Dirk Franssen.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package messenger;

import com.ditavision.messengerengine.Configuration;
import com.ditavision.messengerengine.MessengerEngine;
import com.ditavision.messengerengine.MessengerEngineException;
import com.ditavision.messengerengine.mmp.response.MMPRecipientStatus;
import com.ditavision.messengerengine.mmp.response.MMPStatusReportDetail;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * CLI client using the messenger-engine (https://github.com/dfranssen/messenger-engine).
 * Credits to Adam Bien for the CLI approach inspiration (https://github.com/AdamBien/loadr).
 * 
 * @author dfranssen
 */
public class App {
    
    static Configuration CONFIG = new Configuration();

    public static void main(String[] args) {
        if (args == null || args.length < 1) {
            usage();
            return;
        }
        Map<String, String> arguments = arrayToMap(args);
        
        Arguments action = argumentsToAction(arguments);
        
        switch (action) {
            case HELP_ACTION:
                if (arguments.containsKey(Arguments.INIT_ACTION.argumentName())) {
                    registerUsage();
                } else {
                    if (arguments.containsKey(Arguments.STATUS_ACTION.argumentName())) {
                        statusUsage();
                    } else {
                        sendUsage();
                    }
                }
                break;
            case INIT_ACTION:
                register(
                        arguments.get(Arguments.SERVER.argumentName()), 
                        arguments.get(Arguments.USER.argumentName()), 
                        arguments.get(Arguments.EMAIL.argumentName()));
                break;
            case STATUS_ACTION:
                getStatus(
                        arguments.get(Arguments.SERVER.argumentName()), 
                        arguments.get(Arguments.USER.argumentName()), 
                        arguments.get(Arguments.PASSWORD.argumentName()),
                        arguments.get(Arguments.MESSAGE_ID.argumentName()));
                break;
            default:
                sendMessage(
                        arguments.get(Arguments.SERVER.argumentName()), 
                        arguments.get(Arguments.USER.argumentName()), 
                        arguments.get(Arguments.PASSWORD.argumentName()),
                        arguments.get(Arguments.MESSAGE.argumentName()),
                        arguments.get(Arguments.TO.argumentName()));
                break;
        }
        exit(0);
    }
    
    static void register(String serverUrl, String userMSISDN, String userEmail) {
        String user = getValue(userMSISDN, CONFIG.getMsisdn(), "MSISDN");
        String email = getValue(userEmail, null, "email");
        String server = nullOrEmpty(serverUrl) ? CONFIG.getUrl() : serverUrl;
        MessengerEngine engine = new MessengerEngine();
        System.out.printf("Trying to register user '%s' with email '%s' at server '%s'\n", user, email, server);
        try {
            engine.startRegistration(server, user, email);
        } catch (MessengerEngineException ex) {
            System.out.println("Error during registration: " + ex.getCode() + " - " + ex.getMessage());
            exit(1);
        }
        System.out.print("Registration initialization succeeded, enter pincode (received by SMS): ");
        String pincode = readInput();
        try {
            String password = engine.verifyRegistration(server, user, pincode);
            System.out.println("Password to be used for sending messages: " + password);
        } catch (MessengerEngineException ex) {
            System.out.println("Error during pincode verification: " + ex.getCode() + " - " + ex.getMessage());
            exit(1);
        }
    }
    
    static void sendMessage(String serverUrl, String userMSISDN, String userPassword, String message, String recipients) {
        String user = getValue(userMSISDN, CONFIG.getMsisdn(), "MSISDN");
        String password = getValue(userPassword, CONFIG.getPassword(), "password");
        String msg = getValue(message, null, "message");
        String to = getValue(recipients, null, "recipient(s) (comma separated)");
        List<String> toList = split(to);
        String server = nullOrEmpty(serverUrl) ? CONFIG.getUrl() : serverUrl;
        System.out.printf("Trying to send message '%s' from user '%s' to recipients '%s' at server '%s'\n", msg, user, to, server);
        String msgId = null;
        try {
            msgId = new MessengerEngine().sendMessage(server, user, password, msg, toList);
        } catch (MessengerEngineException ex) {
            System.out.println("Error during sending message: " + ex.getCode() + " - " + ex.getMessage());
            exit(1);
        }
        System.out.println("Send message succeeded. Message id: " + msgId);
    }
    
    static void getStatus(String serverUrl, String userMSISDN, String userPassword, String messageIds) {
        String user = getValue(userMSISDN, CONFIG.getMsisdn(), "MSISDN");
        String password = getValue(userPassword, CONFIG.getPassword(), "password");
        String ids = getValue(messageIds, null, "message id(s) (Comma-separated)");
        List<String> idList = split(ids);
        String server = nullOrEmpty(serverUrl) ? CONFIG.getUrl() : serverUrl;
        System.out.printf("Trying to retrieve the status for message id(s) '%s' sent by user '%s' at server '%s'\n", ids, user, server);
        List<MMPStatusReportDetail> statusReports = new ArrayList<>();
        try {
            statusReports = new MessengerEngine().statusReports(server, user, password, idList);
        } catch (MessengerEngineException ex) {
            System.out.println("Error requesting a status report: " + ex.getCode() + " - " + ex.getMessage());
            exit(1);
        }
        System.out.printf("\nFound %d message(s) according the given id(s).\n\n", statusReports.size());
        System.out.format("%-15s%-15s%-15s%s\n", "MessageId", "Recipient", "StatusId", "Status");
        System.out.format("%-15s%-15s%-15s%s\n", "---------", "---------", "--------", "------");
        for (MMPStatusReportDetail statusReport : statusReports) {
            List<MMPRecipientStatus> recipients = statusReport.getRecipients();
            recipients.stream().forEach((recipient) -> {
                System.out.format("%-15s%-15s%-15s%s\n", 
                        statusReport.getMessageId(),
                        recipient.getMsisdn(),
                        recipient.getStatusId(),
                        recipient.getStatus());
            });
        }
        System.out.println("");
    }

    static ArrayList split(String to) {
        return to == null ? null : new ArrayList(Arrays.asList(to.split(",")));
    }
    
    static String getValue(String originalValue, String defaultValue, String name) {
        String result = nullOrEmpty(originalValue) ? defaultValue : originalValue;
        if (nullOrEmpty(result)) {
            System.out.printf("Please provide a %s: ", name);
            result = readInput();
        }
        return result;
    }
    
    static boolean nullOrEmpty(String value) {
        Optional<String> ostr = Optional.ofNullable(value).filter(s -> !s.isEmpty());
        return !ostr.isPresent();
    }
    
    static Map<String, String> arrayToMap(String args[]) {
        Map<String, String> arguments = new HashMap<>();

        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].startsWith("-")) {
                arguments.put(args[i], args[i + 1]);
            }
        }
        
        Optional<String> init = Arrays.stream(args).
                filter(a -> a.equals(Arguments.INIT_ACTION.argumentName())).
                findFirst();
        if (init.isPresent()) {
            arguments.put(init.get(), "");
        }
        
        Optional<String> status = Arrays.stream(args).
                filter(a -> a.equals(Arguments.STATUS_ACTION.argumentName())).
                findFirst();
        if (status.isPresent()) {
            arguments.put(status.get(), "");
        }
        
        Optional<String> help = Arrays.stream(args).
                filter(a -> a.equals(Arguments.HELP_ACTION.argumentName())).
                findFirst();

        if (help.isPresent()) {
            arguments.put(help.get(), "");
        }
        
        return arguments;
    }
    
    static Arguments argumentsToAction(Map<String, String> arguments) {
        if (arguments.containsKey(Arguments.HELP_ACTION.argumentName())) {
            return Arguments.HELP_ACTION;
        }
        if (arguments.containsKey(Arguments.INIT_ACTION.argumentName())) {
            return Arguments.INIT_ACTION;
        }
        if (arguments.containsKey(Arguments.STATUS_ACTION.argumentName())) {
            return Arguments.STATUS_ACTION;
        }
        return Arguments.SEND_ACTION;
    }
    
    static void usage() {
        System.out.println("Usage:");
        System.out.println("------");
        System.out.println("java -jar messenger-cli.jar --init -h   : for more directions to initialize your phone number with a MMP server.");
        System.out.println("java -jar messenger-cli.jar --send -h   : for more directions to send messages.");
        System.out.println("java -jar messenger-cli.jar --status -h : for more directions to get the status for a specific message id.");
    }
    
    static void registerUsage() {
        System.out.println("Usage:");
        System.out.println("------");
        System.out.println("java -jar messenger-cli.jar --init [-s MMP_SERVER_URI] [-u USER_PHONE_NR] [-e USER_EMAIL]\n");
        System.out.println("-s: Optional MMP server uri, default is '" + CONFIG.getUrl() + "'.");
        System.out.println("    This can also be set via an environment variable named '" + Configuration.URL_ENV_KEY + "'\n"); 
        System.out.println("-u: Optional phone number for which a registration will be done.");
        System.out.println("    This can also be set via an environment variable named '" + Configuration.MSISDN_ENV_KEY + "'.");
        System.out.println("    If missing, it will be requested as input.\n");
        System.out.println("-e: Optional email address of the user in case of 'replytoinbox' (future release).");
        System.out.println("    If missing, it will be requested as input.\n");
    }
    
    static void sendUsage() {
        System.out.println("Usage:");
        System.out.println("------");
        System.out.println("java -jar messenger-cli.jar [--send] [-s MMP_SERVER_URI] [-u USER_PHONE_NR] [-p PASSWORD] -m MESSAGE -t TO_RECEPIENTS\n");
        System.out.println("-s: Optional MMP server uri, default is '" + CONFIG.getUrl() + "'.");
        System.out.println("    This can also be set via an environment variable named '" + Configuration.URL_ENV_KEY + "'\n"); 
        System.out.println("-u: Optional phone number for which a registration will be done.");
        System.out.println("    This can also be set via an environment variable named '" + Configuration.MSISDN_ENV_KEY + "'.");
        System.out.println("    If missing, it will be requested as input.\n");
        System.out.println("-p: Optional password (received during initialisation).");
        System.out.println("    This can also be set via an environment variable named '" + Configuration.PWD_ENV_KEY + "'.");
        System.out.println("    If missing, it will be requested as input.\n");
        System.out.println("-m: The message to be sent, encapsulated between double quotes\n");
        System.out.println("-t: Phone number(s) to which the message will be sent.");
        System.out.println("    Comma-seperated for multiple recipients. E.g. +32495123456,+32495654321\n");
    }
    
    static void statusUsage() {
        System.out.println("Usage:");
        System.out.println("------");
        System.out.println("java -jar messenger-cli.jar --status [-s MMP_SERVER_URI] [-u USER_PHONE_NR] [-p PASSWORD] -id MESSAGE_IDS\n");
        System.out.println("-s:  Optional MMP server uri, default is '" + CONFIG.getUrl() + "'.");
        System.out.println("     This can also be set via an environment variable named '" + Configuration.URL_ENV_KEY + "'\n"); 
        System.out.println("-u:  Optional phone number for which a registration will be done.");
        System.out.println("     This can also be set via an environment variable named '" + Configuration.MSISDN_ENV_KEY + "'.");
        System.out.println("     If missing, it will be requested as input.\n");
        System.out.println("-p:  Optional password (received during initialisation).");
        System.out.println("     This can also be set via an environment variable named '" + Configuration.PWD_ENV_KEY + "'.");
        System.out.println("     If missing, it will be requested as input.\n");
        System.out.println("-id: The message id to get the status for (returned by the send operation)");
        System.out.println("     Comma-seperated for multiple message ids. E.g. 123456,123457\n");
    }
    
    static void exit(int code) {
        new SystemExitHelper().exit(code);
    }
    
    static String readInput() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            return br.readLine();
        } catch (IOException ex) {
            System.out.println("Error reading input: " + ex);
            exit(1);
        }
        return null;
    }
}