<html>
<head>
<meta http-equiv="Content-Type" content="text/html;charset=utf-8" >
 <meta name="viewport" content="width=device-width; initial-scale=1.0; maximum-scale=1.0;">  
  <script type="text/javascript" src="js/datelib.js"></script>
  <script type="text/javascript" src="js/dojo-release-1.5.0/dojo/dojo.js"></script>
<script type="text/javascript">
/*
{
correctMultiplier : 4,
initialInterval : 2,
delimiter: "    ",
measureRealIntervals : true,
alertOnCardDue : true,
fontsize: "40px",
tripletLoad : true
}

*/

        var djConfig = {
            parseOnLoad: false,
            isDebug: false,
            locale: 'en-us',
            debugAtAllCosts: false
        };
</script>

<script type="text/javascript"  >

var deckConfig;
var deckId= <%= request.getParameter("deck_id") %>;
var currentCard = null
var card_id = null
var serverTimeOffset = 0 //how much the server time differs from this computer's local time
var timeShownFront;
var timeShownBack; //time when the user clicked show answer
var responseTime; // time it takes for user to click show answer in milliseconds
dojo.addOnLoad(function(){
	loadDeckConfig(function(){
		dojo.byId("current-word-front").style.fontSize=deckConfig.fontsize;
		//dojo.byId('current-word-front').value = ""
		setInterval(function(){
			dojo.byId("time-now").innerHTML=new Date().toString("h:mm:ss")
		},1000)
		nextCardOrPause();
	});
})

function showNewCardButton(card){
    var b = dojo.byId('show-card-button')
    b.style.display="block"
    b.onclick = function(){showNewCard(card)}
    if(deckConfig.alertOnCardDue){
        alert('card due')
    }
}

function showNewCard(card){
	dojo.byId('show-card-button').style.display="none";
    dojo.byId('current-card').style.display="block"
	dojo.byId('show-answer-button').style.display="inline";
    dojo.byId('current-word-front').innerHTML = card.front
    timeShownFront=new Date().getTime() + serverTimeOffset
    currentCard = card
}

function loadDeckConfig(callback){
	dojo.xhrGet({
		url:"CardDealerServlet",
		content:{op:"getconfig"},
		load:function(data){
			deckConfig=dojo.fromJson(data)
			callback();
		},
		error:function(err){
			alert('unable to load deck config')
		}
	});
}

function addCards(/* json array */ cards, callback,errorcallback){
	var params = {
		deckId:deckId,
		cards:cards
	}
	dojo.xhrPost({
		url:"CardDealerServlet",
		content:{op:"addCards", params: dojo.toJson(params)},
		load:callback,
		error:errorcallback
	});
	
}

function setupForNextCard(nextCardDueDate){
    dojo.byId('show-card-button').style.display="none"
    dojo.byId('answer-div').style.display="none"
    dojo.byId('current-word-front').innerHTML = ""
    dojo.byId('current-card').style.display="none"
    dojo.byId('card-due').innerHTML = nextCardDueDate == null ? "(none)" :  nextCardDueDate.toString("h:mm:ss")
}

function showAnswer(){
	timeShownBack = new Date().getTime() + serverTimeOffset
	responseTime =  timeShownBack  - timeShownFront
    dojo.byId("response-time").innerHTML = responseTime /1000;
    dojo.byId('answer-div').style.display="block";
    dojo.byId('answer-span').innerHTML=currentCard.back;
	dojo.byId('show-answer-button').style.display="none";
}

/**
 * nextCardOrPause - 
 * retrieves the next scheduled card if there is one 
 * and sets a timeout to show it when it is due.
 * 
 * Also gets the new card counts stats to display as <active>/<total>.
 * It will not activate any new cards if "learn more" is not checked, unless
 * the argument learnMore is true ( when the One More button is pressed)
 * 
 * Shows the uncover button based if the "show-uncover-button" checkbox is checked.
 */
