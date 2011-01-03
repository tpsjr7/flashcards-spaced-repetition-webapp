package org.sanders.spacedrep;

import java.sql.SQLException;

import org.junit.Test;

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
	
	@Test public void testDatabase(){
		System.out.println("deck id: " + Database.createDeck("__adtestdeck__"));
	}
	
}
