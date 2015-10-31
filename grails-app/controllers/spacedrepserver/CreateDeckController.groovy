package spacedrepserver

import flashcards.Deck
import grails.converters.JSON

class CreateDeckController {

    def index() {
        PrintWriter pw = null;
        request.setCharacterEncoding("UTF-8");
        try {
            pw = response.getWriter();
            String name = request.getParameter("deck-name");
            name = name.toLowerCase();
            if (!name.equals(name.replaceAll("[^a-z0-9_]+", ""))) {
                def resp = [status:  "Deck name can only contain characters a-z or 0-9 or _"]
                pw.print(resp as JSON);
                return;
            }
            def jo = [status:"unknown status"]
            try {
                def deck =  new Deck(name: name).save(flush: true);

                jo = [status: "Created deck '${name}'"]
                pw.print(jo as JSON);
                return;
            } catch (Exception e) {
                log.warn("message: " + e.getMessage());
                if (e.getMessage().contains("primary key violation")) {
                    jo.status = "There is already a deck named '${name}'."
                } else {
                    jo.status = "Could not create the deck."
                }
                pw.print(jo as JSON);
                return;
            }

        } finally {
            pw.close();
        }
    }
}
