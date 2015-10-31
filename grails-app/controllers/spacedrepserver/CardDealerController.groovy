package spacedrepserver

import flashcards.Card
import flashcards.SchedulerEngineService
import grails.converters.JSON
import static SchedulerEngineService.CardCount

class CardDealerController {


    def schedulerEngineService

    def index() {
        String op = params.op;
        request.setCharacterEncoding("utf-8");
        response.setContentType("text/html;charset=UTF-8");


        if(op == "addCards"){
            schedulerEngineService.addCards(params.params as JSON)
            render "OK"
            return;
        }
        if(op == "nextCardOrPause"){
            int deck_id = params.deck_id as int

            boolean learnMore = Boolean.parseBoolean(params.learn_more);

            Card c = schedulerEngineService.nextCardOrPause(deck_id, learnMore)
            SchedulerEngineService.CardCount cc = schedulerEngineService.countActiveCards(deck_id);

            def out = [
                    "serverTime": new Date().time,
                    "ac": cc.activeCards,
                    "tc": cc.totalCards,
                    "dc": cc.dueCards
            ]

            if(c){
                out.card_id = -1
            } else {
                out.cardToShow = [
                        front: c.foreignWritten,
                        back: "${c.pronunciation} - ${c.translation}"
                ]
                out.timeDue = c.timedue
                out.card_id = c.id
            }
            render out as JSON
            return
        }
        if(op == "getconfig"){
            def out = [
                    alertOnCardDue: false,
                    fontsize: "20px",
            ]
            render out as JSON
        }
        if(op == "reschedulecard"){
            schedulerEngineService.rescheduleCard(
                    params.deck_id as int,
                    params.timeShownBack as long,
                    params.card_id as long,
                    params.rt as long,
                    params.answerVersion as int,
                    params.a as int
            )
            render "OK"
            return
        }
        throw new RuntimeException("operation '" + op + "' is not supported.");
    }
}
