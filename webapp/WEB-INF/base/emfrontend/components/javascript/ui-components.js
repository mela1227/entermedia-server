formatHitCountResult = function(inRow)
{
	return inRow[1];
}

uiload = function() {

	var app = jQuery("#application");
	var apphome = app.data("home") + app.data("apphome");
	var themeprefix = app.data("home") + app.data("themeprefix");
	
	$('#module-dropdown').click(function(e){
		e.stopPropagation();
		if ( $(this).hasClass('active') ) {
			$(this).removeClass('active');
			$('#module-list').hide();
		} else {
			$(this).addClass('active');
			$('#module-list').show();
		}
	});
	
	$('#maximize').click( function(){
		
		html = $('#maximize').html()
		if ( (html == ' Maximize ') || (html == 'Maximize') ) {
			$('#embody').addClass('max');
			$('#maximize').html('Minimize');
			$('#maximize').attr('title', 'Minimize the application.');

		} else {
			
			$('#embody').removeClass('max');
			$('#maximize').html('Maximize');
			$('#maximize').attr('title', 'Maximize the application.')
			var w1 = 574;
			$('#asset-data').width(w1);
			var w2 = ( $('#main').width() - 261 );
			$('#right-col .liquid-sizer').width(w2);
			var w3 = ( 551 );
			$('#commenttext').width(w3);
		}
			$(window).trigger( "resize" );
		
		toggleUserProperty("maximize_screen");
		
	});
	
	jQuery("input.select2editable").livequery( function() 
	{
	 	var input = jQuery(this);
		var arr = new Array(); //[{id: 0, text: 'story'},{id: 1, text: 'bug'},{id: 2, text: 'task'}]
		
		var ulid = input.data("optionsul");
		
		var options = jQuery("#" + ulid + " li");
		
		if( !options.length )
		{
			return;
		}		
		
		options.each(function() 
		{
			var id = $(this).data('value');
			var text = $(this).text();
			//console.log(id + " " + text);
		 	arr.push({id: id, text: text}); 
		});

		
		//Be aware: calling select2 forces livequery to filter again
	 	input.select2({
				createSearchChoice: function(term, data) 
				{ 
					if ($(data).filter(function() { return this.text.localeCompare(term)===0; } ).length===0) 
					{
						console.log("picking" + term );
						return {id:term, text:term};
					}
				 }
				 , multiple: false
				 , data: arr
		});
	});	
	
	jQuery(".validate-inputs").livequery(
			function() 
			{
//				jQuery(".required",this).each(function()
//				{
//					//jQuery(this).attr("required","true");
//				});
				
				var theform = jQuery(this).closest("form");
				
				theform.on("click", function()
				{
					theform.valid();
				});
				
				theform.validate({
					  ignore: ".ignore"
				});
							
			}
		);
		jQuery("select.ajax").livequery('change',
			function(e) 
			{
				var inlink = jQuery(this);
				var nextpage= inlink.data('href');
				nextpage = nextpage + inlink.val();
				var targetDiv = inlink.data("targetdiv");
				targetDiv = targetDiv.replace(/\//g, "\\/");
				jQuery.get(nextpage, {}, function(data) 
				{
					var	cell = jQuery("#" + targetDiv);
					cell.replaceWith(data);
					$(window).trigger( "resize" );
				});	
			}
		);
	
	jQuery("a.toggle-visible").livequery('click',
			function(e) 
			{
				e.preventDefault();
				var div = jQuery(this).data("targetdiv");
				var target = jQuery("#" + div );
				if(target.is(":hidden"))
				{
					var hidelable = jQuery(this).data("hidelabel");
					jQuery(this).find("span").text(hidelable);
          			target.show();
          		} else {

					var showlabel = jQuery(this).data("showlabel");
					jQuery(this).find("span").text(showlabel);
          			target.hide();
          		}
			}
		);
	
	
	if( jQuery.fn.selectmenu )
	{
		jQuery('.uidropdown select').livequery(
				function()
				{
					jQuery(this).selectmenu({style:'dropdown'});
				}
		);
	}
	
	var browserlanguage =  app.data("browserlanguage");
	if( browserlanguage == undefined )
	{
		browserlanguage = "";
	}
	jQuery.datepicker.setDefaults(jQuery.extend({
		showOn: 'button',
		buttonImage: themeprefix + '/entermedia/images/cal.gif',
		buttonImageOnly: true,
		changeMonth: true,
		changeYear: true, 
		yearRange: '1900:2050'
	}, jQuery.datepicker.regional[browserlanguage]));  //Move this to the layout?
	
	jQuery("input.datepicker").livequery( function() 
	{
		var targetid = jQuery(this).data("targetid");
		jQuery(this).datepicker( {
			altField: "#"+ targetid,
			altFormat: "yy-mm-dd", 
			yearRange: '1900:2050'
		});
				
		var current = jQuery("#" + targetid).val();
		if(current != undefined)
		{
			//alert(current);
			var date;
			if( current.indexOf("-") > 0) //this is the standard
			{
				current = current.substring(2,10);
				//2012-09-17 09:32:28 -0400
				date = jQuery.datepicker.parseDate('yy-mm-dd', current);
			}
			else
			{
				date = jQuery.datepicker.parseDate('mm/dd/yy', current); //legacy support
			}
			jQuery(this).datepicker("setDate", date );					
		}
		jQuery(this).blur(function()
		{
			var val = jQuery(this).val();
			if( val == "")
			{
				jQuery("#" + targetid).val("");
			}
		});
	});
	
	//deprecated, use data-confirm
	jQuery(".confirm").livequery('click',
			function(e)
			{
				var inText = jQuery(this).attr("confirm");
				if(confirm(inText) )
				{
					return;
				}
				else
				{	
					e.preventDefault();
				}
			}
		);
	
	jQuery(".uibutton").livequery(
			function()
			{
				jQuery(this).button();
			}
	);
	jQuery(".fader").livequery(
			function()
			{
				jQuery(this).fadeOut(1600, "linear");
			}
	);
	
	jQuery(".uipanel").livequery(
			function()
			{
				jQuery(this).addClass("ui-widget");
				var header = jQuery(this).attr("header");
				if(header != undefined)
				{
					//http://dev.jquery.it/ticket/9134
					jQuery(this).wrapInner('<div class="ui-widget-content"/>');
					jQuery(this).prepend('<div class="ui-widget-header">' + header + '</div>');					
				}
			}
		);
	
	if( jQuery.fn.tablesorter )
	{
		jQuery("#tablesorter").tablesorter();
	}

	jQuery(".ajaxchange select").livequery(
			function()
			{	
				var select = jQuery(this);
				var div = select.parent(".ajaxchange")
				var url = div.attr("targetpath");
				var divid = div.attr("targetdiv");
				
				select.change( function()
					{
					   var url2 = url + $(this).val();						
					   $("#" + divid).load(url2);
					}
				);	
			}
		);

	
	

	jQuery(".jp-play").livequery("click", function(){
		
	
	//	alert("Found a player, setting it up");
		var player = jQuery(this).closest(".jp-audio").find(".jp-jplayer");
		var url = player.data("url");
		var containerid = player.data("container");
		var container = jQuery("#" + containerid);
		
		player.jPlayer({
	        ready: function (event) {
	        	player.jPlayer("setMedia", {
	                mp3:url
	            }).jPlayer("play");
	        },
	        play: function() { // To avoid both jPlayers playing together.
	        	player.jPlayer("pauseOthers");
			},
	        swfPath: apphome + '/components/javascript',
	        supplied: "mp3",
	        wmode: "window",
	        cssSelectorAncestor: "#" + containerid
	    });
		
		//player.jPlayer("play");

	});


	$('.select-dropdown-open').livequery("click",function(){
		
		if ($(this).hasClass('down')) {
			$(this).removeClass('down');
			$(this).addClass('up');
			$(this).siblings('.select-dropdown').show();
		} else {
			$(this).removeClass('up');
			$(this).addClass('down');
			$(this).siblings('.select-dropdown').hide();
		}
	});
	$('.select-dropdown li a').livequery("click",function(){
		$(this).closest('.select-dropdown').siblings('.select-dropdown-open').removeClass('up');
		$(this).closest('.select-dropdown').siblings('.select-dropdown-open').addClass('down');
		$(this).closest('.select-dropdown').hide();
	});
	
	function select2formatResult(emdata, container, query)
	{
		var element = $(this.element);
		var showicon = element.data("showicon");
		if( showicon )
		{
			var type = element.data("searchtype");
	    	var html = "<img class='autocompleteicon' src='" + themeprefix + "/images/icons/" + type + ".png'/>" + emdata.name;
	    	return html;
		}
		else
		{
			return emdata.name;
		}
	}
	function select2Selected(emdata, container) {

		//"#list-" + foreignkeyid
		var id = container.closest(".select2-container").attr("id");
		id = "list-" + id.substring(5); //remove sid2_
		container.closest("form").find("#" + id ).val(emdata.id);
		return select2formatResult(emdata, container);
	}
	jQuery("select.select2").livequery( function() 
	{
		var theinput = jQuery(this);
		theinput.select2();
	});
	
	
	jQuery("input.listtags").livequery( function() 
	{
		
		var theinput = jQuery(this);
		theinput.select2({tags:[],
			formatNoMatches: function () { return theinput.data("enterdata") ; },
			tokenSeparators: [",","|"],
			separator: '|'
		}).change(function() { $(this).valid();	});

	});

	$(".emtabs").livequery( function()   
	{
		var tabs = $(this); 
		
		var tabcontent = $("#" + tabs.data("targetdiv") );
		
		//active the right tab
		var hash = window.location.hash;
		if( !hash )
		{
			hash = "#" + tabs.data("defaulttab");
		}
		var activelink = $(hash);
		var loadedpanel = $(hash + "panel");
		if( loadedpanel.length == 0)
		{
			loadedpanel = $("#loadedpanel",tabcontent);
			loadedpanel.attr("id",activelink.attr("id") + "panel");
			activelink.data("tabloaded",true);
		}	
		activelink.parent("li").addClass("ui-state-active");
		activelink.data("loadpageonce",false);
		
		$("a",tabs).livequery("click", function (e)   
		{
			e.preventDefault();
			
	    	var link = $(this); // activated tab
			$("li",tabs).removeClass("ui-state-active");
	    	link.parent("li").addClass("ui-state-active");
	    	
		    var id = link.attr("id");

		    var url = link.attr("href");
		    url = url + "#" + id;
			var panelid =  id + "panel";
			var tab = $( "#" + panelid);
			if( tab.length == 0)
			{
			  tab = tabcontent.append('<div class="tab-pane" id="' + panelid + '" ></div>');
			  tab = $( "#" + panelid);
			}	  
			
			var reloadpage = link.data("loadpageonce");
			if( reloadpage )
			{
				if( window.location.href.endsWith( url ) )
				{
					window.location.reload();
				}
				else
				{
					window.location.href = url;
				}
			}
			else
			{
				var loaded = link.data("tabloaded");
				if( link.data("allwaysloadpage") )
				{	
					loaded = false;
				}
				if( !loaded )
				{
					var levels = link.data("layouts");
					if( !levels)
					{
						levels = "1";
					}
					jQuery.get(url , {oemaxlevel:levels}, function(data) 
					{
						tab.html(data);
						link.data("tabloaded",true);
						$(".tab-pane",tabcontent).hide();
						tab.show();
						$(window).trigger( "resize" );
					});
				}
				else
				{
					$(".tab-pane",tabcontent).hide();
					tab.show();
					$(window).trigger( "resize" );
				}
			}	
		});
	});


	jQuery("input.listautocomplete").livequery( function() 
	{
		var theinput = jQuery(this);
		var searchtype = theinput.data('searchtype');
		if(searchtype != undefined ) //called twice due to the way it reinserts components
		{
			var searchfield = theinput.data('searchfield');
			var catalogid = theinput.data('listcatalogid');

			var foreignkeyid = theinput.data('foreignkeyid');
			var sortby = theinput.data('sortby');
			
			//var value = theinput.val();
			theinput.select2({
				placeholder : "Search",
				allowClear: true,
				minimumInputLength : 0,
				ajax : { // instead of writing the function to execute the request we use Select2's convenient helper
					url : apphome + "/components/xml/types/autocomplete/datasearch.txt?catalogid=" + catalogid + "&field=" + searchfield + "&operation=contains&searchtype=" + searchtype,
					dataType : 'json',
					data : function(term, page) 
					{
						var fkv = theinput.closest("form").find("#list-" + foreignkeyid + "value").val();
						if( fkv == undefined )
						{
							fkv = theinput.closest("form").find("#list-" + foreignkeyid).val();
						}
						var search = {
							page_limit : 15,
							page: page
						};
						search[searchfield+ ".value"] = term; //search term
						if( fkv )
						{
							search["field"] = foreignkeyid; //search term
							search["operation"] = "matches"; //search term
							search[foreignkeyid+ ".value"] = fkv; //search term
						}
						if( sortby )
						{
							search["sortby"] = sortby; //search term
						}
						return search;
					},
					results : function(data, page) { // parse the results into the format expected by Select2.
					
						// since we are using custom formatting functions we do not need to alter remote JSON data
						return {
							results : data.rows
						};
					}
				},
				escapeMarkup: function(m) { return m; },
				formatResult : select2formatResult, 
				formatSelection : select2Selected
			});
			
			theinput.on("change", function(e) {
				if( e.val == "" ) //Work around for a bug with the select2 code
				{
					var id = "#list-" + theinput.attr("id");
					jQuery(id).val("");
				}
			});
		}
	});		

	if( jQuery.fn.minicolors )
	{
		$(".color-picker").minicolors({
						defaultValue: '',
						letterCase: 'uppercase'
					});
	}	
}


jQuery(document).ready(function() 
{ 
	uiload();
	$(window).on('resize',function()
	{
		w1 = ( $('#main').width() - $('#left-col').width() - 41 );
		$('#right-col .liquid-sizer').width(w1);
		w2 = ( $('#data').width() - 40 );
		$('#asset-data').width(w2);
		w3 = ( w2 - 23);
		$('#commenttext').width(w3);
	});

}); 


