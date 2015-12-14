<html>
<head>
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8" >
    <meta name="viewport" content="width=device-width; initial-scale=1.0; maximum-scale=2.0;">
    <script type="text/javascript" src="js/datelib.js"></script>
    <script type="text/javascript" src="//code.jquery.com/jquery-1.11.3.min.js"></script>
    <script type="text/javascript" src="//cdnjs.cloudflare.com/ajax/libs/json2/20150503/json2.min.js"></script>

    <!-- Latest compiled and minified CSS -->
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css" integrity="sha384-1q8mTJOASx8j1Au+a5WDVnPi2lkFfwwEAa8hDDdjZlpLegxhjVME1fgjWPGmkzs7" crossorigin="anonymous">

    <!-- Optional theme -->
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap-theme.min.css" integrity="sha384-fLW2N01lMqjakBkx3l/M9EahuwpSfeNvV63J5ezn3uZzapT0u7EYsXMjQV+0En5r" crossorigin="anonymous">

    <!-- Latest compiled and minified JavaScript -->
    <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/js/bootstrap.min.js" integrity="sha384-0mSbJDEHialfmuBBQP6A4Qrprq5OVfW37PRR3j5ELqxss1yVqOtnepnHVP9aJ7xS" crossorigin="anonymous"></script>

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

//    var Notification = window.Notification || window.mozNotification || window.webkitNotification;
//
//    Notification.requestPermission(function (permission) {
//    // console.log(permission);
//    });
</script>

<script type="text/javascript">

var deckConfig;
var deckId = ${params.deck_id};
var currentCard = null
var card_id = null
var serverTimeOffset = 0 //how much the server time differs from this computer's local time
var timeShownFront;
var timeShownBack; //time when the user clicked show answer
var responseTime; // time it takes for user to click show answer in milliseconds
$(document).ready(function(){
	loadDeckConfig(function(){
		$("#current-word-front")[0].style.fontSize=deckConfig.fontsize;
		//$('#current-word-front').value = ""
		setInterval(function(){
			$("#time-now").innerHTML=new Date().toString("h:mm:ss")
		},1000)
		nextCardOrPause();
	});
});

function showNewCardButton(card){
    var b = $('#show-card-button')[0];
    b.style.display="block"
    b.onclick = function(){showNewCard(card)}
    if($('#show-alert').checked){
          alert('card due');
//        new Notification("Card is due");
    }
}

function showNewCard(card){
	$('#show-card-button')[0].style.display="none";
    $('#current-card')[0].style.display="block"
	$('#show-answer-button')[0].style.display="inline";
    $('#current-word-front')[0].innerHTML = card.front
    timeShownFront=new Date().getTime() + serverTimeOffset
    currentCard = card
}

function loadDeckConfig(callback){
	$.ajax({
		url:"cardDealer",
		data:{op:"getconfig"}
	}).done(function(data){
        deckConfig=data;
        callback();
    }).fail(function(err){
        alert('unable to load deck config')
    });
}

function addCards(/* json array */ cards, callback,errorcallback){
	var params = {
		deckId:deckId,
		cards:cards
	}
	$.ajax({
        method: "POST",
		url:"cardDealer",
		data:{op:"addCards", params: stringify(params)}
	}).done(callback).fail(errorcallback);
	
}

function setupForNextCard(nextCardDueDate){

    $('#card-due')[0].innerHTML = nextCardDueDate == null ? "(none)" :  nextCardDueDate.toString("h:mm:ss")

    if(!$('#learn-more')[0].checked){
        $("#activate-another")[0].style.display = "block";
    }
}

