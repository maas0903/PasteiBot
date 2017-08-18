/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pasteitelegrambot;

import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import com.pi4j.io.gpio.GpioPinDigitalOutput;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.telegram.telegrambots.api.methods.send.SendPhoto;

/**
 *
 * @author Marius
 */
public class TelegramBot extends TelegramLongPollingBot {

    private final GpioPinDigitalOutput botPin;

    TelegramBot(GpioPinDigitalOutput pin) {
        botPin = pin;
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
        // We check if the update has a message and the message has text
        if (update.hasMessage() && update.getMessage().hasText()) {
            SendMessage message = new SendMessage() // Create a SendMessage object with mandatory fields
                    .setChatId(update.getMessage().getChatId())
                    .setText(update.getMessage().getText());
            try {
                String txt = message.getText();
                if (null == txt.toUpperCase()) {
                    String sTxt = txt + " received - nothing to execute";
                    System.out.println(sTxt);
                    message.setText(sTxt);
                } else switch (txt.toUpperCase()) {
                    case "ON":
                        botPin.high();
                        System.out.println("Should be On");
                        break;
                    case "OFF":
                        botPin.low();
                        System.out.println("Should be Off");
                        break;
                    case "IP":
                        message.setText("Public Ip Address is: " + PasteiBot.GetPublicIp());
                        break;
                    case "/START":
                        message.setText("Bot is started :-)");
                        break;
                    case "PIC":
                        try {
                            Runtime.getRuntime().exec("fswebcam -r 1280x720 --no-banner t1.jpg");
                            System.out.println("Waithing to save picture...");
                            Thread.sleep(2000);
                            System.out.println("Sending...");
                            //message.setText("Sending picture ... ");
                            sendImageUploadingAFile("t1.jpg", update.getMessage().getChatId().toString());
                            System.out.println("Picture sent");
                        } catch (IOException | InterruptedException e) {
                            System.out.println("Exception taking photo: " + e.getMessage());
                        }   break;
                    default:
                        String sTxt = txt + " received - nothing to execute";
                        System.out.println(sTxt);
                        message.setText(sTxt);
                        break;
                }
                execute(message);
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
