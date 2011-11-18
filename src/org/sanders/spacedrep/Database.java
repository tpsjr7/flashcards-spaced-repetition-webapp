package org.sanders.spacedrep;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.h2.jdbcx.JdbcConnectionPool;
import org.sanders.spacedrep.Database.CreateCardsParams.CardSides;

public class Database {
	private static JdbcConnectionPool cp;
	
	private Database(){
		
	}
	static {		
		init();
	}
	
	private static boolean initialized = false;
	public static void init(){
		if(initialized) {
			return;
		}
		
		try {
			cp = JdbcConnectionPool.create("jdbc:h2:/home/tpsjr7/workspace/SpacedRepServer/testdb/testdb", "sa", "sa");
			Connection conn;
			conn = cp.getConnection();
			Statement st = conn.createStatement();
			st.execute("create table if not exists deck ( id int primary key auto_increment, name varchar(255) unique)");
			st.execute("create table if not exists card  ( id int primary key auto_increment, " +
					"deck_id int, foreign_written varchar(255), pronunciation varchar(255),  " +
					"translation varchar(255), interval int8, timedue int8, lastTimeTested int8, active tinyint, foreign key(deck_id) references deck(id) )");
			conn.close();
			initialized = true;
		} catch (SQLException e) {
			initialized = false;
			throw new RuntimeException(e);
		}
		
	}
	
	public static void destroy(){
		try {
			cp.dispose();
			org.h2.Driver.unload();
			
			//DriverManager.deregisterDriver(org.)
		} catch (SQLException e) {
			e.printStackTrace();
		}
		initialized = false;
	}
	
