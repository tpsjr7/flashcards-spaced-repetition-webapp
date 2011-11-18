package org.sanders.spacedrep;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.sanders.spacedrep.Database.Card;
import org.sanders.spacedrep.Database.CardCount;

/**
 * Servlet implementation class CardDealerSerlet
 */
public class CardDealerServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private SchedulerEngine se = new SchedulerEngine();
    /**
     * @see HttpServlet#HttpServlet()
     */
    public CardDealerServlet() {
        super();
        // TODO Auto-generated constructor stub
    }
    

    @Override
    public void destroy(){
    	Database.destroy();
    }
    
    @Override
    public void init(){
    	Database.init();
    }
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	request.setCharacterEncoding("utf-8");
    	response.setContentType("text/html;charset=UTF-8");
		String op = request.getParameter("op");
		PrintWriter pw = response.getWriter();
		try {
			String params = request.getParameter("params");
			System.out.println("params: "+params);
			JSONObject jo = null;
			if(params!=null){
				jo = new JSONObject(params);
			}
			
			if(op.equals("addCards")){
				System.out.println("Addcard jo: "+jo.toString());
				se.addCards(jo);
				return;
			}else if(op.equals("nextCardOrPause")){
				int deck_id = Integer.parseInt(request.getParameter("deck_id"));
				
				boolean learnMore = Boolean.parseBoolean(request.getParameter("learn_more"));
				
				Card c = se.nextCardOrPause(deck_id, learnMore);
				CardCount cc = Database.countActiveCards(deck_id);
				
				JSONObject out = new JSONObject();
				JSONObject cardToShow = new JSONObject();
				cardToShow.put("front", c.foreignWritten);
				cardToShow.put("back", c.pronunciation +" - "+c.translation);
				out.put("cardToShow", cardToShow);
				out.put("timeDue", c.timeDue);
				out.put("card_id", c.id);
				out.put("serverTime", new Date().getTime());
				out.put("ac",cc.activeCards);
				out.put("tc", cc.totalCards);
				pw.print(out);
				return;
			} else if(op.equals("getconfig")){
				jo = new JSONObject();
				jo.put("alertOnCardDue", false);
				jo.put("fontsize", "20px");
				pw.print(jo.toString(4));
				return;
			}else if(op.equals("reschedulecard")){
				int deck_id = Integer.parseInt(request.getParameter("deck_id"));
				boolean bCorrect = Boolean.parseBoolean(request.getParameter("bCorrect"));
				long timeShown = Long.parseLong(request.getParameter("timeShown"));
				int card_id = Integer.parseInt(request.getParameter("card_id"));
				se.rescheduleCard(deck_id, bCorrect, timeShown, card_id);
				return;
			}else{
				throw new RuntimeException("operation '"+op+"' is no supported.");
			}
				
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			throw new ServletException(e);
		}finally{
			pw.close();
		}
	}
	


	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request,response);
	}

}
