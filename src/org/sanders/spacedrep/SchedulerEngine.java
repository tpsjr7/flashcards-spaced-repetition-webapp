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


	public Card nextCardOrPause(int deck_id) throws SQLException{
		int most_overdue = Database.findMostOverdueCard(deck_id);
		long now = new Date().getTime();
		int cardTesting = -1;
		if(most_overdue==-1){
			//no overdue cards, 
            //if there are any more inactive cards, pick one to become active
			int activateCard = Database.selectCardToActivate(deck_id);
			if(activateCard!=-1){//there are inactive cards

				Database.activateCard(deck_id, activateCard, now);
				cardTesting = activateCard;
			}else{//all cards are already activated
				cardTesting = Database.getEarliestCardDue(deck_id);
			}
		}else{
			Database.updateTimeDue(deck_id, most_overdue, now);
			cardTesting = most_overdue;
		}
		return Database.getCard(deck_id, cardTesting);
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