function showAnswer(){
	timeShownBack = new Date().getTime() + serverTimeOffset
	responseTime =  timeShownBack  - timeShownFront
    $("#response-time")[0].innerHTML = responseTime /1000;
    $('#answer-div')[0].style.display="block";
    $('#answer-span')[0].innerHTML=currentCard.back;
	$('#show-answer-button')[0].style.display="none";
    $('#add-another').css('display',"none");
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
 *
 * If suppress is true - don't show an alert and dont show the uncover button because we just clicked
 * the activate new card button.
 */
nextCardOrPause_timer=null
function nextCardOrPause(learnMore, suppress){
	if(nextCardOrPause_timer!=null){
 		clearTimeout(nextCardOrPause_timer)
		nextCardOrPause_timer = null;
	}

	$.ajax({
		url:"cardDealer",
		data:{
            op:"nextCardOrPause",
            deck_id:deckId,
            learn_more: $('#learn-more')[0].checked || learnMore
        }
    }).fail(function(err){
        alert('could not retrieve the next card')
    }).done(function(data){
        var jo = data;
        var now = new Date().getTime();
        card_id = jo.card_id;
        serverTimeOffset = jo.serverTime - now;
        $('#active-count')[0].innerHTML = jo.ac;
        $('#total-count')[0].innerHTML = jo.tc;
        $("#due-count")[0].innerHTML = jo.dc;
        if(card_id != -1){ // if there's a card to show or will be shown
            var wait = jo.timeDue - jo.serverTime;

            wait = wait > 0 ? wait : 0 // turn negative values into 0
            var nextCardDueDate = new Date(now+wait);
            setupForNextCard(nextCardDueDate);
            nextCardOrPause_timer=setTimeout(function(){
                $("#activate-another")[0].style.display = "none";
                if(!suppress && $('#show-uncover-button')[0].checked){
                    showNewCardButton(jo.cardToShow)
                }else{
                    showNewCard(jo.cardToShow)
                }
            },wait );
        }else{
            // no card scheduled
            setupForNextCard(null)
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
			var input = $.trim($("#input-words")[0].value)
			if (input == "") {
				return
			}
			var lines = input.split('\n')
			console.log("lines.length: " + lines.length)
			var cards = tripletParse(lines)
			addCards(cards,
				function(data){
					$("#input-words")[0].value = "";
					nextCardOrPause();
				},
				function(err){
					alert('failed to add cards ' + JSON.stringify(c));
				}
			);
			
		} catch (e) {
			alert(e)
		}
	}

	function rescheduleCurrentCard(iAnswer){
		$('#answer-div')[0].style.display="none"
		$('#show-card-button')[0].style.display="none"
		$('#current-word-front')[0].innerHTML = ""
		$('#current-card')[0].style.display="none"
		$.ajax({
			url:"cardDealer",
			data:{
				op:"reschedulecard",
				deck_id:deckId,	
				a:iAnswer,
                av:0, //answer version, 
				timeShownBack:timeShownBack,
				card_id:card_id,
				rt: responseTime
			}
		}).done(nextCardOrPause).fail(function( jqXHR, textStatus, errorThrown){
            alert('unable to reschedule: '+ errorThrown);
        })
	}
</script>


</head>
<body><center>
Input words:<br/>
<textarea value="hi" cols=30 rows=3 id="input-words"></textarea>
<br/>
<input type="button" value="Parse" onclick="parseInputWords()"/>
<!-- <input type="button" value="Load Config" onclick="reloadConfig()"/> -->

<label for="show-uncover-button">Uncover button</label>
<input type="checkbox" id="show-uncover-button" />

<label for="learn-more">Learn More</label>
<input type="checkbox" checked="yes" id="learn-more" onchange="nextCardOrPause()"/>

<label for="show-alert">Alert</label>
<input type="checkbox" id="show-alert"/>

<br/>
Card due: <span id="card-due">none</span> Now:<span id="time-now"></span>
<br/>

Response Time: <span id="response-time">N/A</span>
<br/>

<span id="active-count">0</span>/<span id="total-count">0</span>/<span id="due-count">0</span>

<hr/>

<div id="show-card-button" style="display:none">
    <input type="button" value="Show Card" />
</div>

<div id="current-card" style="display:none">
    <!-- Current card:<br/> -->
	<div style="margin:15px">
		<!--<span id="current-word-front"  style="border: solid black 1px;padding: 10px; " ></span> -->
		<span id="current-word-front"  style="padding: 10px; " ></span>
	</div>
    
    <input type="button" value="Show answer" id="show-answer-button" onclick="showAnswer()"/>
</div>

<div id="answer-div" style="display:none">
    <div style="margin:20px 0px">
    <input type="button" value="No Clue" onclick="rescheduleCurrentCard(0)"/>
    <input type="button" value="Close" onclick="rescheduleCurrentCard(1)"/>
    <input type="button" value="Correct" onclick="rescheduleCurrentCard(2)"/>
	<input type="button" value="Already Know" onclick="rescheduleCurrentCard(3)"/>
    </div>
    <div>
    <div id="answer-span" style="border: solid black 1px;padding: 10px; margin:5px">
    The answer
    </div>
    </div>
</div>

<div id="activate-another" style="display:none">
    <input type="button" value="Activate Another" onclick="nextCardOrPause(true, true)"/>
</div>

</center>
  
</body>
</html>
