package flashcards

import grails.converters.JSON
import grails.transaction.Transactional
import groovy.sql.Sql

@Transactional
class SchedulerEngineService {
    def dataSource

    public static final long     initialInterval              = 20 * 1000; // 10 seconds
    private static final long    alreadyKnownInitialInterval  = 12 * 60 * 60 * 1000; // 12 hours
    private static final float   minimumEaseFactor            = 1.3f;
    public static final float    defaultEaseFactor            = 2.3f;
    private static final int     instantInterval              = 3000;
    private static final int     hesitationInterval           = 9000;
    private static final long    minimumTimeBeforeAdjust      = 12 * 60 * 60 * 1000; // 12 hours
    private static final double  actualIntervalWeight         = 0.5;
    private static final long    freetimeBeforeNewCard        = 0 * 1000; // minimum amount of time reqired before a card is due before allowing a new card to be learned
    //private static final long    actualIntervalSkipToDay      = 20 * 60 * 1000; // if the actual interval was at least 20 minutes, then make sure it gets rescheduled to at least a day
    private static final boolean useEffectiveFactorIfCorrect = true; // if was way overdue and was correct, then update easefactor taking into account that long interval

    private Sql sql


    public static class CardCount {

        public int activeCards;
        public int totalCards;
        public int dueCards;
    }

    void addCards(Map json) {
        for(def c in json.cards){
            new Card(
                    deck: Deck.get(c.deckId),
                    foreignWritten: c.written,
                    pronunciation: c.pronunciation,
                    translation: c.translation,
                    scheduledInterval: initialInterval,
                    timedue: 0,
                    lastTimeShownBack: 0,
                    active: 0,
                    easeFactor: defaultEaseFactor,
                    lastActualInterval: 0
            ).save()
        }
    }

    /**
     * nextCardOrPause
     *
     * If there are overdue cards then it returns the most overdue one based on
     * the fraction relative to its interval it is overdue.
     *
     * Activates a new card only if none are overdue and learnMore is true.
     *
     * If there are none over due and no more to learn, then have the font end
     * wait until the earliest card will be due.
     *
     * @param deck_id
     * @param learnMore - will activate a new card for learning if true,
     * otherwise it pretends its the end of deck and there are no new cards to
     * show.
     *
     * @return - Always returns a card and when it should be shown to the user.
     * unless there is no card due, then it returns null.
     * @throws java.sql.SQLException - If there are no cards in the deck
     */
    Card nextCardOrPause(Deck deck, boolean learnMore){
        long delayNow = System.currentTimeMillis() + freetimeBeforeNewCard
        Card most_overdue = findMostOverdueCard(deck, delayNow)
        long now = System.currentTimeMillis()
        Card cardTesting = null;
        log.debug("learnmore: " + learnMore)
        if (most_overdue == null) {
            //no overdue cards, so show new cards if available and the user is
            //learning new cards.
            //if there are any more inactive cards, pick one to become active if learnMore is true.
            Card activateCard;

            if(learnMore){
                activateCard = Card.findByActiveAndDeck(0,deck,[sort: "id", order:"asc"])
            }
            if (learnMore && activateCard) {
                //there are inactive cards, pick a new card to activate and learn
                activateCard.active = 1
                activateCard.timedue = now
                activateCard.save()
                cardTesting = activateCard;
            } else {
                //all cards are already activated, so have the front end
                //wait (pause) until the earliest card will be due.
                cardTesting = getEarliestCardDue(deck);
                if (cardTesting == null) {
                    //there are no cards due
                    return null;
                }
            }
        } else {
            //Show the card that is most overdue
            most_overdue.active = 1
            most_overdue.save()
            cardTesting = most_overdue
        }

        return cardTesting;
    }

    CardCount countActiveCards(deck_id){
        throw new RuntimeException("not implemented")
    }

