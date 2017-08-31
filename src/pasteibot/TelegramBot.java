/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pasteibot;

import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import com.pi4j.io.gpio.GpioPinDigitalOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import static melektro.ExtAPIs.GetIss;
import static melektro.ExtAPIs.GetIssWhen;
import static melektro.ExtAPIs.GetPublicIp;
import static melektro.LoadProperty.LoadProperty;
import static melektro.LogsFormatter.Log;
import org.telegram.telegrambots.api.methods.send.SendPhoto;
import org.json.*;
import static pasteibot.PasteiBot.BotToken;
import static pasteibot.PasteiBot.BotUsername;

/**
 *
 * @author Marius
 */
public class TelegramBot extends TelegramLongPollingBot {

    static Logger logger = null;
    private final GpioPinDigitalOutput botPin;
    private static String proxyToUse;
    private static String proxyPort;
    private static String Lat;
    private static String Lon;
    private static String user_id_Config;

    TelegramBot(GpioPinDigitalOutput pin, Logger LOGGER, String proxy, String proxyport, String lat, String lon) {
        botPin = pin;
        logger = LOGGER;
        proxyToUse = proxy;
        proxyPort = proxyport;
        Lat = lat;
        Lon = lon;
    }

    private static void GetProperties() {
        Properties prop = new Properties();
        InputStream input = null;

        File file = new File("TelegramBot.Admin");

        if (!file.exists()) {
            SetProperties();
            Log("Please configure the properties in the 'TelegramBot.Admin' file.");
            System.exit(0);
        } else {
            try {
                input = new FileInputStream(file);
                prop.load(input);

                user_id_Config = LoadProperty(prop, "user_id", "");

            } catch (IOException e) {
                Log("Exception: " + e.getMessage());
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                        Log("Exception: " + e.getMessage());
                    }
                }
            }
        }
    }

    private static void SetProperties() {
        Properties prop = new Properties();
        OutputStream output = null;

        try {
            output = new FileOutputStream("TelegramBot.Admin");
            prop.setProperty("user_id", "");

            prop.store(output, null);

        } catch (IOException e) {
            Log("Exception: " + e.getMessage());
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    Log("Exception: " + e.getMessage());
                }
            }
        }
    }

    public void sendImageUploadingAFile(String filePath, String chatId) {
        // Create send method
        SendPhoto sendPhotoRequest = new SendPhoto();
        // Set destination chat id
        sendPhotoRequest.setChatId(chatId);
        // Set the photo file as a new photo (You can also use InputStream with a method overload)
        sendPhotoRequest.setNewPhoto(new File(filePath));
        try {
            // Execute the method
            sendPhoto(sendPhotoRequest);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        String tmpMessage = "";
        boolean bExit = false;
        // We check if the update has a message and the message has text
        if (update.hasMessage() && update.getMessage().hasText()) {
            SendMessage message = new SendMessage() // Create a SendMessage object with mandatory fields
                    .setChatId(update.getMessage().getChatId())
                    .setText(update.getMessage().getText());
            try {
                Integer user_id = update.getMessage().getFrom().getId();
                Log("user_id=" + user_id);
                GetProperties();
                String txt = message.getText();
                if (null == txt.toUpperCase()) {
                    String sTxt = txt + " received - nothing to execute";
                    Log(sTxt);
                    message.setText(sTxt);
                } else {
                    switch (txt.toUpperCase()) {
                        case "ON":
                            if (user_id.toString().equals(user_id_Config)) {
                                botPin.high();
                                Log("Should be On");
                            }
                            break;
                        case "OFF":
                            if (user_id.toString().equals(user_id_Config)) {
                                botPin.low();
                                Log("Should be Off");
                            }
                            break;
                        case "IP":
                            if (user_id.toString().equals(user_id_Config)) {
                                tmpMessage = "Public Ip Address is: " + GetPublicIp(proxyToUse, proxyPort);
                                message.setText(tmpMessage);
                                Log(tmpMessage);
                            }
                            break;
                        case "EXIT":
                            tmpMessage = "Exiting application";
                            Log(tmpMessage);
                            message.setText(tmpMessage);
                            bExit = true;
                            break;
                        case "/START":
                            message.setText("Bot is started :-)");
                            break;
                        case "PIC":
                            try {
                                Runtime.getRuntime().exec("fswebcam -r 1280x720 --no-banner t1.jpg");
                                Log("Waithing to save picture...");
                                Thread.sleep(2000);
                                Log("Sending...");
                                //message.setText("Sending picture ... ");
                                sendImageUploadingAFile("t1.jpg", update.getMessage().getChatId().toString());
                                Log("Picture sent");
                            } catch (IOException | InterruptedException e) {
                                Log("Exception taking photo: " + e.getMessage());
                            }
                            break;
                        case "ISS":
                            String issJson = GetIss(proxyToUse, proxyPort);
                            tmpMessage = "ISS Json=" + issJson + "\r\n\r\n";
                            Log(tmpMessage);
                            JSONObject jsonObj = new JSONObject(issJson);
                            String lat = jsonObj.getJSONObject("iss_position").getString("latitude");
                            String lon = jsonObj.getJSONObject("iss_position").getString("longitude");
                            Long tme = jsonObj.getLong("timestamp");
                            Log("Lat=" + lat + ", Lon=" + lon + ", time=" + tme);

                            String when = GetIssWhen(proxyToUse, proxyPort, Lat, Lon, "5");
                            jsonObj = new JSONObject(when);
                            Log("When="+when);

                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                            int duration;
                            long timeStamp;
                            String date;
                            JSONArray arr = jsonObj.getJSONArray("response");
                            Log("number of records=" + arr.length());
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject rec = arr.getJSONObject(i);
                                timeStamp = rec.getLong("risetime");
                                duration = rec.getInt("duration");
                                date = Instant
                                        .ofEpochSecond(timeStamp)
                                        .atZone(ZoneId.of("GMT+2"))
                                        .format(formatter);
                                tmpMessage += "risetime=" + date + ", duration=" + duration + "\r\n";
                                Log("risetime=" + date + ", duration=" + duration);
                            }
                            message.setText(tmpMessage);
                            break;

                        default:
                            String sTxt = txt + " received - nothing to execute";
                            Log(sTxt);
                            message.setText(sTxt);
                            break;
                    }
                }
                execute(message);
                if (bExit) {
                    System.exit(0);
                }
            } catch (TelegramApiException e) {
                e.printStackTrace();
            } catch (Exception ex) {
                Logger.getLogger(TelegramBot.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public String getBotUsername() {
        return PasteiBot.BotUsername;
    }

    @Override
    public String getBotToken() {
        return PasteiBot.BotToken;
    }

}
