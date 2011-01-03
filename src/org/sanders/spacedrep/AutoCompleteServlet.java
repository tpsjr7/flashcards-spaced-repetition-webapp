/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sanders.spacedrep;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sanders.spacedrep.Database.NVP;

/**
 *
 * @author Ted
 */
public class AutoCompleteServlet extends HttpServlet {

    /**
	 * 
	 */
	private static final long serialVersionUID = 2837949475656820229L;
	static Logger l = Logger.getLogger(AutoCompleteServlet.class.getName());

    static void p(String mess){ 
        l.info(mess);
    }

    @Override
    public void destroy(){
    	Database.destroy();
    }
    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, JSONException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            String startString = request.getParameter("start");
            if (startString == null) {
                l.severe("Not processing: missing start parameter");
                return;
            }
            String countString = request.getParameter("count");

            
            String name = request.getParameter("name");
            if (name == null) {
                l.severe("Not processing: missing name paramer");
                return;
            }
            p("name is: " + name);
            

            long start = System.currentTimeMillis();
            int resultCount = countString==null ? Integer.MAX_VALUE :  Integer.parseInt(countString);
            List<NVP> sub = Database.listDecks(name.replace("*", "") ,  resultCount);
            
            p("subset time: "+(System.currentTimeMillis() - start));
            
            int startIndex = Integer.parseInt(startString);
            outputJSON(out,skipElements(startIndex, sub),resultCount);

        } finally {
            out.close();
        }
    }

    private Iterator<NVP> skipElements(int startIndex, List<NVP> sub) {
        Iterator<NVP> it = sub.iterator();
        try {
            for (int i = 0; i < startIndex; i++) {
                it.next();
            }
        } catch (NoSuchElementException e) {
            p("The client asked an out of bounds item. StartIndex was: " + startIndex);
            throw new RuntimeException(e);
        }
        return it;
    }
    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">

    /**
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (JSONException ex) {
            l.log(Level.SEVERE, null, ex);
        }
    }

    /** 
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (JSONException ex) {
            l.log(Level.SEVERE, null, ex);
        }
    }

    /** 
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Provides the datasource for the autocomplete dojo drop down.";
    }// </editor-fold>

    private void outputJSON( PrintWriter out,Iterator<NVP> it, int count) throws JSONException {
        JSONObject jo;
        JSONArray ja = new JSONArray();

        NVP item;
        
        for(int i = 0 ; i < count ; i++){
            if(!it.hasNext()){
               break;
            }
            item = it.next();
            jo = new JSONObject();
            jo.put("id", item.id);
            jo.put("name", item.name);
            ja.put(jo);
        }

        jo = new JSONObject();
        jo.put("items", ja);
        out.print(jo.toString());
    }
}
