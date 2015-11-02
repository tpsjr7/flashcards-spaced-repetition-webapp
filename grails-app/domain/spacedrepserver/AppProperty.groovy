package spacedrepserver

/**
 * Created by tedsandersjr on 10/31/15.
 */
class AppProperty {

    public enum Name {
        DatabaseVersion
    }

    Name name
    String value

    static constraints = {
        name unique: true
    }

    static mapping = {
    }

    public static String lookupValue(Name name){
        return AppProperty.get(name.name())
    }
}
