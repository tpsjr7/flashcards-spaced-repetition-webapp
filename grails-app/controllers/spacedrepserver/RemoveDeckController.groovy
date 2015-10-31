package spacedrepserver

import grails.converters.JSON

class RemoveDeckController {

    def databaseService

    def index() {
        response.setContentType("text/html;charset=UTF-8");
        int deck_id = params.deck_id as  int;
        try {
            databaseService.removeDeck(Deck.load(deck_id))
        } catch (Exception ex) {
            def ret = [status: ex.getMessage()] as JSON
            render ret
            return;
        }
        render "{status:'deck removed'}"
        return;
    }
}
