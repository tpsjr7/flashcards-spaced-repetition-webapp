package spacedrepserver

class Card {

    String foreignWritten
    String pronunciation
    String translation
    Long scheduledInterval
    Long timedue
    Long lastTimeShownBack
    byte active
    Float easeFactor
    Long lastActualInterval


    /*
        st.execute("create table if not exists deck ( id int primary key auto_increment, name varchar(255) unique)");
    st.execute("create table if not exists card  ( id int primary key auto_increment, "
            + "deck_id int, foreign_written varchar(255), pronunciation varchar(255),  "
            + "translation varchar(255), scheduled_interval int8, timedue int8, last_time_shown_back int8, "
            + "active tinyint, ease_factor double(6), last_actual_interval int8, foreign key(deck_id) references deck(id) )");
 */
    static belongsTo = [deck: Deck]

    static constraints = {
    }
}
