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
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import melektro.LogsFormatter;
import melektro.ExtAPIs;
import static melektro.LogsFormatter.Log;
import static melektro.MyWget.MyWget;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.generics.BotSession;
import static pasteibot.TwitterBot.TwitterSetup;
import twitter4j.*;

/**
 *
 * @author Marius
 */
public class PasteiBot {

    private static String ProxyToUse = "";
    private static String ProxyPortToUse = "";
    public static String BotUsername = "YourBotUsername";
    public static String BotToken = "YourBotToken";
    private static final String TestMode = "false";
    private static boolean bTestMode = false;
    private static String TwitterKey = "YourTwitterKey";
    private static String TwitterKeySecret = "YourTwitterKeySecret";
    private static String TwitterToken = "YourTwitterToken";
    private static String TwitterTokenSecret = "YourTwitterTokenSecret";
    private static String Bot = "Telegram";
    private static String Lattitude = "";
    private static String Longitude = "";

    private final String[] twitterCommand = {"ON", "OFF", "IP"};

    private static void SetProperties() {
        Properties prop = new Properties();
        OutputStream output = null;

        try {
            output = new FileOutputStream("PasteiBot.config.properties");
            prop.setProperty("BotType", Bot);
            prop.setProperty("BotUsername", BotUsername);
            prop.setProperty("BotToken", BotToken);
            prop.setProperty("TestMode", "false");
            prop.setProperty("TwitterKey", TwitterKey);
            prop.setProperty("TwitterKeySecret", TwitterKeySecret);
            prop.setProperty("TwitterToken", TwitterToken);
            prop.setProperty("TwitterTokenSecret", TwitterTokenSecret);
            prop.setProperty("Lattitude", Lattitude);
            prop.setProperty("Longitude", Longitude);

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

        File file = new File("PasteiBot.config.properties");

        if (!file.exists()) {
            SetProperties();
            Log("Please configure the properties in the 'PasteiBot.config.properties' file.");
            System.exit(0);
        } else {
            try {
                input = new FileInputStream(file);
                prop.load(input);

                Bot = LoadProperty(prop, "BotType", "Telegram");
                BotUsername = LoadProperty(prop, "BotUsername", "YourBotUsername");
                BotToken = LoadProperty(prop, "BotToken", "YourBotToken");
                ProxyToUse = LoadProperty(prop, "ProxyToUse", "");
                ProxyPortToUse = LoadProperty(prop, "ProxyPortToUse", "");
                bTestMode = !TestMode.equals("false");
                TwitterKey = LoadProperty(prop, "TwitterKey", "YourTwitterKey");
                TwitterKeySecret = LoadProperty(prop, "TwitterKeySecret", "YourTwitterKeySecret");
                TwitterToken = LoadProperty(prop, "TwitterToken", "YourTwitterToken");
                TwitterTokenSecret = LoadProperty(prop, "TwitterTokenSecret", "YourTwitterTokenSecret");
                Lattitude = LoadProperty(prop, "Lattitude", "50.88");
                Longitude = LoadProperty(prop, "Longitude", "4.70");

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
            SetProperties();
        }
    }

    public enum BotType {
        Telegram, Twitter
    };

    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     */
    public static void main(String[] args) throws InterruptedException, TwitterException, Exception {
        Logger logger = new LogsFormatter().setLogging(Level.ALL);
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

        BotType botType = BotType.Telegram;
        if (null == Bot) {
            botType = null;
        } else {
            switch (Bot.toLowerCase()) {
                case "telegram":
                    botType = BotType.Telegram;
                    break;
                case "twitter":
                    botType = BotType.Twitter;
                    break;
                default:
                    botType = null;
                    break;
            }
        }

        switch (botType) {
            case Telegram:
                ApiContextInitializer.init();
                TelegramBotsApi botsApi = new TelegramBotsApi();

                try {
                    BotSession registerBot = botsApi.registerBot(
                            new TelegramBot(pin2, logger, ProxyToUse, ProxyPortToUse, Lattitude, Longitude));
                    Log("Listening ....");
                } catch (TelegramApiException e) {
                    Log("Exception: " + e.getMessage());
                }
                break;

            case Twitter:
                if ("yourtwitterkey".equals(TwitterKey.toLowerCase())) {
                    Log("Twitter not setup");
                } else {
                    Twitter twitter = TwitterSetup(TwitterKey, TwitterKeySecret, TwitterToken, TwitterTokenSecret);

                    StatusListener statusListener = new StatusListener() {
                        @Override
                        public void onStatus(Status status) {
                            Log("@" + status.getUser().getScreenName() + " - " + status.getUser().getId() + " - " + status.getText());
                            String Command = status.getText().toUpperCase();
                            Command = Command.substring(14);
                            Log("Substring Command:" + Command);
                            if (null != Command) {
                                switch (Command) {
                                    case "ON":
                                        pin2.high();
                                        Log("Should be " + Command);
                                         {
                                            try {
                                                SendDirectMessage(twitter, status, "Should be " + Command);
                                            } catch (TwitterException ex) {
                                                Logger.getLogger(PasteiBot.class.getName()).log(Level.SEVERE, null, ex);
                                            }
                                        }
                                        //twitter.sendDirectMessage("@" + status.getId() SenderScreenName(), "Should be " + Command);
                                        break;
                                    case "OFF":
                                        pin2.low();
                                        Log("Should be " + Command);
                                         {
                                            try {
                                                SendDirectMessage(twitter, status, "Should be " + Command);
                                            } catch (TwitterException ex) {
                                                Logger.getLogger(PasteiBot.class.getName()).log(Level.SEVERE, null, ex);
                                            }
                                        }
                                        break;
                                    case "IP":
                                        String ip = null;
                                        try {
                                            ip = ExtAPIs.GetPublicIp(ProxyToUse, ProxyPortToUse);
                                            Log("Public IP Address=" + ip);
                                            SendDirectMessage(twitter, status, "Public IP Address=" + ip);
                                        } catch (Exception ex) {
                                            Log("Exception: " + ex.getMessage());
                                        }
                                        break;
                                    default:
                                        break;
                                }
                            }
                        }

                        @Override
                        public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
                            System.out.println("Got a status deletion notice id:" + statusDeletionNotice.getStatusId());
                        }

                        @Override
                        public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
                            System.out.println("Got track limitation notice:" + numberOfLimitedStatuses);
                        }

                        @Override
                        public void onScrubGeo(long userId, long upToStatusId) {
                            Log("Got scrub_geo event userId:" + userId + " upToStatusId:" + upToStatusId);

                        }

                        @Override
                        public void onStallWarning(StallWarning warning) {
                            System.out.println("Got stall warning:" + warning);
                        }

                        @Override
                        public void onException(Exception ex) {
                            ex.printStackTrace();
                        }

                        private void SendDirectMessage(Twitter twitter, Status status, String txt) throws TwitterException {
                            String id = "@" + status.getUser().getScreenName();
                            Log("Sending '" + txt + "' to " + id.toString());
                            twitter.sendDirectMessage(id, txt);
                        }
                    };

                    TwitterStream twitterStream = new TwitterStreamFactory(twitter.getConfiguration()).getInstance();
                    twitterStream.addListener(statusListener);
                    FilterQuery tweetFilterQuery = new FilterQuery();
                    //tweetFilterQuery.track( "Ip");
                    tweetFilterQuery.follow(48290676);
                    //tweetFilterQuery.language(new String[]{"en"});
                    //tweetFilterQuery.language("en");
                    twitterStream.filter(tweetFilterQuery);

                    /*
                    try {
                        Boolean messaged = false;
                        Paging paging = new Paging(1);
                        List<DirectMessage> messages;
                        do {
                            messages = twitter.getDirectMessages(paging);
                            for (DirectMessage message : messages) {
                                Log("From: @" + message.getSenderScreenName() + " id:" + message.getId() + " - " + message.getText());
                                if ("mariustn".equals(message.getSenderScreenName().toLowerCase())) {
                                    String Command = message.getText().toUpperCase();
                                    if ("ON".equals(Command)) {
                                        pin.high();
                                        Log("Should be " + Command);
                                        twitter.sendDirectMessage("@" + message.getSenderScreenName(), "Should be " + Command);
                                    } else if ("OFF".equals(Command)) {
                                        pin.low();
                                        Log("Should be " + Command);
                                        twitter.sendDirectMessage("@" + message.getSenderScreenName(), "Should be " + Command);
                                    } else if ("IP".equals(Command)) {
                                        String ip = GetPublicIp();
                                        twitter.sendDirectMessage("@" + message.getSenderScreenName(), "Public IP Address is " + ip);
                                        Log("Public IP Address=" + ip);
                                    }
                                    break;
                                }
                            }
                            paging.setPage(paging.getPage() + 1);
                        } while (messages.size() > 0 && paging.getPage() < 10);

                        Paging paging2 = new Paging(1);
                        List<DirectMessage> messages2;
                        do {
                            messages2 = twitter.getDirectMessages(paging2);
                            for (DirectMessage message : messages2) {
                                Log("destroying " + message.getId());
                                twitter.destroyDirectMessage(message.getId());
                            }
                            paging2.setPage(paging2.getPage() + 1);
                        } while (messages2.size() > 0 && paging2.getPage() < 10);

                        Log("done.");
                        System.exit(0);
                    } catch (TwitterException e) {
                        e.printStackTrace();
                        Log("Failed to get messages: " + e.getMessage());
                        System.exit(-1);
                    }
                     */

 /*
                    twitter.sendDirectMessage("@mariustn", "Hallo from Raspberry Pi 4");
                     */

 /*
                    twitter.updateStatus("@PasteiEen Hallo from Raspberry Pi 4");
                    Log("Twitter sent");
                     */
 /*
                    Query query = new Query("sandwich");
                    QueryResult tweets = twitter.search(query);
                    for (Status status : tweets.getTweets()) {
                        System.out.println("@" + status.getUser().getScreenName() + ":" + status.getText());
                    }
                     */
                }
                break;
            default:
                Log("Bot Not implemented");
                break;
        }

    }
}
