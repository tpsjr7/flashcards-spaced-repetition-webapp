package spacedrepserver

import grails.transaction.Transactional

@Transactional
class DatabaseService {

    def removeDeck(Deck deck) {
        deck.delete()
    }

    def createDeck(String name){
        new Deck(name: name).save()
    }
}