    void rescheduleCard(Deck deck, long timeShownBack, Card c, long responseTime, int answerVersion, int answer){
        //TODO: think through if the card has been made inactive and has just
        //been reactivated and shown, lastTimeTested should be set to 0
        //so how will diff be used?
        long actualInterval = timeShownBack - c.lastTimeShownBack;


        float oldEaseFactor = c.easeFactor;
        c.easeFactor = udpateEaseFactor(
                c.easeFactor,
                answerVersion,
                answer,
                responseTime,
                c.lastActualInterval,
                actualInterval,
                c.lastTimeShownBack);

        if (isCorrect(answerVersion, answer)) {
            if (c.lastTimeShownBack == 0) {//user responded to just activated card
                if (answer == 2) {//correct
                    c.scheduledInterval = initialInterval;
                } else if (answer == 3) {//correct and already known card
                    c.scheduledInterval = alreadyKnownInitialInterval;
                } else {
                    throw new RuntimeException("unknown correct answer type " + answer);
                }
                c.lastActualInterval = 0;
            } else {
                if (responseTime > hesitationInterval && c.easeFactor == oldEaseFactor) {
                    // The answer was correct, but was really slow to answer and the card was very overdue
                    // then schedule the card less than what it would be with the actual interval
                    // so the user should react faster next time
                    System.out.println("last scheduled interval: " + c.scheduledInterval);
                    double weightedInterval = (1.0 - actualIntervalWeight) * (double) c.scheduledInterval + actualIntervalWeight * (double) actualInterval;
                    c.scheduledInterval = (long) (weightedInterval * c.easeFactor);
                } else {
                    c.scheduledInterval = (long) ((double) actualInterval * c.easeFactor);
                }
                if (answer == 3) { //already known, make sure interval is at least alreadyKnownInitialInterval
                    c.scheduledInterval = Math.max(c.scheduledInterval, alreadyKnownInitialInterval);
                }
                if (c.scheduledInterval == 0) {
                    throw new RuntimeException("scheduledInterval was zero. ActualInterval: " + actualInterval + ", easeFactor: " + c.easeFactor + ", timeShownBack: " + timeShownBack + ", lastTimeShownBack: " + c.lastTimeShownBack);
                }
                c.lastActualInterval = actualInterval;
            }

            c.timedue = timeShownBack + c.scheduledInterval;
            c.lastTimeShownBack = timeShownBack;
        } else { // wrong answer
            c.active = 0; //de activate to prefer reviewing known cards over relearning forgot cards and to prevent having too many missed cards
            //causing it too hard to learn olds ones.
            c.scheduledInterval = initialInterval;
            c.lastActualInterval = 0; //TODO: how should this be handled?
            c.lastTimeShownBack = 0; //TODO: make sure this has a sane default value when creating new cards
            c.timedue = 0;
        }

        System.out.println(c.toString());

        c.save()
    }

    private boolean isCorrect(int answerVersion, int answer) {
        if (answerVersion != 0) {
            throw new RuntimeException("Answer version " + answerVersion + " is not supported.");
        }
        if (answer < 0 || answer > 3) {
            throw new RuntimeException("Unsupported answer choice " + answer);

        }
        return answer == 2 || answer == 3;
    }

