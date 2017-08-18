/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pasteibot;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import java.util.logging.Level;
import melektro.LogsFormatter;
import static melektro.PublicIPAddress.Log;
import static melektro.PublicIPAddress.SetLogger;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiException;

/**
 *
 * @author Marius
 */
public class PasteiBot {

    public static String ProxyToUse = "";
    public static String ProxyPortToUse = "";
    public static String BotUsername;
    public static String BotToken;
    private static final String TestMode = "false";
    private static boolean bTestMode = false;
    
    private static void SetProperties() {
        Properties prop = new Properties();
        OutputStream output = null;

        try {
            output = new FileOutputStream("PasteiTelegramBot.config.properties");
            prop.setProperty("BotUsername", "YourBotUsername");
            prop.setProperty("BotToken", "YourBotToken");
            prop.setProperty("TestMode", "false");
            prop.store(output, null);

        } catch (IOException e) {
            Log("Excetion: "+e.getMessage());
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    Log("Excetion: "+e.getMessage());
                }
            }
        }
    }

    private static String LoadProperty(Properties prop, String property, String defaultvalue) {
        String PropertyValue = prop.getProperty(property);
        if (PropertyValue != null) {
            return PropertyValue;
        } else {
            return defaultvalue;
        }
    }

    private static void GetProperties() {
        Properties prop = new Properties();
        InputStream input = null;

        File file = new File("PasteiTelegramBot.config.properties");

        if (!file.exists()) {
            SetProperties();
            Log("Please configure the properties in the 'PasteiTelegramBot.config.properties' file.");
            System.exit(0);
        } else {
            try {
                input = new FileInputStream(file);
                prop.load(input);

                BotUsername = LoadProperty(prop, "BotUsername", "YourBotUsername");
                BotToken = LoadProperty(prop, "BotToken", "YourBotToken");
                ProxyToUse = LoadProperty(prop, "ProxyToUse", "");
                ProxyPortToUse = LoadProperty(prop, "ProxyPortToUse", "");
                bTestMode = !TestMode.equals("false");

            } catch (IOException e) {
                Log("Excetion: "+e.getMessage());
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (IOException e) {
                        Log("Excetion: "+e.getMessage());
                    }
                }
            }
        }
    }

    public static String GetPublicIp() throws Exception {
        URL whatismyip = new URL("http://checkip.amazonaws.com/");
        URLConnection connection;
        if (ProxyToUse.isEmpty()) {
            connection = whatismyip.openConnection();
        } else {
            Log("  proxy=" + ProxyToUse);
            Log("Using proxy");
            Log("  port=" + ProxyPortToUse);
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ProxyToUse, Integer.parseInt(ProxyPortToUse)));
            connection = whatismyip.openConnection(proxy);
        }
        connection.addRequestProperty("Protocol", "Http/1.1");
        connection.addRequestProperty("Connection", "keep-alive");
        connection.addRequestProperty("Keep-Alive", "1000");
        connection.addRequestProperty("User-Agent", "Web-Agent");

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        String ip = in.readLine();
        return ip;
    }


    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        SetLogger(new LogsFormatter().setLogging(Level.ALL));
        GetProperties();
        final GpioController gpio = GpioFactory.getInstance();
        final GpioPinDigitalOutput pin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "MyLED", PinState.LOW);
        final GpioPinDigitalOutput pin2 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03, "MyLED", PinState.LOW);
        pin.setShutdownOptions(true, PinState.LOW);
        pin2.setShutdownOptions(true, PinState.LOW);


        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Log("Terminating");
            gpio.shutdown();
        }));

        
        ApiContextInitializer.init();
        TelegramBotsApi botsApi = new TelegramBotsApi();

        try {
            botsApi.registerBot(new TelegramBot(pin2));
            Log("Listening ....");
        } catch (TelegramApiException e) {
            Log("Excetion: "+e.getMessage());
        }
    }

}
