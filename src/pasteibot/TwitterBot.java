/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pasteibot;

//import twitter4j.Twitter;
//import twitter4j.TwitterFactory;
//import twitter4j.conf.ConfigurationBuilder;
import twitter4j.conf.*;
import twitter4j.*;

/**
 *
 * @author Marius
 */
public class TwitterBot {

    public static Twitter TwitterSetup(String TwitterKey, String TwitterKeySecret, String TwitterToken, String TwitterTokenSecret) {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(TwitterKey)
                .setOAuthConsumerSecret(TwitterKeySecret)
                .setOAuthAccessToken(TwitterToken)
                .setOAuthAccessTokenSecret(TwitterTokenSecret);
        TwitterFactory tf = new TwitterFactory(cb.build());
        Twitter twitter = tf.getInstance();
        return twitter;
    }
}