    private float udpateEaseFactor(float oldFactor, int answerVersion, int answer, long responseTime, long lastActualInterval, long actualInterval, long lastTimeShownBack) {


        if (lastTimeShownBack == 0) {
            System.out.println("card just activated, keeping ease factor: " + oldFactor);
            return oldFactor;
        }

        if (actualInterval < minimumTimeBeforeAdjust) {
            //dont start adjusting the ease factor until the card is at least 5 minutes old.
            System.out.println("less than " + minimumTimeBeforeAdjust + " keeping ease factor " + oldFactor);
            return oldFactor;
        }

        //based on http://www.supermemo.com/english/ol/sm2.htm with some tweaks
        //0 no clue - 0.8
        //1 familiar - 0.4
        //2 correct
        //response time 0 to 3 seconds + .1
        //response time 3 to 8 seconds +0 ( no change)
        //more than 8 seconds serious difficulty -.15
        float effectiveFactor = lastActualInterval == 0 ? oldFactor : (float) actualInterval / (float) lastActualInterval; //old decks may not have a lastActualInterval and will be 0, so just use the oldFactor

        // Effective ease factor is the ease factor it would have been considering how much time it was over due.
        // If it was way past due and it was missed, then calculate the minimum of the old factor vs the effective factor minus correction

        float newFactor;

        if (answer == 0) { //wrong, had no clue
            // New factor should be at least minimumEaseFactor.
            // If reviewing when way overdate, then the present effectiveFactor could be
            // very large like 10. And so 10 - .8 would be very large so take the old factor instead.
            // But if it was reviewed when due, so that oldFactor == effectiveFactor then
            // it will take off 0.8.
            newFactor = Math.max(Math.min(oldFactor, effectiveFactor - 0.8f), minimumEaseFactor);
        } else if (answer == 1) {
            //wrong but familiar
            newFactor = Math.max(Math.min(oldFactor, effectiveFactor - 0.4f), minimumEaseFactor);
        } else if (answer == 2 || answer == 3) {
            //corect, adjust based on response time
            if (responseTime >= 0 && responseTime <= instantInterval) {
                newFactor = oldFactor + 0.1f; // dont use effective factor here, what if it was x10?
            } else if (responseTime > instantInterval && responseTime <= hesitationInterval) {
                // Didn't respond too fast or too slow, so the ease factor stays the same
                newFactor = oldFactor; // dont use effective factor here, what if it was x10?
            } else if (responseTime > hesitationInterval) {
                // Took too long to respond. Decrease ease factor a little but not less than the minimum.
                newFactor = Math.max(Math.min(oldFactor, effectiveFactor - 0.15f), minimumEaseFactor);
            } else {
                throw new RuntimeException("invalid response time " + responseTime);
            }
        } else {
            throw new RuntimeException("invalid answer " + answer + " for answer version " + answerVersion);
        }
        //System.out.println("answer: " + answer + ", old factor:" + oldFactor + ", effective factor: " + effectiveFactor + ", new factor:" + newFactor + ", responseTime:" + responseTime + ", time diff: " + actualInterval + ", last interval: " + lastActualInterval);
        return newFactor;
    }

    private Sql getSql(){
        if(!sql){
            sql = new Sql(dataSource: dataSource)
            assert sql.isCacheStatements()
            assert sql.isCacheNamedQueries()
        }
        return sql
    }

    Card findMostOverdueCard(Deck deck, long now){
        def row  = getSql().firstRow([
                now: now,
                deck: deck.id
        ],"""
            select id, timedue
            from (
                select id, timedue, (
                    select t1.id
                    from (
                        select id, ( :now - timedue  ) / scheduled_interval as overdue_fraction
                        from card where :now > timedue  and deck_id = :deck and active=1
                    ) as t1
                    left outer join (
                        select id, ( :now - timedue  ) / scheduled_interval as overdue_fraction
                        from card where :now > timedue  and deck_id = :deck and active=1
                    ) as t2
                    on t1.OVERDUE_FRACTION <  t2.OVERDUE_FRACTION
                    where t2.id is null limit 1
                ) as cid,
                from card
            ) as t1
            where t1.cid = id
        """)

        int id = -1
        long due = -1
        if(row){
            id = row.id
            due = row.timedue
            return Card.load(id)
        } else {
            return null
        }
    }

    Card getEarliestCardDue(Deck deck) {
        String sql = """
            select t1.id
            from (
                select id, timedue
                from card
                where deck_id=? and active=1
            ) as t1
            left outer join (
                select id, timedue
                from card
                where deck_id=? and active=1
            ) as t2
            on t1.TIMEDUE > t2.TIMEDUE
            where  t2.id is NULL"""

        def row = getSql().firstRow([deck.id, deck.id], sql)
        return row ? Card.load(row.id) : null
    }

}