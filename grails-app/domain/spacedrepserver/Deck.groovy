package spacedrepserver

class Deck {

    String name


    static constraints = {
        name unique: true
    }
}
