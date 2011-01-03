<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
    "http://www.w3.org/TR/html4/loose.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>JSP Page</title>
        <script type="text/javascript" src="http://ajax.googleapis.com/ajax/libs/dojo/1.4.0/dojo/dojo.xd.js" djConfig="parseOnLoad: true" ></script>
        <script type="text/javascript" src="http://ajax.googleapis.com/ajax/libs/dojo/1.4.0/dijit/dijit.xd.js"></script>
        <style type="text/css">
            @import "http://ajax.googleapis.com/ajax/libs/dojo/1.4.0/dijit/themes/tundra/tundra.css";
            @import "http://ajax.googleapis.com/ajax/libs/dojo/1.4.0/dojo/resources/dojo.css"
        </style>
        <script type="text/javascript">
            dojo.require("dijit.form.FilteringSelect");
            dojo.require("dojo.parser");  // scan page for widgets and instantiate them
            dojo.require("dojox.data.QueryReadStore");
            dojo.addOnLoad(function(){
                console.log("hello");
            });
        </script>

    </head>
    <body class="tundra">
        <div dojoType="dojox.data.QueryReadStore"
             jsId="thestore"
             url="AutoComplete"
             requestMethod="post"></div>
        <select  dojoType="dijit.form.FilteringSelect"
                 id="setvaluetest2"
                 name="state1"
                 autoComplete="true"
                 invalidMessage="Invalid state name"
                 store=thestore
                 pageSize=20
                 />
            
        </select>
    </body>
</html>
