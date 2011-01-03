package org.sanders.spacedrep;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.h2.jdbcx.JdbcConnectionPool;

public class Database {
	private static JdbcConnectionPool cp;
	
	static {		
		try {
			cp = JdbcConnectionPool.create("jdbc:h2:/home/tpsjr7/workspace/SpacedRepServer/testdb/testdb", "sa", "sa");
			Connection conn;
			conn = cp.getConnection();
			Statement st = conn.createStatement();
			st.execute("create table if not exists deck ( id int primary key auto_increment, name varchar(255) unique)");
			st.execute("create table if not exists card  ( id int primary key auto_increment, deck_id int, foreign_written varchar(255), pronunciation varchar(255),  translation varchar(255), foreign key(deck_id) references deck(id) )");
			conn.close();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

	}
	
	public static void destroy(){
		try {
			cp.dispose();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized static int createDeck(String name){
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
		}catch(Exception e){
			throw new RuntimeException(e);
		}finally{
			try{
				ps.close();
				conn.close();							
			}catch(Exception e){}
		}
	}
	
	public static class CreateCardsParams {
		public int deckID;
		public String foreignWritten;
		public String pronunciation;
		public String translation;
	}
	public static class NVP{
		int id;
		String name;
		public NVP(int id, String name){
			this.id = id;
			this.name = name;
		}
	}
	public static List<NVP> listDecks(String prefix, int limit) {
		List<NVP> decks = new ArrayList<NVP>();
		Connection conn = null;
		PreparedStatement ps = null;
		
		try{
			conn = cp.getConnection();
			ps = conn.prepareStatement("select id, name from deck where name like ? ");
			ps.setString(1, prefix+"%");
			ps.setMaxRows(limit);
			final ResultSet rs =  ps.executeQuery();
			for(int c = 0 ; rs.next() && c < limit ; c++){
				decks.add(new NVP(rs.getInt(1),rs.getString(2)));
			}
			rs.close();
			return decks;
		}catch(Exception e){
			throw new RuntimeException(e);
		}finally{
			try{
				ps.close();
				conn.close();
			}catch(Exception e){}
		}
	}
	
	public synchronized static void createCard(CreateCardsParams ccp){
		try {
			Connection conn = cp.getConnection();
			PreparedStatement ps = conn.prepareStatement("insert into card( deck_id, foreign_written, pronunciation, translation ) values ( ?,?,?,? )");
			
			ps.setInt(1, ccp.deckID);
			ps.setString(2, ccp.foreignWritten);
			ps.setString(3, ccp.pronunciation);
			ps.setString(4, ccp.translation);
			
			ps.execute();
			ps.close();
			conn.close();
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
}