	public synchronized static int createDeck(String name) throws SQLException{
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = cp.getConnection();
			ps = conn.prepareStatement("insert into deck(name) values ( ? )",Statement.RETURN_GENERATED_KEYS);
			ps.setString(1, name);
			ps.execute();
			ResultSet rs = ps.getGeneratedKeys();
			int gen_id;
			if(rs.first()){
				gen_id = rs.getInt(1);
			}else{
				throw new RuntimeException("could not create the new deck: "+name);
			}
			return gen_id;
		}finally{
			try{
				ps.close();
				conn.close();							
			}catch(Exception e){}
		}
	}
	
	public static class CreateCardsParams {
		public static class CardSides {
			public String foreignWritten;
			public String pronunciation;
			public String translation;
		}
		public int deckID;
		public long interval;
		public long timeDue;
		public long lastTimeTested;
		public byte active;
		public List<CardSides> cards;
	}
	
	public static class Card {
		public int id = -1;
		public int deck_id = -1;
		public String foreignWritten;
		public String pronunciation;
		public String translation;
		public long interval;
		public long timeDue;
		public long lastTimeTested;
		public byte active;
		
		public Card deepCopy(){
			Card n = new Card();
			n.id = id;
			n.deck_id = deck_id;
			n.foreignWritten = new String(foreignWritten);
			n.pronunciation = new String(pronunciation);
			n.translation = new String(translation);
			n.interval = interval;
			n.timeDue = timeDue;
			n.lastTimeTested = lastTimeTested;
			n.active = active;
			return n;
		}
	}
	
	public static Card getCard(int deck_id, int card_id) throws SQLException{
		Connection conn = null;
		PreparedStatement ps = null;
		
		//deck_id int, foreign_written varchar(255), pronunciation varchar(255),  " +
		//"translation varchar(255), interval int8, timedue int8, lastTimeTested int8, active tinyint
		
		try{
			conn = cp.getConnection();
			ps = conn.prepareStatement("select foreign_written, pronunciation, translation, " +
					"interval, timedue, lastTimeTested, active from card where id=? and deck_id=?");
			//ps.setString(1, prefix+"%");
			ps.setInt(1,card_id);
			ps.setInt(2, deck_id);
			
			final ResultSet rs =  ps.executeQuery();
			Card c = new Card();
			
			if(rs.first()){
				c.foreignWritten = rs.getString(1);
				c.pronunciation = rs.getString(2);
				c.translation = rs.getString(3);
				c.interval = rs.getLong(4);
				c.timeDue = rs.getLong(5);
				c.lastTimeTested = rs.getLong(6);
				c.active = rs.getByte(7);
				c.id = card_id;
				c.deck_id = deck_id;
			}else{
				throw new RuntimeException("Could not find card with id: "+card_id+" in deck: "+deck_id);
			}
			rs.close();
			ps.close();
			return c;
		}finally{
			conn.close();
		}
	}
	public static class NVP{
		int id;
		String name;
		public NVP(int id, String name){
			this.id = id;
			this.name = name;
		}
	}
	public static List<NVP> listDecks(String prefix, int limit) throws SQLException {
		List<NVP> decks = new ArrayList<NVP>();
		Connection conn = null;
		PreparedStatement ps = null;
		
		try{
			conn = cp.getConnection();
			ps = conn.prepareStatement("select id, name from deck where name like ? escape '\' order by name ");
			ps.setString(1, prefix+"%");
			ps.setMaxRows(limit);
			final ResultSet rs =  ps.executeQuery();
			for(int c = 0 ; rs.next() && c < limit ; c++){
				decks.add(new NVP(rs.getInt(1),rs.getString(2)));
			}
			rs.close();
			ps.close();
			return decks;
		}finally{
			conn.close();
		}
	}
	
	public static int selectCardToActivate(int deck_id) throws SQLException{
		Connection conn = cp.getConnection();
		String sql = "select id from card where active=0 and deck_id = ? order by id";
		try{
			PreparedStatement ps = conn.prepareStatement(sql);
			ps.setMaxRows(1);
			ps.setInt(1, deck_id);
			
			ResultSet rs = ps.executeQuery();
			int id = -1;  
			if(rs.first()){
				id = rs.getInt(1);
			}
			rs.close();
			ps.close();
			return id;
		}finally{
			conn.close();
		}
	}
	
	public static void updateTimeDue(int deck_id, int card_id, long timedue) throws SQLException{
		activateCard( deck_id, card_id, timedue) ;
	}
	
	public static void activateCard(int deck_id, int card_id, long timedue) throws SQLException{
		Connection conn = cp.getConnection();
		String sql = "update card set active=1, timedue=? where id=? and deck_id=? ";
		try{
			PreparedStatement ps = conn.prepareStatement(sql);
			ps.setLong(1, timedue);
			ps.setInt(2,card_id);
			ps.setInt(3, deck_id);
			
			int rowcount = ps.executeUpdate();
			if(rowcount!=1){
				throw new RuntimeException("The wrong number of card were activated (should be 1): "+rowcount);
			}
			ps.close();
		}finally{
			conn.close();
		}
	}
	
	public static int getEarliestCardDue(int deck_id) throws SQLException{
		Connection conn = cp.getConnection();
		String sql = "select t1.id from ( select id, timedue from card where deck_id=? and active=1 ) as t1 " +
				"left outer join ( select id, timedue from card where deck_id=? and active=1 ) as t2 " +
				"on t1.TIMEDUE > t2.TIMEDUE " +
				"where  t2.id is NULL";
		
		try{
			PreparedStatement ps = conn.prepareStatement(sql);
			ps.setMaxRows(1);
			ps.setInt(1, deck_id);
			ps.setInt(2, deck_id);
			
			ResultSet rs = ps.executeQuery();
			int id;  
			if(rs.first()){
				id = rs.getInt(1);
			}else{
				throw new RuntimeException("could not find a card due.");
			}
			ps.close();
			return id;
		}finally{
			conn.close();
		}
	}
	
	public static int findMostOverdueCard(int deck_id) throws SQLException{
		
		Connection conn = cp.getConnection();
		String sql;
		
		try{
			sql = "select t1.id from ( select id, ( ? - timedue  ) / interval as overdue_fraction " +
					"from card where ? > timedue  and deck_id = ? and active=1 ) as t1 " +
					"left outer join ( select id, ( ? - timedue  ) / interval as overdue_fraction " +
					"from card where ? > timedue  and deck_id = ? and active=1 ) as t2 " +
					"on t1.OVERDUE_FRACTION <  t2.OVERDUE_FRACTION where t2.id is null; ";
					
			PreparedStatement ps = conn.prepareStatement(sql);
			long now = new Date().getTime();
			ps.setLong(1, now);
			ps.setLong(2, now);
			ps.setInt(3, deck_id);
			ps.setLong(4, now);
			ps.setLong(5, now);
			ps.setInt(6, deck_id);
            
            ps.setMaxRows(1);
			ResultSet rs = ps.executeQuery();
			int ret;
			if(rs.first()){
				ret = rs.getInt(1);
			}else{
				ret = -1; // no over due cards
			}
			ps.close();
			return ret;
		}finally{
			conn.close();
		}

	}
	
	
	public static void updateCard(Card card) throws SQLException{
		Connection conn = null;
		PreparedStatement ps = null;
		try{
			conn = cp.getConnection();
			 ps = conn.prepareStatement("update card set deck_id=?, " +
					"foreign_written=?, pronunciation=?, translation=? , interval=?, timedue=?, " +
					"lastTimeTested=?, active=? where id=? ");
			

			ps.setInt(1, card.deck_id);
			ps.setString(2, card.foreignWritten);
			ps.setString(3, card.pronunciation);
			ps.setString(4, card.translation);
			ps.setLong(5, card.interval);
			ps.setLong(6,card.timeDue);
			ps.setLong(7, card.lastTimeTested);
			ps.setByte(8, card.active);
			ps.setInt(9, card.id);

			ps.execute();
			ps.close();
			return;
		}finally{
			conn.close();
		}
	}
	
	
	public static class CardCount {
		public int activeCards;
		public int totalCards;
	}
	/**
	 * Counts how many cards are active, i.e. the ones 
	 * that are memorized plus the one that is being learned.
	 * 
	 * @param deckid -
	 * @return the number of active cards
	 * @throws SQLException 
	 */
	public static CardCount countActiveCards(int deckid) throws SQLException{
		Connection conn = null;
		PreparedStatement ps = null;
		try{
			conn = cp.getConnection();
			
			String sql = "select count(*) as active_count, " +
					"( select count(*) from card where deck_id=? ) as total_count " +
					"from card where deck_id=? and active=1";
				
			ps = conn.prepareStatement(sql);
			ps.setInt(1,deckid);
			ps.setInt(2,deckid);
			
			ResultSet rs = ps.executeQuery();
			
			CardCount cc = new CardCount();
			if(rs.first()){
				cc.activeCards = rs.getInt(1);
				cc.totalCards = rs.getInt(2);
			}else{
				throw new RuntimeException("Couldn't count cards for the deck.");
			}
			
			ps.close();
			return cc;
		}finally{
			conn.close();
		}
	}
	
	public synchronized static void createCards(CreateCardsParams ccp) throws SQLException{
			Connection conn = null;
			PreparedStatement ps = null;
			try{
				conn = cp.getConnection();
				conn.setAutoCommit(false);
				 ps = conn.prepareStatement("insert into card( deck_id, " +
						"foreign_written, pronunciation, translation , interval, timedue, " +
						"lastTimeTested, active) values ( ?,?,?,?,?,?,?,?)");
				
				for(int i = 0 ; i < ccp.cards.size() ; i++){
					CardSides cs = ccp.cards.get(i);
					ps.setInt(1, ccp.deckID);
					ps.setString(2, cs.foreignWritten);
					ps.setString(3, cs.pronunciation);
					ps.setString(4, cs.translation);
					ps.setLong(5, ccp.interval);
					ps.setLong(6,ccp.timeDue);
					ps.setLong(7, ccp.lastTimeTested);
					ps.setByte(8, ccp.active);
					ps.addBatch();
				}
	
				int [] rowcounts = ps.executeBatch();
				if(rowcounts.length!=ccp.cards.size()){
					conn.rollback();
					throw new RuntimeException("Did not insert the same number of cards as given.");
				}
				conn.commit();
				ps.close();
				return;
			}finally{
				
				try{conn.setAutoCommit(true);}catch(Exception e){};
				conn.close();
			}
	}
}
