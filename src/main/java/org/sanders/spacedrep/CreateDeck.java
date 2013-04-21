package org.sanders.spacedrep;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Servlet implementation class CreateDeck
 */
public class CreateDeck extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public CreateDeck() {
    }

    @Override
    public void destroy() {
        Database.destroy();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     * response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
     * response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter pw = null;
        request.setCharacterEncoding("UTF-8");
        try {
            pw = response.getWriter();
            String name = request.getParameter("deck-name");
            name = name.toLowerCase();
            if (!name.equals(name.replaceAll("[^a-z0-9_]+", ""))) {
                pw.print(new JSONObject() {
                    {
                        put("status", "Deck name can only contain characters a-z or 0-9 or _");
                    }
                });
                return;
            }
            JSONObject jo = new JSONObject();
            jo.put("status", "unknown status");
            try {
                int deck_id = Database.createDeck(name);
                jo.put("status", "Created deck '" + name + "'");
                pw.print(jo.toString());
                return;
            } catch (Exception e) {
                System.out.println("message: " + e.getMessage());
                if (e.getMessage().contains("primary key violation")) {
                    pw.print("{ status:\"There is already a deck named '" + name + "'.\" } ");
                } else {
                    e.printStackTrace();
                    pw.print("{ status:\"Could not create the deck.\" } ");
                }
                return;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            pw.close();
        }
    }
}
