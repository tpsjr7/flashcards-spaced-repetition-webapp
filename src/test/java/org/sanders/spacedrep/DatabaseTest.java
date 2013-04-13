package org.sanders.spacedrep;

import org.sanders.spacedrep.Database.Card;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import static org.sanders.spacedrep.Database.CreateCardsParams.CardSides;
import static org.junit.Assert.*;

public class DatabaseTest {

	@Test public void test() throws SQLException{
		/*
		JdbcConnectionPool cp = JdbcConnectionPool.create("jdbc:h2:testdb", "sa", "sa");
		Connection conn = cp.getConnection();
		Statement st = conn.createStatement();
		st.execute("create table if not exists card  ( id int primary key, foreign_written varchar(255), pronunciation varchar(255),  translation varchar(255) )");
		st.execute("create table if not exists deck ( id int primary key, name varchar(255) )");
		st.execute("create table card_to_deck ( card_id int not null, deck_id int not null, foreign key(card_id) references card(id), foreign key(deck_id) references deck(id) )");
		
		conn.close();
		cp.dispose();*/
	}
	int deck_id ;
	@Test public void testDatabase() throws SQLException{
                if(true){ // hack to skip the test because the database is hardcoded
                    return;
                }
		Database.useDatabase("/home/tpsjr7/workspace/SpacedRepServer/testdb/unit_testdb");

		List<Database.NVP> pairs =  Database.listDecks("test", 1);

		if(pairs.size()==1){
			Database.removeDeck(pairs.get(0).id);
		}


		Database.createDeck("test");

		pairs =  Database.listDecks("test", 1);
		deck_id = pairs.get(0).id;
		
		SchedulerEngine se = new SchedulerEngine();
		Database.CreateCardsParams ccp = new Database.CreateCardsParams();
		ccp.deckID = deck_id;
		
		ArrayList<CardSides> sides = new ArrayList<CardSides>();
		CardSides cs = new CardSides();
		cs.foreignWritten = "fw";
		cs.pronunciation = "pro";
		cs.translation = "trans";
		
		sides.add(cs);
		ccp.cards = sides;

		Database.createCards(ccp);

		
		assertTotalCards(1);
		assertActiveCards(0);
		Card c = se.nextCardOrPause(deck_id, true);
		assertActiveCards(1);
		
		long tsb = 10;
		long rt = 1000;
		int answer = 0;//wrong
		se.rescheduleCard(deck_id, tsb, c.id, 1000, 0, answer);
		assertActiveCards(0);

		c = se.nextCardOrPause(deck_id, true);
		System.out.println(c);

	}

	private void assertTotalCards(int expected) throws SQLException{
		Database.CardCount cc;
		cc = Database.countActiveCards(deck_id);
		assertEquals( expected ,cc.totalCards );
	}
	private void assertActiveCards(int expected) throws SQLException{
		Database.CardCount cc;
		cc = Database.countActiveCards(deck_id);
		assertEquals( expected ,cc.activeCards );
	}
}
