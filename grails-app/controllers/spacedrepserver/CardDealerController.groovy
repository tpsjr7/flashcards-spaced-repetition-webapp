package spacedrepserver

import grails.converters.JSON

class CardDealerController {


    def schedulerEngineService

    def index() {
        String op = params.op;
        request.setCharacterEncoding("utf-8");
        response.setContentType("text/html;charset=UTF-8");


        if(op == "addCards"){
            schedulerEngineService.addCards(JSON.parse(params.params) as Map)
            render "OK"
            return;
        }
        if(op == "nextCardOrPause"){
            int deck_id = params.deck_id as int

            boolean learnMore = Boolean.parseBoolean(params.learn_more);

            Deck deck = Deck.load(deck_id)
            Card c = schedulerEngineService.nextCardOrPause(deck, learnMore)
            Map cc = schedulerEngineService.countActiveCards(deck);

            def out = [
                    "serverTime": new Date().time,
                    "ac": cc.activeCards,
                    "tc": cc.totalCards,
                    "dc": cc.dueCards
            ]

            out.card_id = -1
            if(c){
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
            return
        }
        if(op == "reschedulecard"){
            schedulerEngineService.rescheduleCard(
                    params.deck_id as int,
                    params.timeShownBack as long,
                    params.card_id as int,
                    params.rt as long,
                    (params.answerVersion ?: 0 ) as int,
                    params.a as int
            )
            render "OK"
            return
        }
        throw new RuntimeException("operation '" + op + "' is not supported.");
    }
}
