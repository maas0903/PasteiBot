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
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import static melektro.LogsFormatter.Log;
import org.telegram.telegrambots.api.methods.send.SendPhoto;

/**
 *
 * @author Marius
 */
public class TelegramBot extends TelegramLongPollingBot {
    static Logger logger = null;

    private final GpioPinDigitalOutput botPin;

    TelegramBot(GpioPinDigitalOutput pin, Logger LOGGER) {
        botPin = pin;
        logger = LOGGER;
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
                String txt = message.getText();
                if (null == txt.toUpperCase()) {
                    String sTxt = txt + " received - nothing to execute";
                    Log(sTxt);
                    message.setText(sTxt);
                } else switch (txt.toUpperCase()) {
                    case "ON":
                        botPin.high();
                        Log("Should be On");
                        break;
                    case "OFF":
                        botPin.low();
                        Log("Should be Off");
                        break;
                    case "IP":
                        tmpMessage = "Public Ip Address is: " + PasteiBot.GetPublicIp();
                        message.setText(tmpMessage);
                        Log(tmpMessage);
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
                        }   break;
                    default:
                        String sTxt = txt + " received - nothing to execute";
                        Log(sTxt);
                        message.setText(sTxt);
                        break;
                }
                execute(message);
                if (bExit)
                    System.exit(0);
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
