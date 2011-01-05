Scheduler = function() {

};

Scheduler.prototype.getNextCardDue = function(callback,errorcallback){

	dojo.xhrGet({
		url:"CardDealerServlet",
		params:{op:"getNextCardDue"},
		load:function(data){
			callback(data);
		},
		error:function(data){
			errorcallback(data);
		}
	});
};