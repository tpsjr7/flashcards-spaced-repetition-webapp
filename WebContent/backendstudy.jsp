<html>
<head>
<meta http-equiv="Content-Type" content="text/html;charset=utf-8" >
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

dojo.addOnLoad(function(){
	loadDeckConfig(function(){
		dojo.byId("current-word-front").style.fontSize=deckConfig.fontsize;
		dojo.byId('current-word-front').value = ""
		setInterval(function(){
			dojo.byId("time-now").innerHTML=new Date().toString("h:mm:ss")
		},1000)
		onCheckboxChange()
		nextCardOrPause(showDueTime);
	});
})

function showDueTime(){
	dojo.byId('card-due').innerHTML = nextCardDueDate.toString("h:mm:ss")
}
function showNewCardButton(card){
    var b = dojo.byId('show-card-button')
    b.style.display="block"
    b.onclick = function(){showNewCard(card)}
    if(deckConfig.alertOnCardDue){
        alert('card due')
    }
}

function showNewCard(card){
    dojo.byId('current-card').style.display="block"
    dojo.byId('current-word-front').value = card.front
    timeShown=new Date().getTime()
    currentCard = card
}

function onCheckboxChange(){
    if(dojo.byId('chkbox').checked){
        cardIsScheduledCallback  = showNewCardButton
    }else{
        cardIsScheduledCallback  = showNewCard
    }
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

function setupForNextCard(){
    dojo.byId('show-card-button').style.display="none"
    dojo.byId('answer-div').style.display="none"
    dojo.byId('current-word-front').value = ""
    dojo.byId('current-card').style.display="none"
    showDueTime();
}

function showAnswer(){
    dojo.byId('answer-div').style.display="block"
    dojo.byId('answer-span').innerHTML=currentCard.back
}

function nextCardOrPause(callback){
	dojo.xhrGet({
		url:"CardDealerServlet",
		content:{op:"nextCardOrPause",deck_id:deckId},
		error:function(err){
			alert('could not retrieve the next card')
		},
		load:function(data){
			var jo = dojo.fromJson(data)
			var now = new Date().getTime()
			var wait = jo.timeDue - now;
			card_id = jo.card_id
			if(wait > 0){
				nextCardDueDate = new Date(jo.timeDue)
				setTimeout(function(){
	        		cardIsScheduledCallback(jo.cardToShow)
	    		},wait )
			}else{
				nextCardDueDate = new Date(now)
				cardIsScheduledCallback(jo.cardToShow)
			}
			
    		if(callback!=null){
    			callback();
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
	    return cards
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
					dojo.byId("input-words").value = ""
					nextCardOrPause()
				},
				function(err){
					alert('failed to add cards '+dojo.toJson(c));
				}
			);
			
		} catch (e) {
			alert(e)
		}
	}

	function markCardCorrect(){
		_rescheduleCurrentCard(true)
	}

	function markCardWrong(){
		_rescheduleCurrentCard(false)
	}

	function _rescheduleCurrentCard(bCorrect){
		dojo.xhrGet({
			url:"CardDealerServlet",
			content:{
				op:"reschedulecard",
				deck_id:deckId,	
				bCorrect:bCorrect,
				timeShown:timeShown,
				card_id:card_id
			},
			load:function(data){
				nextCardOrPause(setupForNextCard);
				
			},
			error:function(err){
				alert('unable to reschedule: '+dojo.toJson(err))
			}
			
		});
	}
</script>


</head>
<body><center>
Input words:<br/>
<textarea value="hi" cols=30 rows=3 id="input-words"></textarea>
<br/>
<input type="button" value="Parse" onclick="parseInputWords()"/>
<input type="button" value="Load Config" onclick="reloadConfig()"/>

<label for="chkbox">Uncover button</label>
<input type="checkbox" checked="yes" id="chkbox" onChange="onCheckboxChange()"/>


<br/>
Next card due: <span id="card-due">none</span>
<br/>
Time Now:<span id="time-now"></span>
<br/>

<div id="show-card-button" style="display:none">
    <input type="button" value="Show Card" />
</div>

<div id="current-card" style="display:none">
    Current card:<br/>
    <textarea cols=10 rows=2 id="current-word-front"></textarea>
    <br/>
    <input type="button" value="Show answer" onclick="showAnswer()"/>
</div>

<div id="answer-div" style="display:none">
    <div>
    <div id="answer-span" style="border: solid black 1px;padding: 10px; margin:10px">
    The answer
    </div>
    </div>
    <div style="margin:20px 0px">
    <input type="button" value="Correct" onclick="markCardCorrect()"/>
    <input type="button" value="Wrong" onclick="markCardWrong()"/>
    </div>
</div>
</center>
  
</body>
</html>
