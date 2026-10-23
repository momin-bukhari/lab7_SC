package twitter;
import static org.junit.Assert.*;

import java.time.Instant;
import java.util.*;
import org.junit.Test;

public class SocialNetworkTest {

    // ---------- helpers ----------

    /** Builds a tweet with a fixed timestamp. The methods under test ignore the timestamp. */
    private static Tweet tweet(String author, String text) {
        return new Tweet(0L, author, text, Instant.parse("2026-10-23T09:00:00Z"));
    }

    /** Builds a mutable set from the given names. */
    private static Set<String> setOf(String... names) {
        return new HashSet<>(Arrays.asList(names));
    }

    /**
     * Returns the users that {@code user} follows. A missing key and an empty set
     * both mean "follows nobody" (guessFollowsGraph, Q3).
     */
    private static Set<String> followeesOf(Map<String, Set<String>> graph, String user) {
        return graph.getOrDefault(user, Collections.emptySet());
    }

    // ---------- guessFollowsGraph: required tests 1-5 ----------

    @Test // Test 1: empty list gives empty graph
    public void emptyTweetListGivesEmptyGraph() {
        assertTrue(SocialNetwork.guessFollowsGraph(Collections.emptyList()).isEmpty());
    }

    @Test // Test 2: tweets without mentions add no relationships
    public void tweetsWithoutMentionsAddNoFollowRelationships() {
        List<Tweet> tweets = Arrays.asList(
                tweet("ernie", "Hello, world!"),
                tweet("bert", "Nobody to mention here"));
        Map<String, Set<String>> graph = SocialNetwork.guessFollowsGraph(tweets);
        // Underdetermined: an empty entry is allowed, but no entry may contain a followee.
        for (Set<String> followees : graph.values()) {
            assertTrue(followees.isEmpty());
        }
        assertTrue(followeesOf(graph, "ernie").isEmpty());
        assertTrue(followeesOf(graph, "bert").isEmpty());
    }

    @Test // Test 3: a single mention is recorded for the author
    public void singleMentionAddsAuthorToGraph() {
        Map<String, Set<String>> graph = SocialNetwork.guessFollowsGraph(
                Collections.singletonList(tweet("ernie", "@bert is great")));
        assertTrue(graph.containsKey("ernie"));
        assertEquals(setOf("bert"), followeesOf(graph, "ernie"));
    }

    @Test // Test 4: every mention in one tweet is linked to the author
    public void multipleMentionsInOneTweetAreAllLinkedToAuthor() {
        Map<String, Set<String>> graph = SocialNetwork.guessFollowsGraph(
                Collections.singletonList(tweet("ernie", "@bert and @elmo, say hi to @cookie")));
        assertEquals(setOf("bert", "elmo", "cookie"), followeesOf(graph, "ernie"));
    }

    @Test // Test 5: mentions across several tweets by one author accumulate
    public void repeatedMentionsFromOneUserAreAllCaptured() {
        List<Tweet> tweets = Arrays.asList(
                tweet("ernie", "@bert hi"),
                tweet("ernie", "@elmo hey"),
                tweet("ernie", "@bert again"));
        Map<String, Set<String>> graph = SocialNetwork.guessFollowsGraph(tweets);
        assertEquals(setOf("bert", "elmo"), followeesOf(graph, "ernie"));
    }

    // ---------- influencers: required tests 6-10 ----------

    @Test // Test 6: empty graph gives empty influencer list
    public void emptyGraphGivesEmptyList() {
        assertTrue(SocialNetwork.influencers(new HashMap<>()).isEmpty());
    }

    @Test // Test 7: a user with no followers is not an influencer
    public void singleUserWithoutFollowersGivesNoInfluencers() {
        Map<String, Set<String>> graph = new HashMap<>();
        graph.put("ernie", setOf());                       // ernie follows nobody
        assertTrue(SocialNetwork.influencers(graph).isEmpty());
    }

    @Test // Test 8: a single influencer is identified
    public void singleInfluencerIdentified() {
        Map<String, Set<String>> graph = new HashMap<>();
        graph.put("ernie", setOf("bert"));                 // bert has one follower
        assertEquals(Collections.singletonList("bert"), SocialNetwork.influencers(graph));
    }

    @Test // Test 9: influencers are ordered by follower count, most first
    public void multipleInfluencersSortedByFollowerCount() {
        Map<String, Set<String>> graph = new HashMap<>();
        graph.put("ernie",  setOf("bert", "elmo"));
        graph.put("grover", setOf("bert", "elmo", "cookie"));
        graph.put("oscar",  setOf("bert"));
        // Followers: bert = 3, elmo = 2, cookie = 1
        assertEquals(Arrays.asList("bert", "elmo", "cookie"), SocialNetwork.influencers(graph));
    }

    @Test // Test 10: tied influencers are both reported; their order is not checked
    public void tiedInfluenceUsersAreBothReturned() {
        Map<String, Set<String>> graph = new HashMap<>();
        graph.put("ernie",  setOf("bert", "elmo"));
        graph.put("grover", setOf("bert", "elmo"));
        // bert and elmo each have 2 followers. Their relative order is underdetermined.
        List<String> result = SocialNetwork.influencers(graph);
        assertEquals(2, result.size());
        assertEquals(setOf("bert", "elmo"), new HashSet<>(result));
    }

    // ---------- extra edge-case tests (E1-E4) ----------

    @Test // E1: a duplicate mention inside one tweet counts once
    public void duplicateMentionInSameTweetCountsOnce() {
        Map<String, Set<String>> graph = SocialNetwork.guessFollowsGraph(
                Collections.singletonList(tweet("ernie", "@bert @bert hi")));
        assertEquals(setOf("bert"), followeesOf(graph, "ernie"));
        assertEquals(Collections.singletonList("bert"), SocialNetwork.influencers(graph));
    }

    @Test // E2: punctuation after a mention is not part of the username
    public void punctuationAfterMentionIsNotPartOfUsername() {
        Map<String, Set<String>> graph = SocialNetwork.guessFollowsGraph(
                Collections.singletonList(tweet("ernie", "Thanks @bert!")));
        assertEquals(setOf("bert"), followeesOf(graph, "ernie"));
    }

    @Test // E3: an email address is not a mention
    public void emailAddressIsNotAMention() {
        Map<String, Set<String>> graph = SocialNetwork.guessFollowsGraph(
                Collections.singletonList(tweet("ernie", "write to bert@sesame.org")));
        assertTrue(followeesOf(graph, "ernie").isEmpty());
    }

    @Test // E4: usernames differing only in case are the same user
    public void mentionsDifferingOnlyInCaseAreTheSameUser() {
        Map<String, Set<String>> graph = SocialNetwork.guessFollowsGraph(Arrays.asList(
                tweet("Ernie", "@Bert hi"),
                tweet("ernie", "@BERT again")));
        assertEquals(setOf("ernie"), graph.keySet());
        assertEquals(setOf("bert"), followeesOf(graph, "ernie"));
    }
}