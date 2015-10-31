package spacedrepserver

import grails.converters.JSON

import javax.sql.DataSource

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet;

/**
 *
 * @author Ted
 */
class AutoCompleteController {

    DataSource dataSource

    private List listDecks(String prefix, int limit){

        def decks = []
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = dataSource.getConnection()
            ps = conn.prepareStatement("select id, name from deck where name like ? escape '\' order by name ");
            ps.setString(1, prefix + "%");
            ps.setMaxRows(limit);
            final ResultSet rs = ps.executeQuery();
            for (int c = 0; rs.next() && c < limit; c++) {
                decks << [id: rs.getInt(1), name: rs.getString(2)]
            }
            rs.close();
            ps.close();
            return decks;
        } finally {
            conn.close();
        }
    }

    def index(){
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            String startString = params.start;
            if (!startString) {
                log.warn("Not processing: missing start parameter");
                return;
            }
            String countString = params.count;

            String name = params.name;
            if (!name) {
                log.warn("Not processing: missing name paramer");
                return;
            }

            int resultCount = countString == null ? Integer.MAX_VALUE : Integer.parseInt(countString);
            name = name.replace("*", "");
            name = name.replaceAll("_", "\\\\_");

            def sub = listDecks(name, resultCount);

            int startIndex = Integer.parseInt(startString);
            outputJSON(out, skipElements(startIndex, sub), resultCount);

        } finally {
            out.close();
        }
    }

    private Iterator skipElements(int startIndex, List sub) {
        Iterator it = sub.iterator();
        try {
            for (int i = 0; i < startIndex; i++) {
                it.next();
            }
        } catch (NoSuchElementException e) {
            log.warn("The client asked an out of bounds item. StartIndex was: " + startIndex);
            throw new RuntimeException(e);
        }
        return it;
    }

    private void outputJSON(PrintWriter out, Iterator it, int count) {
        def ja = [];

        for (int i = 0; i < count; i++) {
            if (!it.hasNext()) {
                break;
            }
            def item = it.next();
            ja << [id: item.id, name: item.name]
        }

        def jo = [items: ja] as JSON
        out.print(jo.toString());
    }
}
