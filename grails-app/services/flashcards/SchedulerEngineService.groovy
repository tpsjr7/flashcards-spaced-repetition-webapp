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
    Card nextCardOrPause(int deck_id, boolean learnMore){
        throw new RuntimeException("not implemented")
    }

    CardCount countActiveCards(deck_id){
        throw new RuntimeException("not implemented")
    }

    void rescheduleCard(deck_id, timeShownBack, card_id, responseTime, answerVersion, answer){

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
        }

    }
}