nextCardOrPause_timer=null
function nextCardOrPause(learnMore){
	if(nextCardOrPause_timer!=null){
 		clearTimeout(nextCardOrPause_timer)
		nextCardOrPause_timer = null;
	}

	dojo.xhrGet({
		url:"CardDealerServlet",
		content:{op:"nextCardOrPause",deck_id:deckId, learn_more: dojo.byId('learn-more').checked || learnMore},
		error:function(err){
			alert('could not retrieve the next card')
		},
		load:function(data){
			var jo = dojo.fromJson(data)
			var now = new Date().getTime()
			card_id = jo.card_id
			serverTimeOffset = jo.serverTime - now;
			dojo.byId('active-count').innerHTML = jo.ac;
			dojo.byId('total-count').innerHTML = jo.tc;
			dojo.byId("due-count").innerHTML = jo.dc;
			if(card_id != -1){
				var wait = jo.timeDue - jo.serverTime;

				wait = wait > 0 ? wait : 0
				var nextCardDueDate = new Date(now+wait)
				setupForNextCard(nextCardDueDate)
				nextCardOrPause_timer=setTimeout(function(){
					if(dojo.byId('show-uncover-button').checked){
						showNewCardButton(jo.cardToShow)
					}else{
						showNewCard(jo.cardToShow)
					}
				},wait );
			}else{
				setupForNextCard(null)
			}
		}
	});
}


	function tripletParse(lines){
	    if(lines.length % 3 != 0){
	        throw new Error("Lines must be a multiple of 3 when parsing triplets.")
	    }
	    var cards = []
	    var eng
	    var pinyin
	    var chars
	    while(lines.length > 0){
	        eng = lines.pop()
	        pinyin = lines.pop()
	        chars = lines.pop()
	        cards.push({
	            written: chars,
	            pronunciation: pinyin,
	            translation: eng
	        })
	    }
	    var revcards = []
		while(cards.length > 0){
			revcards.push(cards.pop())
		}
	    return revcards
	}

	function parseInputWords() {
		try {
			var input = dojo.trim(dojo.byId("input-words").value)
			if (input == "") {
				return
			}
			var lines = input.split('\n')
			console.log("lines.length: " + lines.length)
			var cards = tripletParse(lines)
			addCards(cards,
				function(data){
					dojo.byId("input-words").value = "";
					nextCardOrPause();
				},
				function(err){
					alert('failed to add cards '+dojo.toJson(c));
				}
			);
			
		} catch (e) {
			alert(e)
		}
	}

	function rescheduleCurrentCard(iAnswer){
		dojo.xhrGet({
			url:"CardDealerServlet",
			content:{
				op:"reschedulecard",
				deck_id:deckId,	
				a:iAnswer,
                av:0, //answer version, 
				timeShownBack:timeShownBack,
				card_id:card_id,
				rt: responseTime
			},
			load:function(data){
				nextCardOrPause();
			},
			error:function(err){
				alert('unable to reschedule: '+dojo.toJson(err))
			}
			
		});
	}
</script>


</head>
<body><center>
		<!--
Input words:<br/>
<textarea value="hi" cols=30 rows=3 id="input-words"></textarea>
<br/>
<input type="button" value="Parse" onclick="parseInputWords()"/>
  -->
<!-- <input type="button" value="Load Config" onclick="reloadConfig()"/> -->

<label for="show-uncover-button">Uncover button</label>
<input type="checkbox" checked="no" id="show-uncover-button" />

<label for="learn-more">Learn  More</label>
<input type="checkbox" checked="yes" id="learn-more" onchange="nextCardOrPause()"/>

<!--
<input type="button" value="One More" onclick="nextCardOrPause(true)" />
-->
<br/>
Card due: <span id="card-due">none</span> Now:<span id="time-now"></span>
<br/>

Response Time: <span id="response-time">N/A</span>
<br/>

<span id="active-count">0</span>/<span id="total-count">0</span>/<span id="due-count">0</span>

<div id="show-card-button" style="display:none">
    <input type="button" value="Show Card" />
</div>

<div id="current-card" style="display:none">
    <!-- Current card:<br/> -->
	<div style="margin:8px">
		<span id="current-word-front"  style="border: solid black 1px;padding: 7px; " ></span>
	</div>
    
    <input type="button" value="Show answer" id="show-answer-button" onclick="showAnswer()"/>
</div>

<div id="answer-div" style="display:none">
    <div>
    <div id="answer-span" style="border: solid black 1px;padding: 10px; margin:5px">
    The answer
    </div>
    </div>
    <div style="margin:20px 0px">
    <input type="button" value="No Clue" onclick="rescheduleCurrentCard(0)"/>
    <input type="button" value="Close" onclick="rescheduleCurrentCard(1)"/>
    <input type="button" value="Correct" onclick="rescheduleCurrentCard(2)"/>
	<input type="button" value="Already Know" onclick="rescheduleCurrentCard(3)"/>
    </div>
</div>
</center>
  
</body>
</html>
