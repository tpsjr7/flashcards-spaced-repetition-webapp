package spacedrepserver

import grails.converters.JSON

class RemoveDeckController {

    def index() {
        response.setContentType("text/html;charset=UTF-8");
        int deck_id = params.deck_id as  int;
        try {
            Deck.withTransaction {
                def d = Deck.get(deck_id)
                Card.findAllByDeck(d)*.delete()
                d.delete()
            }
        } catch (Exception ex) {
            def ret = [status: ex.getMessage()] as JSON
            render ret
            return;
        }
        render "{status:'deck removed'}"
        return;
    }
}
