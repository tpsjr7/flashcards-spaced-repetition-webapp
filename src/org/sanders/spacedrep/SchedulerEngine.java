package org.sanders.spacedrep;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sanders.spacedrep.Database.Card;
import org.sanders.spacedrep.Database.CreateCardsParams;
import org.sanders.spacedrep.Database.CreateCardsParams.CardSides;

public class SchedulerEngine {
	
	private final long initialInterval = 2000;
	private final boolean measureRealIntervals = false;
	private final double correctMultiplier = 6.0;

	public void addCards(JSONObject params) throws JSONException, SQLException{
		CreateCardsParams ccp = new CreateCardsParams();
		ccp.cards = new ArrayList<CardSides>();
		ccp.active = 0;
		ccp.deckID = params.getInt("deckId");
		ccp.interval = this.initialInterval;
		ccp.timeDue = 0;
		ccp.lastTimeTested = Long.MAX_VALUE;
		
		JSONArray cards = params.getJSONArray("cards");
		
		for(int c = 0 ; c < cards.length(); c++){
			CardSides cs = new CardSides();
			JSONObject jo = cards.getJSONObject(c);
			cs.foreignWritten = jo.getString("written");
			cs.pronunciation = jo.getString("pronunciation");
			cs.translation = jo.getString("translation");
			ccp.cards.add(cs);
		}
		
		Database.createCards(ccp);
		return;
	}


	/**
	 * nextCardOrPausenextCardOrPause
	 * 
	 * If there are overdue cards then it returns the most
	 * overdue one.
	 * 
	 * Activates a new card only if none are overdue and learnMore is true.
	 * 
	 * @param deck_id
	 * @param learnMore - will activate a new card for learning if true, otherwise
	 * it pretends its the end of deck and there are no new cards to show.
	 * 
	 * @return - Always returns a card and when it should be shown to the user. 
	 * @throws SQLException - If there are no cards in the deck
	 */
	public Card nextCardOrPause(int deck_id, boolean learnMore) throws SQLException{
		int most_overdue = Database.findMostOverdueCard(deck_id);
		long now = new Date().getTime();
		int cardTestingId = -1;
		System.out.println("learnmore: " + learnMore);
		if(most_overdue==-1){
			//no overdue cards, so show new cards if available. 
            //if there are any more inactive cards, pick one to become active if learnMore is true.
			int activateCard; 
			//single '=' on purpose in this if statement.
			if(learnMore && (activateCard = Database.selectCardToActivate(deck_id)) !=-1){//there are inactive cards
				Database.activateCard(deck_id, activateCard, now);
				cardTestingId = activateCard;
			}else{//all cards are already activated
				cardTestingId = Database.getEarliestCardDue(deck_id);
			}
		}else{
			//Show the card that is most overdue
			Database.updateTimeDue(deck_id, most_overdue, now);
			cardTestingId = most_overdue;
		}
		return Database.getCard(deck_id, cardTestingId);
	}
	
	public void rescheduleCard(int deck_id, boolean bCorrect,long timeShown, int card_id) throws SQLException{
	    
	    long now = new Date().getTime();
	    
	    Card c = Database.getCard(deck_id, card_id);
	    long  diff = timeShown - c.lastTimeTested;
	    
	    
	    if(this.measureRealIntervals && diff > c.interval){
	        c.interval = diff ;
	    }
	    
	    if(bCorrect){
	        c.interval = (long) ((double)c.interval * correctMultiplier);
	    }else{
	    	c.active = 0; //de activate to prefer reviewing known cards over relearning forgot cards and to prevent having too many missed cards
	    	//causing it too hard to learn olds ones.
	        c.interval = initialInterval;
	    }
	    
	    c.timeDue = now + c.interval;
	    
	    c.lastTimeTested = now;
	    
	    Database.updateCard(c);
	}
	
}
