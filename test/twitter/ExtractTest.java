/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package twitter;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class ExtractTest {

    /*
     * Testing strategy
     *
     * getTimespan(tweets)
     *   Input space, partitioned on:
     *     number of tweets: 1, 2, many (> 2)
     *       (the empty list is not tested: the spec gives no valid Timespan for it)
     *     order of timestamps in the list: ascending, descending, shuffled
     *     timestamps: all distinct, some equal (including all equal)
     *   Output space, partitioned on:
     *     length of the timespan: zero (start == end), positive (start < end)
     *   Boundary cases: 1 tweet; all timestamps equal; the earliest/latest tweet
     *     is first, last, or in the middle of the list.
     *
     * getMentionedUsers(tweets)
     *   Input space, partitioned on:
     *     number of tweets: 1, many
     *     mentions per tweet: 0, 1, many
     *     position of the mention in the text: at the start, in the middle, at the end
     *     what is next to the "@": valid username char before it (e.g. an email
     *       address), nothing valid after it ("@" alone or "@" followed by a space),
     *       punctuation right after the name (e.g. "@bob,"), which is still a mention
     *     the same user mentioned: once, repeatedly, with different letter case
     *     characters in the username: letters only, digits, underscore, hyphen
     *   Output space, partitioned on:
     *     size of the returned set: 0, 1, many
     *   Mentions are compared case-insensitively (the spec allows any case in the
     *     result), so the tests lower-case the returned names before comparing.
     */

    private static final Instant d1 = Instant.parse("2016-02-17T10:00:00Z");
    private static final Instant d2 = Instant.parse("2016-02-17T11:00:00Z");
    private static final Instant d3 = Instant.parse("2016-02-17T12:00:00Z");

    private static final Tweet tweet1 = new Tweet(1, "alyssa", "is it reasonable to talk about rivest so much?", d1);
    private static final Tweet tweet2 = new Tweet(2, "bbitdiddle", "rivest talk in 30 minutes #hype", d2);
    private static final Tweet tweet3 = new Tweet(3, "carol", "no mentions here", d3);

    private static Tweet tweetWithText(long id, String text) {
        return new Tweet(id, "author" + id, text, d1);
    }

    private static Tweet tweetAt(long id, Instant time) {
        return new Tweet(id, "author" + id, "text", time);
    }

    /** Lower-cases every name so tests don't depend on the case the implementation returns. */
    private static Set<String> lowerCased(Set<String> names) {
        Set<String> result = new HashSet<>();
        for (String name : names) {
            result.add(name.toLowerCase());
        }
        return result;
    }

    private static Set<String> setOf(String... names) {
        return new HashSet<>(Arrays.asList(names));
    }

    private static Set<String> mentionedIn(String... texts) {
        long id = 100;
        Tweet[] tweets = new Tweet[texts.length];
        for (int i = 0; i < texts.length; i++) {
            tweets[i] = tweetWithText(id++, texts[i]);
        }
        return lowerCased(Extract.getMentionedUsers(Arrays.asList(tweets)));
    }

    @Test(expected=AssertionError.class)
    public void testAssertionsEnabled() {
        assert false; // make sure assertions are enabled with VM argument: -ea
    }

    // ---------------------------------------------------------------- getTimespan

    // 2 tweets, ascending order, distinct timestamps, positive-length span
    @Test
    public void testGetTimespanTwoTweets() {
        Timespan timespan = Extract.getTimespan(Arrays.asList(tweet1, tweet2));

        assertEquals("expected start", d1, timespan.getStart());
        assertEquals("expected end", d2, timespan.getEnd());
    }

    // 1 tweet, zero-length span
    @Test
    public void testGetTimespanOneTweet() {
        Timespan timespan = Extract.getTimespan(Arrays.asList(tweet1));

        assertEquals("expected start", d1, timespan.getStart());
        assertEquals("expected end", d1, timespan.getEnd());
    }

    // 2 tweets, descending order
    @Test
    public void testGetTimespanTwoTweetsDescending() {
        Timespan timespan = Extract.getTimespan(Arrays.asList(tweet2, tweet1));

        assertEquals("expected start", d1, timespan.getStart());
        assertEquals("expected end", d2, timespan.getEnd());
    }

    // many tweets, shuffled: earliest in the middle, latest first
    @Test
    public void testGetTimespanManyTweetsShuffled() {
        Timespan timespan = Extract.getTimespan(Arrays.asList(tweet3, tweet1, tweet2));

        assertEquals("expected start", d1, timespan.getStart());
        assertEquals("expected end", d3, timespan.getEnd());
    }

    // many tweets, ascending: earliest first, latest last
    @Test
    public void testGetTimespanManyTweetsAscending() {
        Timespan timespan = Extract.getTimespan(Arrays.asList(tweet1, tweet2, tweet3));

        assertEquals("expected start", d1, timespan.getStart());
        assertEquals("expected end", d3, timespan.getEnd());
    }

    // many tweets, all timestamps equal, zero-length span
    @Test
    public void testGetTimespanAllEqualTimestamps() {
        List<Tweet> tweets = Arrays.asList(tweetAt(10, d2), tweetAt(11, d2), tweetAt(12, d2));
        Timespan timespan = Extract.getTimespan(tweets);

        assertEquals("expected start", d2, timespan.getStart());
        assertEquals("expected end", d2, timespan.getEnd());
    }

    // many tweets, some timestamps equal to the extremes
    @Test
    public void testGetTimespanSomeEqualTimestamps() {
        List<Tweet> tweets = Arrays.asList(tweetAt(10, d1), tweetAt(11, d3), tweetAt(12, d1), tweetAt(13, d2), tweetAt(14, d3));
        Timespan timespan = Extract.getTimespan(tweets);

        assertEquals("expected start", d1, timespan.getStart());
        assertEquals("expected end", d3, timespan.getEnd());
    }

    // ----------------------------------------------------------- getMentionedUsers

    // 1 tweet, 0 mentions, output size 0
    @Test
    public void testGetMentionedUsersNoMention() {
        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet1));

        assertTrue("expected empty set", mentionedUsers.isEmpty());
    }

    // many tweets, 0 mentions (the "#hype" tweet has no "@" either)
    @Test
    public void testGetMentionedUsersManyTweetsNoMention() {
        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet1, tweet2, tweet3));

        assertTrue("expected empty set", mentionedUsers.isEmpty());
    }

    // empty list of tweets, output size 0
    @Test
    public void testGetMentionedUsersNoTweets() {
        Set<String> mentionedUsers = Extract.getMentionedUsers(Collections.<Tweet>emptyList());

        assertTrue("expected empty set", mentionedUsers.isEmpty());
    }

    // 1 mention, in the middle of the text, output size 1
    @Test
    public void testGetMentionedUsersOneMentionMiddle() {
        assertEquals(setOf("bob"), mentionedIn("hello @bob how are you"));
    }

    // 1 mention at the very start of the text
    @Test
    public void testGetMentionedUsersMentionAtStart() {
        assertEquals(setOf("bob"), mentionedIn("@bob hello"));
    }

    // 1 mention at the very end of the text
    @Test
    public void testGetMentionedUsersMentionAtEnd() {
        assertEquals(setOf("bob"), mentionedIn("hello @bob"));
    }

    // the whole text is one mention
    @Test
    public void testGetMentionedUsersMentionIsWholeText() {
        assertEquals(setOf("bob"), mentionedIn("@bob"));
    }

    // several mentions in one tweet, output size > 1
    @Test
    public void testGetMentionedUsersManyMentionsOneTweet() {
        assertEquals(setOf("bob", "carol", "dave"), mentionedIn("@bob and @carol and @dave"));
    }

    // mentions spread across many tweets
    @Test
    public void testGetMentionedUsersMentionsAcrossTweets() {
        assertEquals(setOf("bob", "carol"), mentionedIn("hi @bob", "no one here", "bye @carol"));
    }

    // same user mentioned repeatedly, in several tweets: appears once
    @Test
    public void testGetMentionedUsersSameUserRepeated() {
        assertEquals(setOf("bob"), mentionedIn("@bob @bob", "again @bob"));
    }

    // same user mentioned with different letter case: appears once
    @Test
    public void testGetMentionedUsersDifferentCaseCountsOnce() {
        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(
                tweetWithText(100, "hi @Bob"), tweetWithText(101, "hi @bOB")));

        assertEquals("expected the username only once", 1, mentionedUsers.size());
        assertEquals(setOf("bob"), lowerCased(mentionedUsers));
    }

    // username containing digits, underscore and hyphen
    @Test
    public void testGetMentionedUsersUsernameCharacters() {
        assertEquals(setOf("a_b-9"), mentionedIn("thanks @a_b-9 !"));
    }

    // punctuation right after the name is not part of the username
    @Test
    public void testGetMentionedUsersPunctuationAfterName() {
        assertEquals(setOf("bob", "carol"), mentionedIn("thanks @bob, and @carol."));
    }

    // hyphen is a valid username character, so it is part of the mentioned name
    @Test
    public void testGetMentionedUsersHyphenInsideName() {
        assertEquals(setOf("bob-smith"), mentionedIn("hello @bob-smith!"));
    }

    // "@" preceded by a hyphen (a valid username character): no mention
    @Test
    public void testGetMentionedUsersHyphenBeforeAtIsNotMention() {
        assertTrue("expected empty set", mentionedIn("x-@bob").isEmpty());
    }

    // "@" preceded by a valid username character (email address): no mention
    @Test
    public void testGetMentionedUsersEmailAddressIsNotMention() {
        assertTrue("expected empty set", mentionedIn("mail bitdiddle@mit.edu now").isEmpty());
    }

    // an email address and a real mention in the same tweet: only the real mention
    @Test
    public void testGetMentionedUsersEmailAndMention() {
        assertEquals(setOf("alyssa"), mentionedIn("bitdiddle@mit.edu cc @alyssa"));
    }

    // "@" alone, and "@" followed by a space: no username, no mention
    @Test
    public void testGetMentionedUsersLoneAt() {
        assertTrue("expected empty set", mentionedIn("meet @ noon", "just @").isEmpty());
    }

    /*
     * Warning: all the tests you write here must be runnable against any
     * Extract class that follows the spec. It will be run against several staff
     * implementations of Extract, which will be done by overwriting
     * (temporarily) your version of Extract with the staff's version.
     * DO NOT strengthen the spec of Extract or its methods.
     *
     * In particular, your test cases must not call helper methods of your own
     * that you have put in Extract, because that means you're testing a
     * stronger spec than Extract says. If you need such helper methods, define
     * them in a different class. If you only need them in this test class, then
     * keep them in this test class.
     */

}