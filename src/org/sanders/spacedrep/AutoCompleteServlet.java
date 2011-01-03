/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sanders.spacedrep;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Ted
 */
public class AutoCompleteServlet extends HttpServlet {

    static final String dataPath = "C:\\Users\\Ted\\Desktop\\some.txt";
    static Logger l = Logger.getLogger(AutoCompleteServlet.class.getName());
    static SortedSet<String> names = Collections.synchronizedSortedSet(new TreeSet<String>());

    static void p(String mess) {
        System.out.println(mess);
    }

    static {
        l.info("Servlet initialized");
        try {
            BufferedReader br = new BufferedReader(new FileReader(dataPath));
            String line;
            int i = 0;
            while ((line = br.readLine()) != null) {
                names.add(line);
                i++;
                if((i%1000)==0){
                    p("on: "+i);
                }
            }
        p("AutoCompleteServlet: counted:"+i+" names");
        } catch (IOException ex) {
            l.log(Level.SEVERE, null, ex);
        }
    }

    private void pause() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {

        }
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
            
            String before = name.replace("*", "") + (char) 0;
            String after = name.replace("*", "") + (char) 999;

            p("before is: " + before);
            p("after is: " + after);
            long start = System.currentTimeMillis();

            SortedSet<String> sub = names.subSet(before, after);
            p("subset time: "+(System.currentTimeMillis() - start));
            
            int startIndex = Integer.parseInt(startString);
            int resultCount = countString==null ? Integer.MAX_VALUE :  Integer.parseInt(countString);
            outputJSON(out,skipElements(startIndex, sub),resultCount);

        } finally {
            out.close();
        }
    }

    private Iterator<String> skipElements(int startIndex, SortedSet<String> sub) {
        Iterator<String> it = sub.iterator();
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
        return "Short description";
    }// </editor-fold>

    private void outputJSON( PrintWriter out, Iterator<String> it, int count) throws JSONException {
        JSONObject jo;
        JSONArray ja = new JSONArray();

        String item;
        for(int i = 0 ; i < count ; i++){
            if(!it.hasNext()){
               break;
            }
            item = it.next();
            jo = new JSONObject();
            jo.put("name", item);
            jo.put("value", item);
            ja.put(jo);
        }

        jo = new JSONObject();
        jo.put("items", ja);
        out.print(jo.toString());
    }
}
