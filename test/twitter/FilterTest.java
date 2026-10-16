/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package twitter;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class FilterTest {

    /*
     * Testing strategy
     *
     * writtenBy(tweets, username)
     *   Input space, partitioned on:
     *     tweets: empty list, non-empty list
     *     number of tweets by username: 0, 1, many (> 1)
     *     other authors present in the list: no, yes
     *     letter case of username vs. the author's name: same, different
     *   Output space, partitioned on:
     *     size: 0, 1, many
     *     result is: a proper subset of the input, the whole input
     *     order: same relative order as the input (checked when size > 1)
     *
     * inTimespan(tweets, timespan)
     *   Input space, partitioned on:
     *     tweets: empty list, non-empty list
     *     position of each tweet's time relative to the timespan: before the start,
     *       exactly at the start, strictly inside, exactly at the end, after the end
     *     timespan length: zero (start == end), positive
     *   Output space, partitioned on:
     *     size: 0, 1, many; none / some / all of the input tweets
     *     order: same relative order as the input (checked when size > 1)
     *
     * containing(tweets, words)
     *   Input space, partitioned on:
     *     tweets: empty list, non-empty list
     *     words: empty list, 1 word, many words
     *     number of tweets containing at least one word: 0, 1, many, all
     *     number of the given words found in one tweet: 0, 1, many
     *     letter case of the word vs. the tweet text: same, different
     *     where the word occurs in the text: start, middle, end of the text
     *     the word occurs only as part of a longer word (e.g. "tal" in "talk"):
     *       it must NOT match, because matching is by whole space-bounded words
     *   Output space, partitioned on:
     *     size: 0, 1, many; none / some / all of the input tweets
     *     order: same relative order as the input (checked when size > 1)
     */

    private static final Instant d1 = Instant.parse("2016-02-17T10:00:00Z");
    private static final Instant d2 = Instant.parse("2016-02-17T11:00:00Z");
    private static final Instant d3 = Instant.parse("2016-02-17T12:00:00Z");

    private static final Tweet tweet1 = new Tweet(1, "alyssa", "is it reasonable to talk about rivest so much?", d1);
    private static final Tweet tweet2 = new Tweet(2, "bbitdiddle", "rivest talk in 30 minutes #hype", d2);
    private static final Tweet tweet3 = new Tweet(3, "alyssa", "Talk is cheap", d3);

    private static Tweet tweetWithText(long id, String text) {
        return new Tweet(id, "author", text, d1);
    }

    @Test(expected=AssertionError.class)
    public void testAssertionsEnabled() {
        assert false; // make sure assertions are enabled with VM argument: -ea
    }

    // ------------------------------------------------------------------ writtenBy

    // 2 tweets, 1 match, other authors present, result size 1
    @Test
    public void testWrittenByMultipleTweetsSingleResult() {
        List<Tweet> writtenBy = Filter.writtenBy(Arrays.asList(tweet1, tweet2), "alyssa");

        assertEquals("expected singleton list", 1, writtenBy.size());
        assertTrue("expected list to contain tweet", writtenBy.contains(tweet1));
    }

    // empty list of tweets
    @Test
    public void testWrittenByEmptyList() {
        List<Tweet> writtenBy = Filter.writtenBy(Collections.<Tweet>emptyList(), "alyssa");

        assertTrue("expected empty list", writtenBy.isEmpty());
    }

    // 0 tweets by the user
    @Test
    public void testWrittenByNoMatch() {
        List<Tweet> writtenBy = Filter.writtenBy(Arrays.asList(tweet1, tweet2), "nobody");

        assertTrue("expected empty list", writtenBy.isEmpty());
    }

    // many tweets by the user, mixed with another author's tweet; order preserved
    @Test
    public void testWrittenByManyResultsKeepsOrder() {
        List<Tweet> writtenBy = Filter.writtenBy(Arrays.asList(tweet1, tweet2, tweet3), "alyssa");

        assertEquals("expected two tweets", Arrays.asList(tweet1, tweet3), writtenBy);
    }

    // all tweets by the user (result is the whole input)
    @Test
    public void testWrittenByAllTweets() {
        List<Tweet> writtenBy = Filter.writtenBy(Arrays.asList(tweet1, tweet3), "alyssa");

        assertEquals("expected all tweets in order", Arrays.asList(tweet1, tweet3), writtenBy);
    }

    // username differs from the author only by letter case
    @Test
    public void testWrittenByDifferentCase() {
        List<Tweet> writtenBy = Filter.writtenBy(Arrays.asList(tweet1, tweet2), "ALYSSA");

        assertEquals("expected singleton list", Arrays.asList(tweet1), writtenBy);
    }

    // --------------------------------------------------------------- inTimespan

    // 2 tweets strictly inside a positive-length span, all returned, order preserved
    @Test
    public void testInTimespanMultipleTweetsMultipleResults() {
        Instant testStart = Instant.parse("2016-02-17T09:00:00Z");
        Instant testEnd = Instant.parse("2016-02-17T12:00:00Z");

        List<Tweet> inTimespan = Filter.inTimespan(Arrays.asList(tweet1, tweet2), new Timespan(testStart, testEnd));

        assertFalse("expected non-empty list", inTimespan.isEmpty());
        assertTrue("expected list to contain tweets", inTimespan.containsAll(Arrays.asList(tweet1, tweet2)));
        assertEquals("expected same order", 0, inTimespan.indexOf(tweet1));
    }

    // empty list of tweets
    @Test
    public void testInTimespanEmptyList() {
        Timespan span = new Timespan(d1, d3);

        assertTrue("expected empty list", Filter.inTimespan(Collections.<Tweet>emptyList(), span).isEmpty());
    }

    // all tweets before the start, or after the end: nothing returned
    @Test
    public void testInTimespanNoneInside() {
        List<Tweet> tweets = Arrays.asList(tweet1, tweet3);

        assertTrue("expected empty list (span after tweets)",
                Filter.inTimespan(tweets, new Timespan(d2.plusSeconds(1), d2.plusSeconds(60))).isEmpty());
        assertTrue("expected empty list (span before tweets)",
                Filter.inTimespan(tweets, new Timespan(d1.minusSeconds(60), d1.minusSeconds(1))).isEmpty());
    }

    // some inside, some before, some after: only the inside one, size 1
    @Test
    public void testInTimespanSomeInside() {
        List<Tweet> inTimespan = Filter.inTimespan(Arrays.asList(tweet1, tweet2, tweet3), new Timespan(d1.plusSeconds(1), d2.plusSeconds(1)));

        assertEquals("expected only the middle tweet", Arrays.asList(tweet2), inTimespan);
    }

    // boundary: tweet exactly at the start and exactly at the end are included (span is inclusive)
    @Test
    public void testInTimespanEndpointsIncluded() {
        List<Tweet> inTimespan = Filter.inTimespan(Arrays.asList(tweet1, tweet2, tweet3), new Timespan(d1, d3));

        assertEquals("expected all three, in order", Arrays.asList(tweet1, tweet2, tweet3), inTimespan);
    }

    // boundary: one second outside each endpoint is excluded
    @Test
    public void testInTimespanJustOutsideEndpointsExcluded() {
        List<Tweet> inTimespan = Filter.inTimespan(Arrays.asList(tweet1, tweet2, tweet3), new Timespan(d1.plusSeconds(1), d3.minusSeconds(1)));

        assertEquals("expected only the middle tweet", Arrays.asList(tweet2), inTimespan);
    }

    // zero-length span matching exactly one tweet's time
    @Test
    public void testInTimespanZeroLengthSpan() {
        List<Tweet> inTimespan = Filter.inTimespan(Arrays.asList(tweet1, tweet2, tweet3), new Timespan(d2, d2));

        assertEquals("expected only the tweet at d2", Arrays.asList(tweet2), inTimespan);
    }

    // ----------------------------------------------------------------- containing

    // 1 word found in 2 tweets, one in different case; order preserved
    @Test
    public void testContaining() {
        List<Tweet> containing = Filter.containing(Arrays.asList(tweet1, tweet2), Arrays.asList("talk"));

        assertFalse("expected non-empty list", containing.isEmpty());
        assertTrue("expected list to contain tweets", containing.containsAll(Arrays.asList(tweet1, tweet2)));
        assertEquals("expected same order", 0, containing.indexOf(tweet1));
    }

    // empty list of tweets
    @Test
    public void testContainingEmptyTweets() {
        List<Tweet> containing = Filter.containing(Collections.<Tweet>emptyList(), Arrays.asList("talk"));

        assertTrue("expected empty list", containing.isEmpty());
    }

    // empty list of words: nothing can match
    @Test
    public void testContainingNoWords() {
        List<Tweet> containing = Filter.containing(Arrays.asList(tweet1, tweet2), Collections.<String>emptyList());

        assertTrue("expected empty list", containing.isEmpty());
    }

    // word in no tweet
    @Test
    public void testContainingNoMatch() {
        List<Tweet> containing = Filter.containing(Arrays.asList(tweet1, tweet2), Arrays.asList("obama"));

        assertTrue("expected empty list", containing.isEmpty());
    }

    // word in exactly one tweet, so the result is a proper subset
    @Test
    public void testContainingOneMatch() {
        List<Tweet> containing = Filter.containing(Arrays.asList(tweet1, tweet2), Arrays.asList("minutes"));

        assertEquals("expected only tweet2", Arrays.asList(tweet2), containing);
    }

    // word differs from the text only by letter case (both directions)
    @Test
    public void testContainingCaseInsensitive() {
        List<Tweet> containing = Filter.containing(Arrays.asList(tweet1, tweet2, tweet3), Arrays.asList("TALK"));

        assertEquals("expected all three, in order", Arrays.asList(tweet1, tweet2, tweet3), containing);
    }

    // many words, a tweet matching only one of them is enough ("at least one")
    @Test
    public void testContainingManyWordsAnyMatch() {
        List<Tweet> containing = Filter.containing(Arrays.asList(tweet1, tweet2, tweet3), Arrays.asList("cheap", "minutes"));

        assertEquals("expected tweet2 and tweet3, in input order", Arrays.asList(tweet2, tweet3), containing);
    }

    // a tweet containing many of the words is returned only once
    @Test
    public void testContainingTweetMatchesManyWordsReturnedOnce() {
        List<Tweet> containing = Filter.containing(Arrays.asList(tweet2), Arrays.asList("rivest", "talk", "minutes"));

        assertEquals("expected tweet2 once", Arrays.asList(tweet2), containing);
    }

    // word at the start, in the middle and at the end of the text
    @Test
    public void testContainingWordPositions() {
        Tweet start = tweetWithText(10, "alpha beta gamma");
        Tweet end = tweetWithText(11, "gamma beta alpha");
        Tweet middle = tweetWithText(12, "beta alpha gamma");
        Tweet none = tweetWithText(13, "delta epsilon");

        List<Tweet> containing = Filter.containing(Arrays.asList(start, end, middle, none), Arrays.asList("alpha"));

        assertEquals("expected the three tweets with alpha, in order", Arrays.asList(start, end, middle), containing);
    }

    // word occurs only inside a longer word: not a match
    @Test
    public void testContainingPartOfLongerWordDoesNotMatch() {
        Tweet longer = tweetWithText(10, "talking about rivest");

        List<Tweet> containing = Filter.containing(Arrays.asList(longer), Arrays.asList("talk"));

        assertTrue("expected empty list", containing.isEmpty());
    }

    /*
     * Warning: all the tests you write here must be runnable against any Filter
     * class that follows the spec. It will be run against several staff
     * implementations of Filter, which will be done by overwriting
     * (temporarily) your version of Filter with the staff's version.
     * DO NOT strengthen the spec of Filter or its methods.
     *
     * In particular, your test cases must not call helper methods of your own
     * that you have put in Filter, because that means you're testing a stronger
     * spec than Filter says. If you need such helper methods, define them in a
     * different class. If you only need them in this test class, then keep them
     * in this test class.
     */

}