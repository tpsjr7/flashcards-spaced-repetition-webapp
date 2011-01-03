Scheduler = function(){
    this.initialInterval = 2
    this.correctMultiplier = 6
    this.incorrectMultiplier = .5
    
    this.inactiveCards = []
    this.activeCards = []
    
    
    this.earliestCardDue = null
    this.cardTesting = null
    this.nextCardDueDate = null
    this.cardIsScheduledCallback = null
    this.delimiter=","
    this.measureRealIntervals = false
    this.alertOnCardDue = false
    this.fontsize = "60px"
    this.tripletLoad = true
} //constructor

Scheduler.prototype.addWord = function(front, back){
    this.inactiveCards.push({
        front:front,
        back:back, 
        interval:this.initialInterval, 
        timeDue:0,
        lastTimeTested:Number.MAX_VALUE
    })
}

Scheduler.prototype.sortByDueDate = function(a,b){
    return a.timeDue - b.timeDue
}


Scheduler.prototype.getEarliestCardDue = function(){
    if(this.activeCards.length > 0){
        return this.activeCards.sort(this.sortByDueDate)[0]
    }else{
        throw new Error("cannot call getEarliestCardDue with no active cards")
    }
    
}

Scheduler.prototype.pickOverDueCard = function(){
    var acs = this.activeCards
    acs = acs.sort(this.sortByDueDate)
    //alert('make sure its sorting in the right order')
    this.activeCards = acs
    
    var pastDueCards = []
    
    var now = new Date().getTime()
    
    var t =  0,maxFractionOverDue = 0, maxFractionIndex = -1 ,f,overdue
    var hasOverDue = false
    for(var i = 0 ; i < acs.length ; i++){
        t = acs[i].timeDue
        overdue = (t - now ) / 1000 //put it in seconds overdue
        if(overdue < 0){
            f = Math.abs(overdue / acs[i].interval)
            if(f > maxFractionOverDue){
                maxFractionIndex = i
            }
        }else{
            break;
        }
    }
    
    return maxFractionIndex
}

Scheduler.prototype.nextCardOrPause = function(){

        var cardIndex = this.pickOverDueCard()
        
        var now = new Date().getTime()
        
        if(cardIndex==-1){//no overdue cards, 
            //if there are any more inactive cards, pick one to become active


            if(this.inactiveCards.length > 0){
                var card = this.inactiveCards.pop()
                card.timeDue = now 
                this.activeCards.push(card)
                this.cardTesting = card
            }else{
                this.cardTesting = this.getEarliestCardDue() //no more card so just wait
            }
        }else{//picked the card that was most overdue for the size of its interval
            this.cardTesting = this.activeCards[cardIndex]
            this.cardTesting.timeDue = now
        }
        
        var waitTime = this.cardTesting.timeDue - now
        if(waitTime < 0 ){
            throw  ("waitTime was negative: "+waitTime)
        }
        var cardToShow = this.cardTesting
        var self = this
        
        setTimeout(function(){
            self.cardIsScheduledCallback(cardToShow)
        },waitTime)
        
        this.nextCardDueDate = new Date(waitTime + now)
}


Scheduler.prototype.rescheduleCurrentCard = function(bCorrect,timeShown){
    if(this.cardTesting==null) throw new Error("cardTesting cannot be null")
    
    var now = new Date().getTime()
    
    var  diff = timeShown - this.cardTesting.lastTimeTested 
    if(this.measureRealIntervals && diff > this.cardTesting.interval){
        this.cardTesting.interval = diff / 1000
    }
    
    if(bCorrect){
        this.cardTesting.interval = this.cardTesting.interval * this.correctMultiplier
    }else{
        this.cardTesting.interval = this.initialInterval
    }
    
    this.cardTesting.timeDue = now + this.cardTesting.interval*1000
    this.cardTesting.lastTimeTested = now
}
Scheduler.prototype.forgotCard = function(timeShown){
    this.rescheduleCurrentCard(false,timeShown)
    this.nextCardOrPause()
}
Scheduler.prototype.rememberedCard = function( timeShown){

    this.rescheduleCurrentCard(true,timeShown)
    this.nextCardOrPause()
}