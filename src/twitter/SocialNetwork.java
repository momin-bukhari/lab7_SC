package twitter;


import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
 
public class SocialNetwork {
 
    /**
     * An @-mention. The lookbehind rejects an '@' that directly follows a username
     * character, so an email address such as bert@sesame.org is not a mention.
     * Group 1 captures the username without the '@'.
     */
    private static final Pattern MENTION =
            Pattern.compile("(?<![A-Za-z0-9_])@([A-Za-z0-9_]+)");
 
    /**
     * Builds the follows graph from tweets (guessFollowsGraph, Q1 and Q2).
     * Precondition: tweets is non-null and contains no null elements.
     */
    public static Map<String, Set<String>> guessFollowsGraph(List<Tweet> tweets) {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        for (Tweet tweet : tweets) {
            Set<String> mentioned = mentionedUsers(tweet.getText());
            if (mentioned.isEmpty()) {
                continue;                                    // Q1: no mention, no edge
            }
            followsGraph
                    .computeIfAbsent(normalize(tweet.getAuthor()), k -> new HashSet<>())
                    .addAll(mentioned);                      // Q2: record every mention
        }
        return followsGraph;
    }
 
    /**
     * Returns users with at least one follower, most-followed first
     * (influencers, Q1 and Q2).
     * Precondition: followsGraph is non-null.
     */
    public static List<String> influencers(Map<String, Set<String>> followsGraph) {
        // Each pair (follower a, followee x) gives x one more follower.
        Map<String, Integer> followerCount = new HashMap<>();
        for (Set<String> followees : followsGraph.values()) {
            for (String followee : followees) {
                followerCount.merge(followee, 1, Integer::sum);
            }
        }
        // Keys exist only for users with at least one follower (Q1).
        List<String> influencers = new ArrayList<>(followerCount.keySet());
        // Sort by follower count, descending (Q2).
        influencers.sort((a, b) -> Integer.compare(followerCount.get(b), followerCount.get(a)));
        return influencers;
    }
 
    /** Returns the distinct, lower-cased usernames mentioned in one tweet. */
    private static Set<String> mentionedUsers(String text) {
        Set<String> users = new HashSet<>();
        Matcher m = MENTION.matcher(text);
        while (m.find()) {
            users.add(normalize(m.group(1)));
        }
        return users;
    }
 
    /** Twitter usernames are case-insensitive, so every username is stored in lower case. */
    private static String normalize(String username) {
        return username.toLowerCase(Locale.ROOT);
    }
}
 