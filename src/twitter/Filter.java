package twitter;

import java.util.List;
import java.time.Instant;
import java.util.ArrayList;

public class Filter {

    public static List<Tweet> writtenBy(List<Tweet> tweets, String username) {
     List<Tweet> result = new ArrayList<>();

        for (Tweet tweet : tweets) {
            if (tweet.getAuthor().equalsIgnoreCase(username)) {
                result.add(tweet);
            }
        }

        return result;
    }

    public static List<Tweet> inTimespan(List<Tweet> tweets, Timespan timespan) {
     List<Tweet> result = new ArrayList<>();

        Instant start = timespan.getStart();
        Instant end = timespan.getEnd();

        for (Tweet tweet : tweets) {
            Instant timestamp = tweet.getTimestamp();

            if (!timestamp.isBefore(start) && !timestamp.isAfter(end)) {
                result.add(tweet);
            }
        }

        return result;
    }

    public static List<Tweet> containing(List<Tweet> tweets, List<String> words) {
     List<Tweet> result = new ArrayList<>();

        for (Tweet tweet : tweets) {
            String[] tweetWords = tweet.getText().split(" ");
            boolean matches = false;

            for (String tweetWord : tweetWords) {
                for (String word : words) {
                    if (tweetWord.equalsIgnoreCase(word)) {
                        matches = true;
                        break;
                    }
                }
                if (matches) {
                    break;
                }
            }
            if (matches) {
                result.add(tweet);
            }
        }
        return result;
    }
}