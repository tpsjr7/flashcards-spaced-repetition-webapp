package spacedrepserver

import grails.transaction.Transactional

@Transactional
class DatabaseService {

    def removeDeck(Deck deck) {
        Card.findAllByDeck(deck)*.delete()
        deck.delete()
    }

    def createDeck(String name){
        new Deck(name: name).save()
    }
}
