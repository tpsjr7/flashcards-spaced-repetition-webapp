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
	
	public static final long initialInterval = 10 * 1000; // 10 seconds
	private static final long alreadyKnownInitialInterval = 10 * 60 * 1000;
	
	private static final float minimumEaseFactor = 1.3f;
	public static final float defaultEaseFactor = 2.3f;
	
	private static final int instantInterval = 3000;
	private static final int hesitationInterval = 8000;

	private static final long minimumTimeBeforeAdjust = 5* 60 * 1000;
        
	public void addCards(JSONObject params) throws JSONException, SQLException{
		CreateCardsParams ccp = new CreateCardsParams();
		ccp.cards = new ArrayList<CardSides>();
		ccp.deckID = params.getInt("deckId");
		
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
	 * nextCardOrPause
	 * 
	 * If there are overdue cards then it returns the most
	 * overdue one based on the fraction relative to its interval it is overdue.
	 * 
	 * Activates a new card only if none are overdue and learnMore is true.
	 *
	 * If there are none over due and no more to learn, then have the font
	 * end wait until the earliest card will be due. 
	 * 
	 * @param deck_id
	 * @param learnMore - will activate a new card for learning if true, otherwise
	 * it pretends its the end of deck and there are no new cards to show.
	 * 
	 * @return - Always returns a card and when it should be shown to the user.
     *  unless there is no card due, then it returns null.
	 * @throws SQLException - If there are no cards in the deck
	 */
	public Card nextCardOrPause(int deck_id, boolean learnMore) throws SQLException{
		int most_overdue = Database.findMostOverdueCard(deck_id);//find most percent overdue card ( time overdue / interval)
		long now = new Date().getTime();
		int cardTestingId = -1;
		System.out.println("learnmore: " + learnMore);
		if(most_overdue==-1){
			//no overdue cards, so show new cards if available and the user is
			//learning new cards.
            //if there are any more inactive cards, pick one to become active if learnMore is true.
			int activateCard; 
			//single '=' on purpose in this if statement.
			if(learnMore && (activateCard = Database.selectCardToActivate(deck_id)) !=-1){
				//there are inactive cards, pick a new card to activate and learn
				Database.activateCard(deck_id, activateCard, now);
				cardTestingId = activateCard;
			}else{
				//all cards are already activated, so have the front end
				//wait (pause) until the earliest card will be due.
				cardTestingId = Database.getEarliestCardDue(deck_id);
                if(cardTestingId==-1){
                    //there are no cards due
                   return null;
                }
			}
		}else{
			//Show the card that is most overdue
			Database.activateCard(deck_id, most_overdue, now);
			cardTestingId = most_overdue;
		}
		return Database.getCard(deck_id, cardTestingId);
	}

        private boolean isCorrect(int answerVersion, int answer){
            if(answerVersion!=0){
                throw new RuntimeException("Answer version "+answerVersion + " is not supported.");
            }
			if( answer < 0 ||  answer  > 3 ){
				throw new RuntimeException("Unsupported answer choice "+answer);

			}
            return answer == 2 || answer==3;
        }
        private float udpateEaseFactor(float oldFactor, int answerVersion, int answer, long responseTime, long lastActualInterval, long actualInterval, long lastTimeShownBack){


			if(lastTimeShownBack==0){
				System.out.println("card just activated, keeping ease factor: " + oldFactor);
				return oldFactor;
			}
			
			if(actualInterval < minimumTimeBeforeAdjust){
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
            float effectiveFactor = lastActualInterval == 0  ? oldFactor : (float)actualInterval / (float)lastActualInterval; //old decks may not have a lastActualInterval and will be 0, so just use the oldFactor

            //effective ease factor is the ease factor it would have been considering how much time it was over due

            //if it was way past due and it was missed, then calculate the minimum of the old factor vs the effective factor minus correction

            float newFactor;

            if(answer==0){
                //wrong, had no clue
                newFactor = Math.max(Math.min(oldFactor, effectiveFactor - .8f), minimumEaseFactor);
            }else  if(answer==1){
                //wrong but familiar
                newFactor = Math.max(Math.min(oldFactor, effectiveFactor - .4f), minimumEaseFactor);
            }else if(answer==2 || answer==3){
                //corect, adjust based on response time
                if(responseTime >= 0 && responseTime <= instantInterval){
                    newFactor = oldFactor + 0.1f;
                }else if(responseTime > instantInterval && responseTime <= hesitationInterval){
                    newFactor = oldFactor;
                } else if(responseTime > hesitationInterval){
					newFactor = Math.max(Math.min(oldFactor, effectiveFactor - .15f), minimumEaseFactor);
                }else{
                    throw new RuntimeException("invalid response time "+responseTime);
                }
            }else{
                throw new RuntimeException("invalid answer " + answer + " for answer version "+answerVersion);
            }
            System.out.println("answer: "+ answer +", old factor:" + oldFactor +", effective factor: "+ effectiveFactor+ ", new factor:"+newFactor+", responseTime:" + responseTime +", time diff: "+actualInterval+", last interval: "+lastActualInterval );
            return newFactor;
        }
/*
	public static enum AnswerChoice{
		Wrong, Close, Correct, AlreadyKnown
	}

	private AnswerChoice interpretAnswer(int answer, int answerVersion){
		if(answerVersion!=0){
			throw new RuntimeException("Answer version "+answerVersion + " is not supported.");
        }
		switch (answer){
			case 0:
				return AnswerChoice.Wrong;
			case 1:
				return AnswerChoice.Close;
			case 2:
				return AnswerChoice.Correct;
			case 3:
				return AnswerChoice.AlreadyKnown;
			default:
				throw new RuntimeException("Unsupported answer "+answer);
		}
	}*/
	public void rescheduleCard(int deck_id, long timeShownBack,  int card_id, long responseTime, int answerVersion, int answer) throws SQLException {

		Card c = Database.getCard(deck_id, card_id);
	    
		//TODO: think through if the card has been made inactive and has just
		//been reactivated and shown, lastTimeTested should be set to 0
		//so how will diff be used?
		long actualInterval = timeShownBack -  c.lastTimeShownBack;
		
        c.easeFactor = udpateEaseFactor(c.easeFactor, answerVersion, answer, responseTime,  c.lastActualInterval,actualInterval,  c.lastTimeShownBack);

	    if(isCorrect(answerVersion, answer)){
			if(c.lastTimeShownBack == 0){//user responded to just activated card
				if(answer==2){//correct
					c.scheduledInterval = initialInterval;
				}else if(answer==3){//correct and already known card
					c.scheduledInterval = alreadyKnownInitialInterval;
				}else{
					throw new RuntimeException("unknown correct answer type "+answer);
				}
				c.lastActualInterval = 0;
			}else{
				c.scheduledInterval = (long) ((double)actualInterval * c.easeFactor);
				c.lastActualInterval = actualInterval;
			}
			c.timeDue = timeShownBack + c.scheduledInterval;
			c.lastTimeShownBack = timeShownBack;
	    }else{
	    	c.active = 0; //de activate to prefer reviewing known cards over relearning forgot cards and to prevent having too many missed cards
	    	//causing it too hard to learn olds ones.
			c.scheduledInterval = initialInterval;
	        c.lastActualInterval = 0; //TODO: how should this be handled?
			c.lastTimeShownBack = 0 ; //TODO: make sure this has a sane default value when creating new cards
			c.timeDue = 0;
	    }
		
	    System.out.println(c.toString());
	    Database.updateCard(c);
	}
	
}
