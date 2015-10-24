package flashcards

class Deck {

    String name


    static constraints = {
        name unique: true
    }
}
