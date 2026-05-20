
/** 
 EnterMediaDB javascriptGenerator : 404 NOT FOUND/${applicationid}/components/javascript/jquery.form.js
/finder/find/_site.xconf
  **/





/** 
 EnterMediaDB javascriptGenerator : 404 NOT FOUND/${applicationid}/components/javascript/validation/jquery.validate.min.js
/finder/find/_site.xconf
  **/





/** 
 EnterMediaDB javascriptGenerator : 404 NOT FOUND/${applicationid}/components/javascript/emedia/ajax/ajaxformsubmit.js
/finder/find/_site.xconf
  **/





/** 
 EnterMediaDB javascriptGenerator : 404 NOT FOUND/${applicationid}/components/javascript/emedia/liveajax.js
/finder/find/_site.xconf
  **/





/** 
 EnterMediaDB javascriptGenerator : /openedit/components/javascript/openedit.js
/finder/find/_site.xconf
 Modified: Tue May 05 23:52:52 CST 2026 Size: 8.27 KB **/

jQuery(document).ready(function () {
	//insert a chunk of html
	var body = jQuery("body");
	var hide = body.data("hidetoolbar");
	var isBlockfind = $("#application").hasClass("blockfind");
	//console.log(hide);

	if (hide != true && !isBlockfind) {
		var path = window.location.pathname;
		if (window.location.search) {
			path = path + window.location.search;
		}

		jQuery.get(
			"/openedit/components/toolbar/admintoolbarselector.html",
			{ path: path },
			function (data) {
				body.prepend(data);

				loadToolbar();
			},
		);

		$(document).on("click", ".oe-enableedit", function () {
			var apphome = jQuery("#application").data("apphome");
			//<li><a href="$home/openedit/views/workflow/mode/viewdebug.html?origURL=$origURL" ><img src="$home/openedit/theme/images/toolbar/modepreview.gif" border="0" title="Debug Mode" />Debug Mode</a></li>

			var args = $(this).data();
			//Enable dashes on
			$.get(href, args, function (data) {
				//reload page
			});
		});

		$(document).on("click", ".oeDialog", function () {
			var target = $(this).data("target");
			if (target == null) {
				target = $(this).attr("href");
			}
			var title = $(this).data("title");
			if (title != null) {
				$("#modal-title").text(title);
			}
			$("#edit-modal-body").load(target, function (result) {
				var textareas = jQuery(".htmleditor");
				if (textareas.length > 0) {
					loadEditors();
				}
				$("#editmodal").modal("show");
			});
			return false;
		});

		$(document).on("click", ".oemodechange", function (e) {
			e.preventDefault();
			var target = $(this).attr("href");
			var title = $(this).attr("title");
			jQuery.get(target, function () {
				if (title != "DebugMode") {
					window.location.reload();
				}
			});
		});

		jQuery("a.openeditdialog").each(function () {
			var height = jQuery(window).height();
			var width = jQuery(window).width();
			height = height * 0.9;
			width = width * 0.9;
			if (width < 900) {
				width = 1050;
			}

			var newfancy = jQuery(this).fancybox({
				zoomSpeedIn: 300,
				zoomSpeedOut: 300,
				overlayShow: true,
				enableEscapeButton: true,
				type: "iframe",
				height: height,
				width: width,
			});
		});

		//OLD Approach
		jQuery("a.oeinlineedit").on("click", function (e) {
			e.preventDefault();
			e.stopPropagation();

			var parent = $(this).closest(".openeditinline");
			var container = parent.find(".openediteditcontent");

			var editpath = parent.data("editpath");

			// var catalogid = app.data("catalogid");
			//alert("using " + catalogid);
			var home = $("#openedit").data("home");
			if (!home) {
				home = "";
			}
			var savepath = home + "/openedit/components/html/save.html";

			container.data("savepath", savepath);
			container.data("editpath", editpath);

			$(window).trigger("inlinehtmlstart", [container]);

			/*
			CKEDITOR.config.saveSubmitURL = savepath + "?editPath=" + editpath; //TODO: Save this URL specific to this editor
			CKEDITOR.config.filebrowserBrowseUrl =
				home + "/openedit/components/html/browse/index.html?editPath=$editPath";
			CKEDITOR.config.filebrowserUploadUrl =
				home + "/openedit/components/html/edit/actions/imageupload-finish.html";
			CKEDITOR.config.filebrowserImageBrowseUrl =
				home + "/openedit/components/html/browse/index.html?editPath=$editPath";
			CKEDITOR.config.filebrowserImageUploadUrl =
				home + "/openedit/components/html/edit/actions/imageupload-finish.html";
			CKEDITOR.config.entities = false;
			CKEDITOR.config.basicEntities = true;
			var content = container.find(".openediteditcontent").get(0);
			//var content = jQuery(".openediteditcontent" ).get(0);
			content.setAttribute("contenteditable", "true");
			var editor = CKEDITOR.inline(content, {
				extraConfig: { oldcontent: "null" },
				startupFocus: true,
				on: {
					dataReady: function (event) {
						event.editor.config.extraConfig.oldcontent = event.editor.getData();
					},
					blur: function (event) {
						var enCollator = new Intl.Collator("en");
						var data = event.editor.getData();

						if (
							enCollator.compare(data, editor.config.extraConfig.oldcontent) !=
							0
						) {
							$(window).on("beforeunload", function () {
								return "You have unsaved changes.  Reloading will loose these changes.";
							});
						}
						return false;
					},
					savecontentdone: function (event) {
						location.reload();
					},
				},
			});
			*/
		});
	}

	var content = $("textarea.oeeditorhtml");
	if (content.length) {
		$(window).trigger("edithtmlstart", [content]);
	}

	loadHtmlEditor = function (field, viewtype, container) {
		var apphome = jQuery("#application").data("apphome");

		var home = $("#openedit").data("home");
		if (!home) {
			home = "";
		}

		if (viewtype == "html") {
			var savepath = home + apphome + "/components/data/save.html";
			container.data("savepath", savepath);
			var dataid = container.data("dataid");
			if (dataid) {
				container.data("id", dataid);
			}
			$(window).trigger("edithtmlstart", [container]);
		} else if (viewtype == "input") {
			var savepath = home + apphome + "/components/data/save.html";
			var oldborder = container.css("border");
			container.css("border", "1px dashed black");
			content.setAttribute("contenteditable", "true");
			container.focus();
			var options = container.data();
			options.save = true;
			options.oemaxlevel = 1;
			options.id = options.dataid;
			var field = options.field;
			container.keyup(function (evt) {
				//save as we go?
				// enter pressed
				var contents = compoent.innerHTML;
				options[field + ".value"] = contents;

				//TODO: use "post" method for larger inputs not "get"
				$.get(savepath, options, function () {
					//reset border
					//content.setAttribute('contenteditable', 'false');
					container.css("border", oldborder);
				});
			});
			//Capture the enter key
		}
	};

	//onload

	var editmode = jQuery("#application").data("editmode");

	if (editmode == "postedit") {
		var container = $(".oe-editable");
		$(window).trigger("edithtmlstart", [container]);
	}

	//THis is a click that enabled something else to edit. Like a pencil icon
	jQuery(document).on("click", ".oe-dataedit", function (e) {
		console.log("Opening editor");
		var container = $(this).data("target");
		container = $(container);
		// var content = container.get(0);
		// var searchtype = container.data("searchtype");
		// var id = container.data("dataid");
		var field = container.data("field");
		var viewtype = container.data("viewtype");
		if (!viewtype) {
			viewtype = "html";
		}
		var home = $("#openedit").data("home");
		if (!home) {
			home = "";
		}
		e.preventDefault();

		loadHtmlEditor(field, viewtype, container);

		return false;
	});

	var htminp = jQuery(".oehtmlinput");
	if (htminp.length > 0) {
		var field = htminp.data("field");
		var viewtype = "html";
		loadHtmlEditor(field, viewtype, htminp);
	}

	jQuery("form.oeajaxform").on("submit", function () {
		var targetdiv = jQuery(this).attr("targetdiv");
		targetdiv = targetdiv.replace(/\//g, "\\/");
		//allows for posting to a div in the parent from a fancybox.
		if (targetdiv.indexOf("parent.") == 0) {
			targetdiv = targetdiv.substr(7);
			parent.jQuery(this).ajaxSubmit({
				target: "#" + targetdiv,
				success: function () {
					$(document).trigger("domchanged", "#" + targetdiv);
				},
			});

			//closes the fancybox after submitting
			parent.jQuery.fn.fancybox.close();
		} else {
			jQuery(this).ajaxSubmit({
				target: "#" + targetdiv,
				success: function () {
					$(document).trigger("domchanged", "#" + targetdiv);
				},
			});
		}
		return false;
	});
});

loadToolbar = function () {
	jQuery("#oeselector").on("mouseenter", function () {
		if (jQuery("#oeadmintoolbarlocation").is(":visible")) {
			return;
		}
		var me = jQuery(this);
		jQuery.get(me.attr("href"), {}, function (data) {
			me.html(data);
			jQuery("#oeadmintoolbarlocation").on("mouseleave", function () {
				jQuery(this).hide();
			});
		});
	});
};

showHover = function (inAssetId) {
	var el = document.getElementById("oehover");
	el = jQuery(el);
	if (el.attr("status") == "show") {
		if (inAssetId == el.attr("assetid")) {
			el.show();
		}
	}
};

refreshFileMenu = function () {
	var editpath = $("#fileoptionsmenu").data("editpath");

	var home = $("#openedit").data("home");
	if (!home) {
		home = "";
	}
	$("#fileoptionsmenu").load(
		home +
			"/openedit/components/html/edit/menu.html?oemaxlevel=1&editPath=" +
			editpath,
	);
};




//Ended: /openedit/components/javascript/openedit.js Size: 8.27 KB


/** 
 EnterMediaDB javascriptGenerator : 404 NOT FOUND/${applicationid}/components/javascript/select2/4.1.0rc0/js/select2.full.js
/finder/find/_site.xconf
  **/





/** 
 EnterMediaDB javascriptGenerator : 404 NOT FOUND/${applicationid}/components/javascript/jplayer/jquery.jplayer.min.js
/finder/find/_site.xconf
  **/





/** 
 EnterMediaDB javascriptGenerator : /${applicationid}/components/xml/detaileditor.js
/finder/find/_site.xconf
 Modified: Wed May 20 10:42:48 CST 2026 Size: 1.39 KB **/



Array.prototype.index = function(val) {
  for(var i = 0, l = this.length; i < l; i++) {
    if(this[i] == val) return i;
  }
  return null;
}

Array.prototype.include = function(val) {
  return this.index(val) !== null;
}

var DetailEditor = {
	find_field_from_row: function(detail){
		
		if(detail.constructor == String) detail = jQuery('.detaileditor #inputarea_' + detail);
		var result = detail.find('input[type!="hidden"], textarea, select').filter('input:not(.nodependsondefault),select:not(.nodependsondefault),textarea:not(.nodependsondefault)'); 

		return 	result;
	},
	
	toggle_dependency_row: function(row,dependant,on,default_value,delimiter){
		if(default_value == '') default_value = 'N/A';
		if(delimiter == '') delimiter = ',';
		
		e = jQuery('.detaileditor #inputarea_' + row);
		i = DetailEditor.find_field_from_row(e);
		currentval = dependant.val();
		
		if(dependant.is(":checkbox")) {
			if(!dependant.attr('checked')){
				currentval = "";
			}
			
		}
		
		if(on.split(delimiter).include(currentval)){
			if(i.val() == default_value) 
				i.val('');
				
			e.show();
		}
		else{
			e.hide();
			
			if(i.val() == '') 
				i.val(default_value);
		}		
	}
};
	
	
jQuery.fn.extend({
	bind_and_run: function(trigger,fn){		
		e = jQuery(this);

		e.bind(trigger + '_and_run',fn);
		e.bind(trigger,function(){jQuery(this).trigger(trigger + '_and_run');})
		e.trigger(trigger + '_and_run');
	}
});



//Ended: /${applicationid}/components/xml/detaileditor.js Size: 1.39 KB


/** 
 EnterMediaDB javascriptGenerator : 404 NOT FOUND/${applicationid}/components/javascript/emedia/ajax/runajax.js
/finder/find/_site.xconf
  **/





/** 
 EnterMediaDB javascriptGenerator : 404 NOT FOUND/${applicationid}/components/javascript/emedia/ajax/emdialog.js
/finder/find/_site.xconf
  **/





/** 
 EnterMediaDB javascriptGenerator : 404 NOT FOUND/${applicationid}/components/javascript/emedia/dataentryfields.js
/finder/find/_site.xconf
  **/





/** 
 EnterMediaDB javascriptGenerator : 404 NOT FOUND/${applicationid}/components/javascript/emedia/dataentrypickers.js
/finder/find/_site.xconf
  **/





/** 
 EnterMediaDB javascriptGenerator : 404 NOT FOUND/${applicationid}/components/javascript/emedia/utility-plugins.js
/finder/find/_site.xconf
  **/





/** 
 EnterMediaDB javascriptGenerator : /${applicationid}/components/javascript/emedia/util.js
/finder/find/_site.xconf
 Modified: Tue May 05 23:52:52 CST 2026 Size: 16.21 KB **/

var app, siteroot, apphome;
var externalmessage;

function getRandomColor() {
	var letters = "0123456789ABCDEF".split("");
	var color = "#";
	for (var i = 0; i < 6; i++) {
		color += letters[Math.floor(Math.random() * 16)];
	}
	return color;
}

function lightenHex(hex, lighten = 0) {
	var result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);

	var r = parseInt(result[1], 16);
	var g = parseInt(result[2], 16);
	var b = parseInt(result[3], 16);

	r /= 255;
	g /= 255;
	b /= 255;
	var max = Math.max(r, g, b),
		min = Math.min(r, g, b);
	var h,
		s,
		l = (max + min) / 2;

	if (max == min) {
		h = s = 0; // achromatic
	} else {
		var d = max - min;
		s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
		switch (max) {
			case r:
				h = (g - b) / d + (g < b ? 6 : 0);
				break;
			case g:
				h = (b - r) / d + 2;
				break;
			case b:
				h = (r - g) / d + 4;
				break;
		}
		h /= 6;
	}

	s *= 100;
	s = Math.round(s);
	l *= 100;
	if (l + lighten > 100 || l + lighten < 0) {
		l -= lighten;
	} else {
		l += lighten;
	}
	l = Math.round(l);
	h = Math.round(360 * h);

	l /= 100;
	var a = (s * Math.min(l, 1 - l)) / 100;
	var f = (n) => {
		var k = (n + h / 30) % 12;
		var color = l - a * Math.max(Math.min(k - 3, 9 - k, 1), -1);
		return Math.round(255 * color)
			.toString(16)
			.padStart(2, "0");
	};
	return `#${f(0)}${f(8)}${f(4)}`;
}
function contrastColor(hex) {
	if (hex.indexOf("#") === 0) {
		hex = hex.slice(1);
	}

	if (hex.length === 3) {
		hex = hex[0] + hex[0] + hex[1] + hex[1] + hex[2] + hex[2];
	}
	if (hex.length !== 6) {
		throw new Error("Invalid HEX color.");
	}
	var r = parseInt(hex.slice(0, 2), 16),
		g = parseInt(hex.slice(2, 4), 16),
		b = parseInt(hex.slice(4, 6), 16);

	return r * 0.299 + g * 0.587 + b * 0.114 > 186 ? "#444444" : "#FFFFFF";
}

toggleUserProperty = function (property, onsuccess = null, onfailure = null) {
	app = $("#application");
	siteroot = app.data("siteroot");
	apphome = app.data("apphome");
	console.log("Saving: " + property);
	jQuery.ajax({
		url:
			apphome +
			"/components/userprofile/toggleprofileproperty.html?field=" +
			property,
		success: function () {
			if (onsuccess) onsuccess();
		},
		error: function () {
			if (onfailure) onfailure();
		},
	});
};

saveProfileProperty = function (property, value, onsuccess = null) {
	app = $("#application");
	siteroot = app.data("siteroot");
	apphome = app.data("apphome");

	var data = app.cleandata();
	data.oemaxlevel = 1;
	data.propertyfield = property;
	data["property.value"] = value;

	jQuery.ajax({
		url: apphome + "/components/userprofile/saveprofileproperty.html",
		data: data,
		success: function () {
			if (onsuccess) onsuccess();
		},
		xhrFields: {
			withCredentials: true,
		},
		crossDomain: true,
	});
};

setSessionValue = function (key, value) {
	app = $("#application");
	siteroot = app.data("siteroot");
	apphome = app.data("apphome");

	jQuery.ajax({
		url:
			apphome +
			"/components/session/setvalue.html?key=" +
			key +
			"&value=" +
			value,
	});
};

getSessionValue = function (key) {
	var returnval = null;
	app = $("#application");
	siteroot = app.data("siteroot");
	apphome = app.data("apphome");

	jQuery.ajax({
		url: apphome + "/components/session/getvalue.html?key=" + key,
		async: false,
		success: function (data) {
			returnval = data;
		},
	});

	return returnval;
};

var desktopOffset = 0;

$(document).ready(function () {
	app = $("#application");
	if (app.data("desktop")) {
		desktopOffset = 32;
	}
	function setMaxHeight(elm, child, offset = 32) {
		if (!elm || !elm.length) {
			return;
		}
		var target = elm;
		if (child) {
			target = elm.find(child);
			if (!target || !target.length) {
				return;
			}
		}
		var top = $(window).height() - elm.offset().top - offset - desktopOffset;
		top = Math.max(top, 400);
		target.css("height", top + "px");
	}

	function resizeColumns() {
		var windowh = $(window).height() - desktopOffset;

		//togglers always screen height
		var coltogglers = $(".col-sidebar-togglers");
		coltogglers.css("height", windowh - 4);
		var colsidebar = $(".col-mainsidebar");
		colsidebar.css("height", windowh);

		//reset some heights
		$(".settingslayout").css("height", "auto");
		$(".col-content-main").css("height", "auto"); //reset

		$(".adjustHeight").each(function () {
			setMaxHeight($(this));
		});
	}

	function resizeSearchCategories() {
		var container = $("#sidecategoryresults");
		if (!container) {
			return;
		}
		var w = container.width();

		var ctree = container.find(".searchcategories-tree");
		var cfilter = container.find(".searchcategories-filter");
		if (w > 640) {
			ctree.addClass("widesidebar");
			cfilter.addClass("widesidebar");
		} else {
			ctree.removeClass("widesidebar");
			cfilter.removeClass("widesidebar");
		}
		//console.log(h);
	}

	function adjustDataManagerTable() {
		if ($(".datamanagertable").length) {
			var height = $(window).height() - desktopOffset;
			$(".datamanagertable").height(height - 352);
		}
	}

	var resizeTimer = null; //Prevent back to back resize events, only run the last trigger
	jQuery(window).on("resize", function () {
		resizeTimer && clearTimeout(resizeTimer);
		resizeTimer = setTimeout(function () {
			adjustDataManagerTable();
			resizeSearchCategories();
			resizeColumns();
		}, 50);
	});

	window.debugMode = false;
	window.onkeydown = function (event) {
		if (event.ctrlKey) {
			var selector = document.querySelector("a#oeselector");
			if (selector == undefined) {
				return;
			}
			var href = selector.href;
			if (event.key == "r") {
				event.preventDefault();
				href = href.replace(
					"components/toolbar/plugintoolbar",
					"views/filemanager/clearpagemanager",
				);
				window.location.href = href;
			} else if (event.key == "d") {
				event.preventDefault();
				if (!debugMode) {
					debugMode = !document.querySelector(".openeditdebug");
				} else {
					debugMode = false;
				}
				var mode = debugMode ? "debug" : "preview";
				href = href.replace(
					"components/toolbar/plugintoolbar",
					`views/workflow/mode/view${mode}`,
				);
				jQuery.get(href);
				customToast("Switched to&nbsp;<b>" + mode + "</b>&nbsp;mode!", {
					positive: !debugMode,
					icon: debugMode ? "bug-fill" : "eye-fill",
				});
				if (mode == "preview") {
					window.location.reload();
				}
			}
		} else {
			const targetTagName = event.target.tagName;
			if (targetTagName === "INPUT" || targetTagName === "TEXTAREA") {
				if ($(event.target).val() !== "") {
					return;
				}
			}

			if (event.which == "37" || event.which == "39") //left | arrow arrow key
			{
				var container;
				var link;
				if ($(".entitydialog").length > 0) {
					container = $(".entitydialog");
				} else if ($("#main-media-container").length > 0) {
					container = $("#main-media-container");
				}
				if (event.which == "37") {
					link = $(".goleftclick", container);
				} else {
					link = $(".gorightclick", container);
				}
				if (link) {
					link.trigger("click");
				}
			}
		}
	};

	setTimeout(function () {
		var path = new URL(window.location.href).pathname;
		$(".auto-active-link").each(function () {
			var href = $(this).attr("href");
			if (href == path) {
				var container = $(this).closest(".auto-active-container");
				container.find("li.current").removeClass("current");
				container.find("a.active").removeClass("active");
				$(this).addClass("active");
				$(this).parents("li").addClass("current");
			}
		});
	});

	lQuery("img.hasFallback").livequery("error", function () {
		var err = $(this).data("error");
		$(this).replaceWith(`<div class="img-error">${err}</div>`);
	});

	lQuery(".favorite-star").livequery("click", function (e) {
		e.preventDefault();
		e.stopImmediatePropagation();
		$(this).runAjax();
	});

	lQuery(".pickfromiframe").livequery("click", function (e) {
		if ($("#application").hasClass("blockfind")) {
			e.preventDefault();
			e.stopImmediatePropagation();

			var pickerBtn = $(this);

			var imageUrl = pickerBtn.data("imageurl");
			var assetInfo = pickerBtn.closest("[data-assetid]");
			var assetid = "";
			var target = $("#application").data("targetfieldid");

			if (!target || target.trim() == "") {
				if (externalmessage && externalmessage.target) {
					target = externalmessage.target;
				}
			}
			if (assetInfo.length) {
				assetid = assetInfo.data("assetid");
			}

			var type = $("#application").data("targettype");
			if (!type) {
				type = "asset";
			}

			if (type == "entity") {
				assetid = pickerBtn.data("primarymedia");
			}

			var entityid = pickerBtn.data("entityid");

			var payload = {
				name: "eMediaAssetPicked",
				assetpicked: imageUrl,
				assetid: assetid,
				entityid: entityid,
				target: target,
				type: type,
			};

			if (top && externalmessage) {
				var parenturl = externalmessage["parenturl"];
				if (parenturl !== null) {
					var parentProtocol = new URL(parenturl).protocol;
					var hostname = new URL(parenturl).hostname;
					var parentPort = new URL(parenturl).port;
					var targetOrigin = `${parentProtocol}//${hostname}`;
					if (parentPort) targetOrigin += `:${parentPort}`;

					// Use the target origin in the postMessage.
					top.postMessage(payload, targetOrigin);
				}
			} else {
				window.parent.postMessage(payload);
			}
		}
	});

	focusInput = function (input) {
		if (window.innerWidth < 768) return false;
		if (input.hasClass("datepicker")) {
			return;
		}
		if (input.length > 0) {
			input.trigger("focus");
			var inputVal = input.val();
			if (inputVal) {
				input.val("");
				input.val(inputVal);
			}
			return true;
		}
		return false;
	};

	lQuery("form").livequery(function () {
		var modal = $(this).closest(".modal");
		if (modal.length === 0 || $(this).hasClass("noautofocus")) {
			return;
		}
		var input = $(this).find("input[autofocus]:visible:first");
		if (input.length === 0) {
			input = $(this).find("textarea[autofocus]:visible:first");
		}

		var focused = focusInput(input);
		if (!focused) {
			var $this = $(this);
			setTimeout(function () {
				focusInput($this.find("input:visible:first"));
			});
		}
	});
	lQuery("textarea#postcontent").livequery(function () {
		var text = $(this).val();
		var $text = $(text);
		var finalText = "";
		let counter = 0;
		function getText(node, parents = []) {
			var nodeName = node.nodeName;

			if (nodeName == "OL") {
				counter = 0;
			}

			if (nodeName == "LI") {
				if (parents.length >= 1 && parents[parents.length - 1] == "OL") {
					counter++;
					finalText += counter + ". ";
				} else {
					finalText += "  •  ";
				}
			}

			if (nodeName == "BR") {
				finalText += "\n";
			} else if (nodeName == "#text") {
				var textContent = node.textContent;
				if (parents.length > 0) {
					var parent = parents[parents.length - 1];
					var grandParents = parents.slice(0, parents.length - 1);
					if (parent == "B" || parent == "STRONG" || /H\d/.test(parent)) {
						if (grandParents.includes("I") || grandParents.includes("EM")) {
							textContent = asciiBoldItalicText(textContent);
						} else {
							textContent = asciiBoldText(textContent);
						}
					} else if (parent == "I" || parent == "EM") {
						if (grandParents.includes("B") || grandParents.includes("STRONG")) {
							textContent = asciiBoldItalicText(textContent);
						} else {
							textContent = asciiItalicText(textContent);
						}
					} else if (parent == "U" || parent == "INS") {
						if (grandParents.includes("B") || grandParents.includes("STRONG")) {
							textContent = asciiBoldUnderlineText(textContent);
						} else if (
							grandParents.includes("I") ||
							grandParents.includes("EM")
						) {
							textContent = asciiBoldUnderlineText(textContent);
						} else {
							textContent = asciiUnderlineText(textContent);
						}
					}
				}
				finalText += textContent;
			} else {
				var childNodes = node.childNodes;
				for (var i = 0; i < childNodes.length; i++) {
					getText(childNodes[i], parents.concat(node.nodeName));
				}
			}
			if (nodeName == "P") {
				finalText += "\n\n";
			}
			if (
				/H\d/.test(nodeName) ||
				nodeName == "LI" ||
				nodeName == "UL" ||
				nodeName == "OL"
			) {
				finalText += "\n";
			}
		}

		$text.each(function () {
			getText(this);
		});

		finalText = finalText.trim();
		$(this).text(finalText);
		$(this).val(finalText);
	});

	lQuery(".postiz-format").livequery("click", function () {
		var textarea = document.querySelector("textarea#postcontent");
		var format = $(this).data("format");
		var selectionStart = textarea.selectionStart;
		var selectionEnd = textarea.selectionEnd;
		var selection = "";
		if (selectionStart >= 0 && selectionEnd >= 1) {
			if (selectionStart == selectionEnd) {
				selection = prompt("Enter the text you want to insert");
			} else {
				selection = textarea.value.substring(selectionStart, selectionEnd);
			}
		}
		if (selection.length) {
			selection = asciiNormalText(selection);
			if (format == "bold") {
				selection = asciiBoldText(selection);
			} else if (format == "italic") {
				selection = asciiItalicText(selection);
			} else if (format == "underline") {
				selection = asciiUnderlineText(selection);
			}
		}
		if (selectionStart != selectionEnd) {
			textarea.value =
				textarea.value.substring(0, selectionStart) +
				selection +
				textarea.value.substring(selectionEnd);
		} else {
			textarea.value = textarea.value + selection;
			textarea.scroll(0, textarea.scrollHeight);
		}
		$(textarea).trigger("focus");
	});
}); //document ready

window.addEventListener(
	"message",
	function (e) {
		if (
			typeof e.data === "object" &&
			e.data.name === "setEmediaLibraryPicker"
		) {
			externalmessage = e.data;
		}
	},
	false,
);

var asciiFormatRanges = {
	n: [120812, 120821],
	bA: [120276, 120301],
	ba: [120302, 120327],
	iA: [120328, 120353],
	ia: [120354, 120379],
};

function getAsciiFormat(char) {
	for (var type in asciiFormatRanges) {
		var start = asciiFormatRanges[type][0];
		var end = asciiFormatRanges[type][1];
		if (char.codePointAt(0) >= start && char.codePointAt(0) <= end) {
			return type;
		}
	}
	return false;
}

function asciiNormalText(text) {
	Object.keys(asciiFormatRanges).forEach(function (type) {
		var start = asciiFormatRanges[type][0];
		var end = asciiFormatRanges[type][1];
		var diff = 48;
		if (type == "bA" || type == "iA") diff = 65;
		else if (type == "ba" || type == "ia") diff = 97;
		for (var i = start; i <= end; i++) {
			text = text.replaceAll(String.fromCodePoint(i), function (char) {
				return String.fromCharCode(char.codePointAt(0) - start + diff);
			});
		}
		text = text.replaceAll(String.fromCodePoint(818), "");
	});
	return text;
}

function asciiBoldText(text) {
	return text.replace(/[A-Za-z0-9]/g, function (char) {
		let diff;
		if (/[0-9]/.test(char)) {
			diff = "𝟬".codePointAt(0) - "0".codePointAt(0);
		} else if (/[A-Z]/.test(char)) {
			diff = "𝗔".codePointAt(0) - "A".codePointAt(0);
		} else {
			diff = "𝗮".codePointAt(0) - "a".codePointAt(0);
		}
		return String.fromCodePoint(char.codePointAt(0) + diff);
	});
}
function asciiItalicText(text) {
	return text.replace(/[A-Za-z]/g, function (char) {
		let diff;
		if (/[A-Z]/.test(char)) {
			diff = "𝘈".codePointAt(0) - "A".codePointAt(0);
		} else {
			diff = "𝘢".codePointAt(0) - "a".codePointAt(0);
		}
		return String.fromCodePoint(char.codePointAt(0) + diff);
	});
}
function asciiBoldItalicText(text) {
	return text.replace(/[A-Za-z]/g, function (char) {
		let diff;
		if (/[A-Z]/.test(char)) {
			diff = "𝘼".codePointAt(0) - "A".codePointAt(0);
		} else {
			diff = "𝙖".codePointAt(0) - "a".codePointAt(0);
		}
		return String.fromCodePoint(char.codePointAt(0) + diff);
	});
}

function asciiUnderlineText(text) {
	return text.replace(/[A-Za-z0-9]/g, function (char) {
		return String.fromCodePoint(char.codePointAt(0), 818);
	});
}
function asciiBoldUnderlineText(text) {
	return text.replace(/[A-Za-z]/g, function (char) {
		let diff;
		if (/[0-9]/.test(char)) {
			diff = "𝟬".codePointAt(0) - "0".codePointAt(0);
		} else if (/[A-Z]/.test(char)) {
			diff = "𝗔".codePointAt(0) - "A".codePointAt(0);
		} else {
			diff = "𝗮".codePointAt(0) - "a".codePointAt(0);
		}
		return String.fromCodePoint(char.codePointAt(0) + diff, 818);
	});
}

function asciiItalicUnderlineText(text) {
	return text.replace(/[A-Za-z]/g, function (char) {
		let diff;
		if (/[A-Z]/.test(char)) {
			diff = "𝘈".codePointAt(0) - "A".codePointAt(0);
		} else {
			diff = "𝘢".codePointAt(0) - "a".codePointAt(0);
		}
		return String.fromCodePoint(char.codePointAt(0) + diff, 818);
	});
}




//Ended: /${applicationid}/components/javascript/emedia/util.js Size: 16.21 KB


/** 
 EnterMediaDB javascriptGenerator : 404 NOT FOUND/${applicationid}/components/javascript/emedia/toast.js
/finder/find/_site.xconf
  **/





/** 
 EnterMediaDB javascriptGenerator : /${applicationid}/components/javascript/emedia/image-editor.js
/finder/find/_site.xconf
 Modified: Tue May 05 23:52:52 CST 2026 Size: 25.63 KB **/

function isWebGLEnabled() {
	try {
		var canvas = document.createElement("canvas");
		return (
			!!window.WebGLRenderingContext &&
			(canvas.getContext("webgl") || canvas.getContext("experimental-webgl"))
		);
	} catch (e) {
		return false;
	}
}

var fabricFilters = [
	"Brightness",
	"Contrast",
	"Saturation",
	"HueRotation",
	"Vibrance",
	"Blur",
	"Noise",
	"Pixelate",
	"Sharpen",
	"Grayscale",
	"BlackWhite",
	"Sepia",
	"Invert",
	"Vintage",
	"Technicolor",
	"Polaroid",
	"Kodachrome",
	"Sharpen",
	"Emboss",
	"Edge",
];
var convolutionMatrices = {
	Emboss: [-2, -1, 0, -1, 1, 1, 0, 1, 2],
	Sharpen: [0, -1, 0, -1, 5, -1, 0, -1, 0],
	Edge: [1, 1, 1, 1, -7, 1, 1, 1, 1],
};

$(document).ready(function () {
	function initializeEditor() {
		$(".photo-editor-container").css("width", window.innerWidth - 260);
		var imgSrc = $(this).attr("src");
		if (!imgSrc) return;
		var editorWidth = window.innerWidth - (260 + 72);
		var editorHeight = window.innerHeight - 100;

		fabric.Object.prototype.transparentCorners = false;

		fabric.textureSize = 4096;
		var canvas = new fabric.Canvas("canvas");
		canvas.setWidth(editorWidth);
		canvas.setHeight(editorHeight);
		canvas.preserveObjectStacking = true;
		canvas.selection = false;

		canvas.on("mouse:wheel", function (opt) {
			if (!opt.e.ctrlKey) return;
			var delta = opt.e.deltaY;
			var zoom = canvas.getZoom();
			zoom *= 0.999 ** delta;
			if (zoom > 3) zoom = 3;
			if (zoom < 0.1) zoom = 0.1;
			canvas.zoomToPoint({ x: opt.e.offsetX, y: opt.e.offsetY }, zoom);
			opt.e.preventDefault();
			opt.e.stopPropagation();
		});

		$(".zoom-pan button").click(function () {
			var action = $(this).data("action");
			if (action === "tips") {
				$(this).popover({
					container: "body",
					html: true,
				});
				$(this).popover("show");
			} else if (action === "reset") {
				centerViewPort();
			} else {
				var zoom = canvas.getZoom();
				if (action === "zoomIn") {
					zoom *= 1.1;
				}
				if (action === "zoomOut") {
					zoom /= 1.1;
				}
				if (zoom > 3) zoom = 3;
				if (zoom < 0.1) zoom = 0.1;
				canvas.setZoom(zoom);
			}
		});

		document.addEventListener("keydown", function (e) {
			if (e.code === "Numpad0" && (e.ctrlKey || e.metaKey)) {
				centerViewPort();
			}
		});

		function centerViewPort() {
			canvas.setViewportTransform([
				1,
				0,
				0,
				1,
				-imageRenderLeft + canvas.width / 2 - imageRenderWidth / 2,
				-imageRenderTop + canvas.height / 2 - imageRenderHeight / 2,
			]);
		}

		canvas.on("mouse:down", function (opt) {
			var evt = opt.e;
			var activeObj = canvas.getActiveObject();
			if (!activeObj) {
				this.isDragging = true;
				this.selection = false;
				this.lastPosX = evt.clientX;
				this.lastPosY = evt.clientY;
			}
		});
		canvas.on("mouse:move", function (opt) {
			if (this.isDragging) {
				var e = opt.e;
				var vpt = this.viewportTransform;
				vpt[4] += e.clientX - this.lastPosX;
				vpt[5] += e.clientY - this.lastPosY;
				this.renderAll();
				this.lastPosX = e.clientX;
				this.lastPosY = e.clientY;
			}
		});
		canvas.on("mouse:up", function (opt) {
			this.setViewportTransform(this.viewportTransform);
			this.isDragging = false;
			this.selection = true;
		});

		var deleteIcon =
			"data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxNiIgaGVpZ2h0PSIxNiIgZmlsbD0iI2ZmM2M0MSIgdmlld0JveD0iMCAwIDE2IDE2Ij4KICA8cGF0aCBkPSJNNS41IDUuNUEuNS41IDAgMCAxIDYgNnY2YS41LjUgMCAwIDEtMSAwVjZhLjUuNSAwIDAgMSAuNS0uNW0yLjUgMGEuNS41IDAgMCAxIC41LjV2NmEuNS41IDAgMCAxLTEgMFY2YS41LjUgMCAwIDEgLjUtLjVtMyAuNWEuNS41IDAgMCAwLTEgMHY2YS41LjUgMCAwIDAgMSAweiIvPgogIDxwYXRoIGQ9Ik0xNC41IDNhMSAxIDAgMCAxLTEgMUgxM3Y5YTIgMiAwIDAgMS0yIDJINWEyIDIgMCAwIDEtMi0yVjRoLS41YTEgMSAwIDAgMS0xLTFWMmExIDEgMCAwIDEgMS0xSDZhMSAxIDAgMCAxIDEtMWgyYTEgMSAwIDAgMSAxIDFoMy41YTEgMSAwIDAgMSAxIDF6TTQuMTE4IDQgNCA0LjA1OVYxM2ExIDEgMCAwIDAgMSAxaDZhMSAxIDAgMCAwIDEtMVY0LjA1OUwxMS44ODIgNHpNMi41IDNoMTFWMmgtMTF6Ii8+Cjwvc3ZnPg==";
		var copyIcon =
			"data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxNiIgaGVpZ2h0PSIxNiIgZmlsbD0iI2IyY2NmZiIgdmlld0JveD0iMCAwIDE2IDE2Ij4KICA8cGF0aCBmaWxsLXJ1bGU9ImV2ZW5vZGQiIGQ9Ik00IDJhMiAyIDAgMCAxIDItMmg4YTIgMiAwIDAgMSAyIDJ2OGEyIDIgMCAwIDEtMiAySDZhMiAyIDAgMCAxLTItMnptMi0xYTEgMSAwIDAgMC0xIDF2OGExIDEgMCAwIDAgMSAxaDhhMSAxIDAgMCAwIDEtMVYyYTEgMSAwIDAgMC0xLTF6TTIgNWExIDEgMCAwIDAtMSAxdjhhMSAxIDAgMCAwIDEgMWg4YTEgMSAwIDAgMCAxLTF2LTFoMXYxYTIgMiAwIDAgMS0yIDJIMmEyIDIgMCAwIDEtMi0yVjZhMiAyIDAgMCAxIDItMmgxdjF6Ii8+Cjwvc3ZnPg==";

		var deleteImg = document.createElement("img");
		deleteImg.src = deleteIcon;

		var cloneImg = document.createElement("img");
		cloneImg.src = copyIcon;

		function renderIcon(icon) {
			return function renderIcon(ctx, left, top, _, fabricObject) {
				var size = this.cornerSize;
				ctx.save();
				ctx.translate(left, top);
				ctx.rotate(fabric.util.degreesToRadians(fabricObject.angle));
				ctx.drawImage(icon, -size / 2, -size / 2, size, size);
				ctx.restore();
			};
		}

		function getDeleteControl() {
			return new fabric.Control({
				x: 0.5,
				y: -0.5,
				offsetY: -32,
				offsetX: -4,
				cursorStyle: "pointer",
				mouseUpHandler: deleteObject,
				render: renderIcon(deleteImg),
				cornerSize: 24,
			});
		}
		function getCloneControl() {
			return new fabric.Control({
				x: -0.5,
				y: -0.5,
				offsetY: -32,
				offsetX: 4,
				cursorStyle: "pointer",
				mouseUpHandler: cloneObject,
				render: renderIcon(cloneImg),
				cornerSize: 24,
			});
		}
		var controls = fabric.controlsUtils.createObjectDefaultControls();
		delete controls.mt;
		delete controls.ml;
		delete controls.mr;
		delete controls.mb;
		fabric.InteractiveFabricObject.ownDefaults.controls = {
			...controls,
			deleteControl: getDeleteControl(),
			clone: getCloneControl(),
		};
		// fabric.Textbox.prototype.controls.deleteControl = getDeleteControl();
		// fabric.Textbox.prototype.controls.clone = getCloneControl();
		fabric.Object.prototype.cornerStyle = "circle";
		fabric.Text.prototype.cornerStyle = "circle";

		function deleteObject(_, transform) {
			var target = transform.target;
			var canvas = target.canvas;
			canvas.remove(target);
			canvas.renderAll();
		}

		function cloneObject(_, transform) {
			var target = transform.target;
			var canvas = target.canvas;
			target.clone(function (cloned) {
				cloned.left += 10;
				cloned.top += 10;
				canvas.add(cloned);
			});
		}

		var primaryImg, imgInstance, selectionRect, debugRect;
		var imgLoading = true;

		function loadPrimaryImage(src) {
			imgLoading = true;
			$("#preDefFilters a").removeClass("show");
			primaryImg = new Image();
			primaryImg.onload = function () {
				imgLoading = false;
				var hRatio = (editorWidth - 16) / primaryImg.width;
				var vRatio = (editorHeight - 16) / primaryImg.height;
				var ratio = Math.min(hRatio, vRatio);
				if (ratio > 1) ratio = 1;

				var renderWidth = Math.floor(primaryImg.width * ratio);
				var renderHeight = Math.round(primaryImg.height * ratio);

				window.imageRenderWidth = renderWidth;
				window.__imageRenderWidth = renderWidth;
				window.imageRenderHeight = renderHeight;
				window.__imageRenderHeight = renderHeight;
				window.imageRenderLeft = 0;
				window.__imageRenderLeft = 0;
				window.imageRenderTop = 0;
				window.__imageRenderTop = 0;
				window.imageRenderAngle = 0;

				imgInstance = new fabric.Image(primaryImg, {
					left: 0,
					top: 0,
					selectable: false,
					evented: false,
				});
				imgInstance.scaleToWidth(renderWidth);
				imgInstance.scaleToHeight(renderHeight);

				canvas.add(imgInstance);
				canvas.setViewportTransform([1, 0, 0, 1, 8, 8]);
				window.imageRenderWidth = imgInstance.getScaledWidth();
				window.imageRenderHeight = imgInstance.getScaledHeight();

				$("#editCandidateLoader").hide();

				centerViewPort();

				imgInstance.filters = [];
				fabricFilters.forEach(function (_, i) {
					imgInstance.filters.push(false);
				});

				canvas.renderAll();
			};

			primaryImg.src = src;
		}

		loadPrimaryImage(imgSrc);

		function generateFilterPreview() {
			$("#preDefFilters a").each(function () {
				if ($(this).is(":visible")) return;
				var filter = $(this).data("action");
				var fpCanvas = new fabric.StaticCanvas("fpCanvas");
				fpCanvas.width = 100;
				fpCanvas.height = 100;
				var fpFilter = new fabric.filters[filter]();
				var fpImgInstance = new fabric.Image(primaryImg, { left: 0, top: 0 });
				var fpRatio = primaryImg.width / primaryImg.height;
				var fpW, fpH;
				if (fpRatio > 1) {
					fpH = 100;
					fpW = 100 * fpRatio;
				} else {
					fpW = 100;
					fpH = 100 / fpRatio;
				}
				fpImgInstance.scaleToWidth(fpW);
				fpImgInstance.scaleToHeight(fpH);
				fpImgInstance.filters.push(fpFilter);
				fpImgInstance.applyFilters();
				fpCanvas.add(fpImgInstance);
				fpCanvas.renderAll();
				$(this).find("img").attr("src", fpCanvas.toDataURL());
				fpCanvas.dispose();
				$(this).addClass("show");
			});
		}

		$(".editorarea a").click(function (e) {
			e.preventDefault();
			if (imgLoading) return;
			$(".panel").each(function () {
				$(this).removeClass("active");
			});
			var action = $(this).data("action");
			var panel = "." + action + "-editor";
			$(panel).css("top", $(this).offset().top);
			$(panel).toggleClass("active");
			if (["flipX", "flipY", "rotateLeft", "rotateRight"].includes(action)) {
				destroySelectionRect();
			} else {
				destroySelectionRect();
				canvas.discardActiveObject();
				canvas.renderAll();
			}
			if (action === "filter") {
				setTimeout(function () {
					generateFilterPreview();
				}, 250);
			}
		});

		$(".x-close").click(function () {
			$(this).parent().removeClass("active");
			destroySelectionRect();
			canvas.discardActiveObject();
			canvas.renderAll();
		});

		$("#preDefFilters a").click(function (e) {
			e.preventDefault();
			var filter = $(this).data("action");
			var filterIdx = fabricFilters.indexOf(filter);
			var isActive = $(this).hasClass("active");
			$(this).toggleClass("active");
			if (!isActive) {
				var filterInstance = new fabric.filters[filter]();
				imgInstance.filters[filterIdx] = filterInstance;
			} else {
				imgInstance.filters[filterIdx] = false;
			}
			imgInstance.applyFilters();
			canvas.renderAll();
		});

		function createCropSelectionRect(freeTransform = false) {
			if (selectionRect) {
				destroySelectionRect();
			}
			selectionRect = new fabric.Rect({
				left: window.imageRenderLeft,
				top: window.imageRenderTop,
				width: window.imageRenderWidth,
				height: window.imageRenderHeight,
				fill: "rgba(255,255,255,0.35)",
				transparentCorners: false,
				stroke: "black",
				strokeDashArray: [2, 5],
				cornerColor: "white",
				cornerSize: 10,
				cornerStrokeColor: "black",
				cornerStyle: "circle",
				borderColor: "transparent",
			});
			selectionRect.setControlVisible("mtr", false);
			selectionRect.setControlVisible("deleteControl", false);
			selectionRect.setControlVisible("clone", false);
			if (freeTransform) {
				var controls = fabric.controlsUtils.createObjectDefaultControls();
				delete controls.mtr;
				selectionRect.controls = controls;
			}
			canvas.add(selectionRect);
			setTimeout(function () {
				canvas.setActiveObject(selectionRect);
				canvas.renderAll();
			});

			// selectionRect.scaleToWidth(renderWidth);
			// selectionRect.scaleToHeight(renderHeight);
		}

		function destroySelectionRect() {
			canvas.remove(selectionRect);
			canvas.discardActiveObject();
			canvas.renderAll();
			selectionRect = null;
		}

		function cdr(object) {
			if (debugRect) {
				canvas.remove(debugRect);
			}
			var bounds = object.getBoundingRect();
			debugRect = new fabric.Rect({
				...bounds,
				stroke: "blue",
				strokeWidth: 1,
				fill: "rgba(0,0,255,0.1)",
			});
			canvas.add(debugRect);
			canvas.renderAll();
		}

		$(".rotate-editor button").click(function () {
			var action = $(this).data("action");
			var activeObject = canvas.getActiveObject();
			if (!activeObject) {
				if (action.startsWith("rotate")) {
					alert("Please select an object to rotate");
					return;
				} else {
					activeObject = imgInstance;
				}
			}
			if (action === "flipX") {
				activeObject.set("flipX", !activeObject.flipX);
			}
			if (action === "flipY") {
				activeObject.set("flipY", !activeObject.flipY);
			}
			var angle = activeObject.angle % 360;
			if (action.startsWith("rotate")) {
				if (action === "rotateLeft") {
					angle -= 90;
				} else {
					angle += 90;
				}
				activeObject.rotate(angle);
				// activeObject.setCoords();
				// cdr(activeObject);
			}

			canvas.renderAll();
		});

		canvas.on("selection:created", onObjectSelected);
		canvas.on("selection:updated", onObjectSelected);
		function onObjectSelected(obj) {
			if (
				obj.selected.length > 0 &&
				obj.selected[0].get("type") === "textbox"
			) {
				$("#textField").val(obj.selected[0].text);
				$("#font-color").minicolors("value", obj.selected[0].fill);
				$("#font-family").val(obj.selected[0].fontFamily);
				$("#font-weight").val(
					JSON.stringify({
						weight: obj.selected[0].fontWeight,
						style: obj.selected[0].fontStyle,
					})
				);
				$("button.text-align-btn.active").removeClass("active");
				$(
					`button.text-align-btn[data-action=${obj.selected[0].textAlign}]`
				).addClass("active");
			} else {
				resetTextPanel();
			}
		}
		canvas.on("selection:cleared", resetTextPanel);

		function resetTextPanel() {
			$("#textField").val("");
		}

		$("#textField").keyup(function () {
			var activeObject = canvas.getActiveObject();
			if (activeObject && activeObject.get("type") == "textbox") {
				activeObject.set("text", $(this).val());
			} else {
				canvas.discardActiveObject();
				var styles = JSON.parse($("#font-weight").val());
				var text = new fabric.Textbox($(this).val(), {
					left: window.imageRenderWidth / 2 - 100,
					top: window.imageRenderHeight / 2 - 100,
					width: 250,
					fontSize: 54,
					fill: $("#font-color").minicolors("value"),
					textAlign: "center",
					fontFamily: $("#font-family").val(),
					fontWeight: styles.weight,
					fontStyle: styles.style,
				});
				canvas.add(text);
				canvas.setActiveObject(text);
			}
			canvas.renderAll();
		});
		$("#font-color").minicolors({
			change: function (hex) {
				var activeObject = canvas.getActiveObject();
				if (activeObject && activeObject.get("type") == "textbox") {
					activeObject.set("fill", hex);
					canvas.renderAll();
				}
			},
		});

		$("button.text-align-btn").click(function () {
			var activeObject = canvas.getActiveObject();
			if (activeObject && activeObject.get("type") == "textbox") {
				activeObject.set("textAlign", $(this).data("action"));
				canvas.renderAll();
			}
			$(this).siblings().removeClass("active");
			$(this).addClass("active");
		});

		$("#font-family").change(function () {
			updateFont();
		});
		$("#font-weight").change(function () {
			updateFont();
		});

		function updateFont() {
			var activeObject = canvas.getActiveObject();
			if (activeObject && activeObject.get("type") === "textbox") {
				var font = $("#font-family").val();
				var style = JSON.parse($("#font-weight").val());
				activeObject.set("fontFamily", font);
				activeObject.set("fontWeight", style.weight);
				activeObject.set("fontStyle", style.style);
				canvas.renderAll();
			}
		}

		$(".fltr input[type=range]").on("input", function () {
			var valInput = $(this).next("input");
			var dataType = valInput.data("type");
			var filterValue = $(this).val();
			var valInputValue = filterValue;
			if (dataType === "percentage") {
				valInputValue = Math.round(valInputValue * 100);
			} else if (dataType === "degree") {
				valInputValue = Math.round(valInputValue * 180);
			}
			valInput.val(valInputValue);
			var primaryParent = $(this).parent().parent();
			var id = primaryParent.attr("id");
			if (!id) id = primaryParent.data("id");
			var filterIdx = fabricFilters.indexOf(id);

			var prop = primaryParent.data("prop");
			if (!prop) prop = id.toLowerCase();
			var filterInstance = imgInstance.filters.find((f) => f.type === id);
			if (!filterInstance) {
				filterInstance = new fabric.filters[id]();
				imgInstance.filters[filterIdx] = filterInstance;
			}
			filterInstance[prop] = filterValue;
			imgInstance.applyFilters();
			canvas.renderAll();
		});

		$(".matrixFilter input[type=checkbox]").click(function () {
			var action = $(this).data("action");
			var filterIdx = fabricFilters.indexOf(action);
			var checked = $(this).prop("checked");
			var matrix = convolutionMatrices[action];
			if (checked) {
				var filterInstance = new fabric.filters["Convolute"]({
					matrix: matrix,
				});
				imgInstance.filters[filterIdx] = filterInstance;
			} else {
				imgInstance.filters[filterIdx] = false;
			}
			imgInstance.applyFilters();
			canvas.renderAll();
		});

		$(".fltr a").click(function () {
			var id = $(this).data("id");
			var filterIdx = fabricFilters.indexOf(id);
			imgInstance.filters[filterIdx] = false;
			imgInstance.applyFilters();
			canvas.renderAll();
			var rangeInp = $("#" + id).find("input[type=range]");
			var defaultVal = rangeInp.attr("defaultValue");
			rangeInp.val(defaultVal);
			$("#" + id)
				.find("input[type=text]")
				.val(0);
		});

		$("#imageField").change(function () {
			var fileReader = new FileReader();
			fileReader.onload = function (e) {
				var img = new Image();
				img.onload = function () {
					var hRatio = (editorWidth - 100) / img.width;
					var vRatio = (editorHeight - 100) / img.height;
					var ratio = Math.min(hRatio, vRatio);
					if (ratio > 1) ratio = 1;
					$("#imagePreview").attr("src", img.src).show();
					$("#imagePreview").data("ratio", ratio);
					$("#insertImg").parent().addClass("d-flex").show();
				};
				img.src = e.target.result;
			};
			fileReader.readAsDataURL(this.files[0]);
		});

		$("#insertImg").click(function () {
			var selectedImage = document.getElementById("imagePreview");
			var ratio = $("#imagePreview").data("ratio");
			var newImg = new fabric.Image(selectedImage, {
				left: 100,
				top: 100,
				scaleX: ratio,
				scaleY: ratio,
			});
			canvas.add(newImg);
			canvas.setActiveObject(newImg);
			canvas.renderAll();
			$("#imageField").val("");
			$("#imagePreview").hide();
			$(this).parent().removeClass("d-flex").hide();
			$(this).parent().parent().removeClass("active");
		});

		function getFinalImage(format = "png", obj = null) {
			var imgDim = obj
				? obj.getBoundingRect()
				: {
						left: window.imageRenderLeft,
						top: window.imageRenderTop,
						width: window.imageRenderWidth,
						height: window.imageRenderHeight,
				  };
			// if (imgInstance.clipPath) {
			// 	imgDim = imgInstance.clipPath.getBoundingRect();
			// }
			var data = canvas.toDataURL({
				format: format,
				...imgDim,
			});
			return data;
		}

		$("#imagesave").click(function (e) {
			e.preventDefault();
			var form = $("#saveform");
			var formdata = new FormData(form[0]);
			formdata.append("oemaxlevel", 1);

			var assetfileformat = $(form).data("assetfileformat").toLowerCase();
			if (assetfileformat === "jpg") assetfileformat = "jpeg";
			canvas.setViewportTransform([1, 0, 0, 1, 0, 0]);
			formdata.append("image", getFinalImage(assetfileformat));
			centerViewPort();

			$.ajax({
				url: form.attr("action"),
				data: formdata,
				type: "POST",
				contentType: false, // NEEDED, DON'T OMIT THIS (requires jQuery 1.6+)
				processData: false, // NEEDED, DON'T OMIT THIS
				//Refresh imageeditor
				success: function (data) {
					$("#photo-editor-container").html(data);
				},
			});
		});

		$("#saveAs").click(function () {
			var mask = $(this).siblings(".mask");
			var saveAs = $(this).siblings(".save-as-menu");
			mask.addClass("active");
			saveAs.addClass("active");
			var filenameInp = $("#saveasform input.newfilename");
			filenameInp.focus();
			var val = filenameInp.val();
			filenameInp.val("");
			filenameInp.val(val);
		});

		$("#saveAsImg").click(function () {
			var form = $("#saveasform");

			var formdata = new FormData(form[0]);

			var filenameInp = form.find("input.newfilename");
			var oldFilename = filenameInp.data("originalname");
			var newFilename = filenameInp.val();
			var fileExtInp = form.find("select.newfileext");
			var oldFileExt = fileExtInp.data("originalext");
			var newFileExt = fileExtInp.val();
			var fullNewName = newFilename + "." + newFileExt;
			var fullOldName = oldFilename + "." + oldFileExt;
			var existingNames = [fullOldName];
			var fNames = $("p.filename");
			fNames.each(function () {
				existingNames.push($(this).text());
			});
			if (existingNames.includes(fullNewName)) {
				var conf = confirm(
					"The file name is the same as an existing file. Do you want to overwrite?"
				);
				if (!conf) {
					return;
				}
			}
			var assetfileformat = $(form).data("assetfileformat");

			if (assetfileformat != "jpg" || assetfileformat != "png") {
				assetfileformat = "png";
			}

			canvas.setViewportTransform([1, 0, 0, 1, 0, 0]);
			formdata.append(
				"image",
				canvas.toDataURL({
					format: assetfileformat,
					left: window.imageRenderLeft,
					top: window.imageRenderTop,
					width: window.imageRenderWidth,
					height: window.imageRenderHeight,
				})
			);
			centerViewPort();
			formdata.append("oemaxlevel", 1);

			$.ajax({
				url: form.attr("action"),
				data: formdata,
				type: "POST",
				contentType: false, // NEEDED, DON'T OMIT THIS (requires jQuery 1.6+)
				processData: false, // NEEDED, DON'T OMIT THIS
				//Refresh imageeditor
				success: function (data) {
					$("#photo-editor-container").html(data);
				},
			});
		});

		$("#exportAs").click(function () {
			var mask = $(this).siblings(".mask");
			var exportAs = $(this).siblings(".export-menu");
			mask.addClass("active");
			exportAs.addClass("active");
		});

		function closeSaveAs() {
			$(".mask").removeClass("active");
			$(".save-as-menu").removeClass("active");
			$(".export-menu").removeClass("active");
		}
		$(".mask").click(closeSaveAs);
		$("#saveAsCancel").click(closeSaveAs);
		$("#exportAsCancel").click(closeSaveAs);

		$("#downloadImg").click(function () {
			canvas.renderAll();
			var a = document.createElement("a");
			var filename = $(this).data("filename");
			if (!filename) filename = "image";

			var ext = $("select#exportAsType").val();
			if (!ext) ext = "png";
			canvas.setViewportTransform([1, 0, 0, 1, 0, 0]);
			a.href = canvas.toDataURL({
				format: ext,
				left: window.imageRenderLeft,
				top: window.imageRenderTop,
				width: window.imageRenderWidth,
				height: window.imageRenderHeight,
			});
			centerViewPort();
			a.download = filename + "." + ext;
			a.click();
			a.remove();
			closeSaveAs();
		});

		$("input[name=replaceall]").click(function () {
			if ($(this).prop("checked")) {
				$("#customSizeOptions").hide();
			} else {
				$("#customSizeOptions").show();
			}
		});
		$("#copyImg").click(function () {
			if ("clipboard" in navigator) {
				var _this = $(this);
				_this.html('<i class="fas fa-spinner fa-spin"></i>');
				canvas.setViewportTransform([1, 0, 0, 1, 0, 0]);
				var dataURL = canvas.toDataURL({
					left: window.imageRenderLeft,
					top: window.imageRenderTop,
					width: window.imageRenderWidth,
					height: window.imageRenderHeight,
				});
				centerViewPort();
				fetch(dataURL)
					.then((res) => res.blob())
					.then((blob) => {
						navigator.clipboard.write([
							new ClipboardItem({
								"image/png": blob,
							}),
						]);
					})
					.then(() => {
						_this.html('<i class="bi bi-check-lg"></i>');
						setTimeout(() => {
							_this.html('<i class="bi bi-clipboard"></i>');
						}, 2000);
					})
					.catch(() => {
						_this.html(
							'<i class="bi bi-exclamation-triangle-fill text-warning"></i>'
						);
						setTimeout(() => {
							_this.html('<i class="bi bi-clipboard"></i>');
						}, 2000);
					});
			} else {
				alert("Clipboard API not supported");
				return;
			}
		});
	}
	lQuery("#editingCandidate").livequery(initializeEditor);

	var fonts = {
		Lato: ["Bold", "Italic", "Regular"],
		Arvo: ["Bold", "Italic", "Regular"],
		Caveat: ["Bold", "Regular"],
		Corinthia: ["Bold", "Regular"],
		DancingScript: ["Bold", "Regular"],
		KodeMono: ["Bold", "Regular"],
		MadimiOne: ["Regular"],
		MajorMonoDisplay: ["Regular"],
		Montserrat: ["Bold", "Italic", "Regular"],
		OpenSans: ["Bold", "Italic", "Regular"],
		Oswald: ["Bold", "Regular"],
		PixelifySans: ["Bold", "Regular"],
		Poppins: ["Bold", "Italic", "Regular"],
		PTSans: ["Bold", "Italic", "Regular"],
		PTSerif: ["Bold", "Italic", "Regular"],
		Roboto: ["Bold", "Italic", "Regular"],
		Wallpoet: ["Regular"],
	};

	lQuery("select#font-family").livequery(function () {
		var _this = $(this);
		var options = _this.find("option");
		if (options.length !== 0) return;

		var themeprefix = siteroot + $("#application").data("themeprefix");

		var promises = [];
		var fontInstances = [];
		Object.keys(fonts).forEach((font) => {
			_this.append('<option value="' + font + '">' + font + "</option>");
			var styles = fonts[font];
			styles.forEach((style) => {
				var url =
					"url(" + themeprefix + "/fonts/" + font + "-" + style + ".ttf)";
				var f = new FontFace(font, url, {
					style: style === "Italic" ? "italic" : "normal",
					weight: style === "Bold" ? "bold" : "normal",
				});
				fontInstances.push(f);
				promises.push(function () {
					return f.load();
				});
			});
		});
		Promise.all(promises).then(() => {
			fontInstances.forEach((ins) => {
				document.fonts.add(ins);
			});
		});
		_this.val("Roboto").change();
		_this.select2({
			minimumResultsForSearch: 10,
			templateResult: function (state) {
				if (!state.id) {
					return state.text;
				}
				var $state = $(
					`<span style="font-family:${state.id};font-size:1.25em">${state.text}</span>`
				);
				return $state;
			},
			templateSelection: function (state) {
				if (!state.id) {
					return state.text;
				}
				var $state = $(
					`<span style="font-family:${state.id};font-size:1.25em">${state.text}</span>`
				);
				return $state;
			},
			dropdownParent: _this.parent(),
		});
	});
});




//Ended: /${applicationid}/components/javascript/emedia/image-editor.js Size: 25.63 KB


/** 
 EnterMediaDB javascriptGenerator : /${applicationid}/components/chatterbox/chatterbox.js
/finder/find/_site.xconf
 Modified: Wed May 20 10:42:43 CST 2026 Size: 17.12 KB **/

jQuery(document).ready(function () {
	"use strict";

	let chatConnection;

	const app = $("#application");
	let appHome = app.data("apphome");
	const home = app.data("home");
	if (home !== undefined) {
		appHome = home + appHome;
	}
	const userid = app.data("user");

	function initChatterbox() {
		cancelKeepAlive();
		connect();
		keepAlive();

		lQuery(".chatter-send").livequery("click", function () {
			const button = $(this);
			const chatter = button.closest(".chatterbox");
			let data = chatter.data();

			data = $.extend({}, data); //So we can edit it
			data.command = button.data("command");

			const input = $("#chatter-msg");
			const replytoid = input.data("replytoid");
			if (replytoid) {
				data.replytoid = replytoid;
			}
			const message = input.val();
			data.message = message;

			const json = JSON.stringify(data);

			if (chatConnection.readyState === chatConnection.CLOSED) {
				connect();
				//IF we do a reconnect render the whole page
			}
			const toggle = button.data("toggle");
			if (toggle === true) {
				$(".chatter-toggle").toggle();
			}

			if ($("#chatter-msg").val() !== "") {
				chatConnection.send(json);

				//Clear editing area
				const area = $("#chatterbox-write");
				$("#chatter-msg", area).val("");
				$("#chatter-msg").data("replytoid", "");
				$(".chatterboxreplyto", area).hide();

				scrollToChat();

				const ses = $(".sessionhistory-item.active");
				if (ses.length > 0) {
					const span = ses.find(".item span");
					if (span.length > 0 && span.text() === "Current Session") {
						span.text(message.substring(0, 25));
					}
				}
			}
		});

		lQuery(".ai-suggest").livequery("click", function () {
			const button = $(this);
			const message = button.text();
			const input = $("#chatter-msg");
			input.val(message);
			input.trigger("focus");
			setTimeout(() => {
				$(".chatter-send").trigger("click");
				button.closest(".msg-bubble").remove();
			});
		});

		lQuery("#chatterboxreplycancel").livequery("click", function () {
			const button = $(this);
			button.closest(".chatterboxreplyto").hide();
		});

		lQuery(".chatter-text").livequery("keydown", function (e) {
			if (e.keyCode === 13 && !e.shiftKey) {
				//$("#chatter-msg").val("");
				e.preventDefault();
				const button = $('button[data-command="messagereceived"]');
				button.trigger("click");
				return false;
			} else {
				const scrollHeight = $(this).get(0).scrollHeight;
				if (
					!$(".chatterbox").hasClass("chatterlongtext") &&
					scrollHeight > 30
				) {
					$(".chatterbox").addClass("chatterlongtext");
					scrollToChat();
				}
			}
		});

		lQuery('button[data-command="messagereceived"]').livequery(
			"click",
			function (e) {
				//$("#chatter-msg").val("");
			},
		);

		lQuery(".chatter-save").livequery("click", function (e) {
			e.preventDefault();
			const button = $(this);
			const form = button.closest(".chatter-edit-form");
			const chatdiv = form.find(".chatter-msg-edit");
			const text = chatdiv.html();
			form.find(".chatter-msg-input").val(text);
			/*var button = $('submit');		    	
    	button.trigger("#submit");*/
			form.trigger("submit");
		});

		lQuery("a.ajax-edit-msg").livequery("click", function (e) {
			e.stopPropagation();
			e.preventDefault();
			const editbtn = $(this);
			const targetDiv = editbtn.data("targetdiv");
			const options = editbtn.cleandata();
			options.oemaxlevel = 1;
			const nextpage = editbtn.attr("href");
			$.get(nextpage, options, function (data) {
				//var cell = findclosest($(this), "#" + targetDiv);
				const cell = editbtn.closest("#" + targetDiv);
				cell.replaceWith(data);
				scrollToEdit(targetDiv);
			});
		});

		lQuery("a.appendgoalbutton").livequery("click", function (e) {
			const parent = $(this).closest(".goalstatusopen");
			if (parent) {
				parent[0].scrollIntoView();
			}
		});
	}

	function scrollToChat() {
		setTimeout(function () {
			const inside = $(".chatterbox-body-inside");
			if (inside.length > 0) {
				inside.animate({ scrollTop: inside.get(0).scrollHeight }, 30);
			}
		});
	}

	function scrollToEdit(targetDiv) {
		const messagecontainer = $("#" + targetDiv);
		if (messagecontainer.length) {
			messagecontainer.get(0).scrollIntoView();
		}
	}

	function connect() {
		if (chatConnection && chatConnection.readyState !== chatConnection.CLOSED) {
			return;
		}
		const tabID =
			sessionStorage.tabID && sessionStorage.closedLastTab !== "2"
				? sessionStorage.tabID
				: (sessionStorage.tabID = Math.random());
		sessionStorage.closedLastTab = "2";
		$(window).on("unload beforeunload", function () {
			sessionStorage.closedLastTab = "1";
		});

		const protocol = location.protocol;

		let url = `/entermedia/services/websocket/org/entermediadb/websocket/chat/ChatConnection?sessionid=${tabID}&userid=${userid}`;

		//Get the channel
		const channel = $(".chatterbox").data("channel");
		if (channel != null) {
			url = `${url}&channel=${channel}`;
		}

		if (protocol === "https:") {
			chatConnection = new WebSocket(`wss://${location.host}${url}`);
		} else {
			chatConnection = new WebSocket(`ws://${location.host}${url}`);
			// console.log(new Date().toISOString(), "Chat initialized with ws");
		}

		chatConnection.addEventListener("message", function (event) {
			// console.info(new Date().toISOString(), "Received message");

			$(window).trigger("ajaxsocketautoreload");
			const message = JSON.parse(event.data);
			const channelId = message.channel;
			const chatterbox = $(`div.chatterbox[data-channel="${channelId}"]`);

			if (message && chatterbox.length === 1) {
				//Channel on the screen no need to notify

				channelUpdateMessage(chatterbox, message);

				return;
			}

			registerServiceWorker();

			/*Check if you are the sender, play sound and notify. "message.topic != message.user" checks for private chat*/
			if (message.user !== userid && message.user !== "agent") {
				console.log(`Got a message: ${document.hasFocus()}`);
				if (!document.hasFocus()) {
					function showNotification() {
						console.log("Showing notification...");
						let header = "New Message";
						if (message.name !== undefined) {
							header = message.name;
						}
						if (message.topic !== undefined) {
							header += ` in ${message.topic}`;
						}
						let messagebody = message.message;
						if (messagebody !== null && messagebody !== undefined) {
							messagebody = "New message...";
						}
						const notification = new Notification(header, {
							//TODO: URL?
							body: message.message,
							renotify: false,
							tag: messagebody,
							icon: `${appHome}/theme/images/logo.png`,
						});
						notification.addEventListener("click", function (event) {
							//window.open('http://www.mozilla.org', '_blank');
						});
					}

					/*Check para permissions and ask.*/
					if (Notification.permission === "granted") {
						showNotification();
					} else if (Notification.permission !== "denied") {
						createNotificationSubscription();

						Notification.requestPermission().then((permission) => {
							if (permission === "granted") {
								showNotification();
							}
						});
					} else {
						console.log(
							`Notification Browser permission:${Notification.permission}`,
						);
					}
				}
			}
		});

		chatConnection.addEventListener("open", function () {
			keepAlive();
			const chatterbox = $(`div.chatterbox[data-channel="${channel}"]`);
			const chatterboxHome = chatterbox.data("chatterboxhome");
			const messagesUrl = chatterboxHome + "/index.html";
			let options = chatterbox.cleandata();
			if (!options) options = {};
			options.oemaxlevel = 1;
			$.get(messagesUrl, options, function (data) {
				chatterbox.replaceWith(data);
				scrollToChat();
			});
		});
		chatConnection.addEventListener("close", function () {
			// console.info(new Date().toISOString(), "Chat Connection Closed");
			// console.log("Chat Connection Closed");
		});
		chatConnection.addEventListener("error", function (event) {
			// console.error(new Date().toISOString(), "Chat Connection Error", event);
		});
	}

	const messages = {};

	function channelUpdateMessage(chatterbox, message) {
		//Cancel an existing one
		if (messages[message.messageid]) {
			messages[message.messageid] = setTimeout(function () {
				updateMessage(chatterbox, message);
			}, 1000);
		} else {
			messages[message.messageid] = true;
			updateMessage(chatterbox, message);
		}
	}

	function updateMessage(chatterbox, message) {
		console.info(new Date().toISOString(), message);

		const listArea = chatterbox.find(".chatterbox-message-list");

		const chatterboxHome = chatterbox.data("chatterboxhome");

		let renderMessageUrl = appHome + "/components/chatterbox/message.html";
		if (chatterboxHome.length) {
			renderMessageUrl = chatterboxHome + "/message.html";
		}

		const existing = listArea.find("#chatter-message-" + message.messageid);
		if (existing.length) {
			if (message.command === "messageremoved") {
				existing.remove();
			} else {
				const msgBody = $(existing).find(".msg-body-content");
				if (msgBody.length) {
					msgBody.html(message.message);
				} else {
					const chatMsg = $(existing).find(".chat-msg");
					chatMsg.html(message.message);
				}
			}

			scrollToChat();
			sortChatterbox(listArea);
			return;
		}

		scrollToChat();

		let options = chatterbox.cleandata();
		if (!options) options = {};
		const editdiv = chatterbox.closest(".editdiv");
		if (
			chatterbox.data("includeeditcontext") === undefined ||
			chatterbox.data("includeeditcontext") === true
		) {
			if (editdiv.length > 0) {
				const otherdata = editdiv.cleandata();
				options = {
					...otherdata,
					...options,
				};
			}
		}

		options.id = message.messageid;

		/*
	var params = {};
	params.id = message.id;
	params.channel = message.channel;
	if (message.entityid != null) {
		params.entityid = message.entityid;
		params.collectionid = message.entityid;
	} else {
		params.entityid = message.collectionid;
		params.collectionid = message.collectionid;
	}*/

		$.get(renderMessageUrl, options, function (data) {
			listArea.append(data);
			sortChatterbox(listArea);
			scrollToChat();
		});
	}

	function sortChatterbox(container) {
		//var messages = Array.from(container.querySelectorAll(".msg-bubble"));
		const messages = Array.from(container.find(".msg-bubble"));

		messages
			.sort((a, b) => {
				const dateA = parseInt(a.dataset.createdat);
				const dateB = parseInt(b.dataset.createdat);
				return dateA - dateB;
			})
			.forEach((el) => container.append(el));
	}

	let keepAliveTimeoutID = 0;

	function keepAlive() {
		if (!chatConnection) {
			return;
		}
		const timeout = 20000;
		if (chatConnection.readyState === chatConnection.OPEN) {
			const command = {};
			command.command = "keepalive";

			command.userid = userid;

			const chatter = $(".chatterbox").data("channel");
			command.channel = chatter;

			const json = JSON.stringify(command);
			chatConnection.send(json);
		}

		if (chatConnection.readyState === chatConnection.CLOSED) {
			connect();
			//reloadAll();
		}

		keepAliveTimeoutID = setTimeout(keepAlive, timeout);
	}

	function cancelKeepAlive() {
		if (keepAliveTimeoutID) {
			clearTimeout(keepAliveTimeoutID);
		}
	}

	/*-------Start Push and Notification --------*/
	const pushServerPublicKey =
		"BIN2Jc5Vmkmy-S3AUrcMlpKxJpLeVRAfu9WBqUbJ70SJOCWGCGXKY-Xzyh7HDr6KbRDGYHjqZ06OcS3BjD7uAm8";

	function registerServiceWorker() {
		if (navigator.serviceWorker !== undefined) {
			navigator.serviceWorker.register(
				appHome + "/components/chatterbox/sw.js",
			);
		}
	}

	function createNotificationSubscription() {
		//wait for service worker installation to be ready, and then
		return navigator.serviceWorker.ready.then(function (serviceWorker) {
			// subscribe and return the subscription
			return serviceWorker.pushManager
				.subscribe({
					userVisibleOnly: true,
					applicationServerKey: pushServerPublicKey,
				})
				.then(function (subscription) {
					// send this to Entermedia backend with a user id
					// 'subscription' == PushSubscription (object)
					console.log("User is subscribed.", subscription);
					console.log(subscription.endpoint);
					return subscription;
				});
		});
	}

	function hideAttachFile() {
		if ($(".message-attach-box").is(":visible")) {
			$(".message-attach-box").fadeOut(function () {
				$(this).remove();
				$(".chatter-attachfile").removeClass("active");
			});
		}
	}
	function hideEmojiPicker() {
		if ($(".emoji-picker").is(":visible")) {
			$(".emoji-picker").fadeOut(function () {
				$(this).remove();
				$(".chatter-emoji").removeClass("active");
			});
		}
	}

	lQuery("a.chatEmDialog").livequery("click", function (e) {
		e.preventDefault();
		e.stopPropagation();
		$(this).emDialog(function () {
			setTimeout(function () {
				scrollToChat();
			});
		});
	});
	lQuery("#supportchat").livequery("shown.bs.collapse	", function (e) {
		scrollToChat();
	});
	lQuery("#chatter-msg").livequery(function () {
		const $this = $(this);
		setTimeout(function () {
			$this.trigger("focus");
		});
	});

	lQuery(".chatterbox").livequery(function () {
		initChatterbox();
		scrollToChat();
	});

	lQuery(".expandaisearchtable").livequery("click", function (e) {
		e.preventDefault();
		e.stopPropagation();
		$(this).toggleClass("expanded");
		$(this)
			.closest(".aisearchtable-container")
			.find(".aisearchtable")
			.collapse("toggle");
	});

	lQuery(".chat-msg").livequery(function () {
		const emojiparsed = $(this).data("emojiparsed");
		if (emojiparsed) {
			return;
		}
		$(this).data("emojiparsed", true);
		const msgContent = $(this).find(".msg-body-content");
		const reacts = $(this).find("span.emote");
		if (window.parseEmojis !== undefined) {
			if (msgContent.length > 0) {
				window.parseEmojis(msgContent[0]);
			}
			if (reacts.length > 0) {
				window.parseEmojis(reacts[0]);
			}
		}
	});

	let chatSelectionStart = null;

	lQuery(".chatter-emoji").livequery("click", function (e) {
		e.preventDefault();
		e.stopPropagation();
		let textarea = $("#emojipicker").data("textarea");
		if (textarea) {
			textarea = $("#" + textarea);
		} else {
			textarea = $("#chatter-msg");
		}
		chatSelectionStart = textarea.prop("selectionStart");
		hideAttachFile();
		$(this).addClass("active");
		$(this).runAjax();
	});
	lQuery(".chatter-attachfile").livequery("click", function (e) {
		e.preventDefault();
		e.stopPropagation();
		hideEmojiPicker();
		$(this).addClass("active");
		$(this).runAjax();
	});

	lQuery("#emojinav a").livequery("click", function (e) {
		e.preventDefault();
		e.stopPropagation();
		const goTo = $(this).data("id");
		if (goTo === "smileys") {
			$(".emoji-wrapper").animate({ scrollTop: 0 }, 500);
			return;
		}
		$(".emoji-wrapper").scrollTop(0);
		const dest =
			$("#" + goTo).offset().top -
			$("#" + goTo)
				.offsetParent()
				.offset().top;
		$(".emoji-wrapper").animate({ scrollTop: dest - 70 }, 500);
	});

	lQuery(".emjbtn").livequery("click", function () {
		let textarea = $("#emojipicker").data("textarea");
		if (textarea) {
			textarea = $("#" + textarea);
		} else {
			textarea = $("#chatter-msg");
		}

		const emoji = $(this).text();
		let prev = textarea.val() || "";
		if (chatSelectionStart != null) {
			prev =
				prev.slice(0, chatSelectionStart) +
				emoji +
				prev.slice(chatSelectionStart);
		} else {
			prev += emoji;
		}

		textarea.val(prev);

		$(".emoji-picker").fadeOut(function () {
			textarea.trigger("focus");
			$(this).remove();
		});
	});
	function hideChatPickers(e) {
		if ($(e.target).closest("#emojipicker").length === 0) {
			hideEmojiPicker();
		}
	}

	lQuery("#closeattachfileonchat").livequery("click", function () {
		hideAttachFile();
	});

	lQuery("window").livequery("click", hideChatPickers);
	lQuery(".modal").livequery("click", hideChatPickers);

	/**Attachments */

	lQuery(".chat-msg-attachments-asset .removefieldassetvalueZ").livequery(
		"click",
		function (e) {
			$(this).runAjax();
		},
	);

	lQuery("a.lightbox").livequery(function () {
		const slb = $(this).simpleLightbox({
			captionSelector: "self",
			captionType: "data",
			captionsData: "caption",
			captionDelay: 250,
			widthRatio: 0.98,
			heightRatio: 0.98,
			overlayOpacity: 1,
			fadeSpeed: 60,
		});

		slb.on("shown.simplelightbox", function () {
			const lang = document.documentElement.lang;
			if (lang) {
				let locales = $(this).data("locales");
				if (locales) {
					if (typeof locales === "string") {
						const txt = document.createElement("textarea");
						txt.innerHTML = locales;
						locales = JSON.parse(txt.value);
					}
					const localeCaption = locales[lang];
					if (localeCaption) {
						$(".sl-caption").html(localeCaption);
					}
				}
			}

			const dl = $(this).data("downloadlink");
			const al = $(this).data("assetlink");
			if (dl || al) {
				$(".simple-lightbox .sl-actions").remove();
				$(".simple-lightbox").append("<div class='sl-actions'></div>");

				if (dl) {
					$(".simple-lightbox .sl-actions").append(
						"<a class='sl-btn sl-dl' href='" + dl + "' target='_blank'></a>",
					);
				}
				//Asset Link

				if (al) {
					$(".simple-lightbox .sl-actions").append(
						"<a class='sl-btn sl-al' href='" + al + "' target='_blank'></a>",
					);
				}
			}
		});
	});

	$(document).on("visibilitychange", function () {
		if (document.visibilityState === "visible") {
			if (chatConnection) {
				keepAlive();
			} else {
				if ($(".chatterbox").length > 0) {
					initChatterbox();
				}
			}
		}
	});
});




//Ended: /${applicationid}/components/chatterbox/chatterbox.js Size: 17.12 KB


/** 
 EnterMediaDB javascriptGenerator : 404 NOT FOUND/${applicationid}/components/javascript/emedia/ui-components-old.js
/finder/find/_site.xconf
  **/





/** 
 EnterMediaDB javascriptGenerator : 404 NOT FOUND/${applicationid}/components/javascript/emedia/ui-components.js
/finder/find/_site.xconf
  **/





/** 
 EnterMediaDB javascriptGenerator : /${applicationid}/components/javascript/emedia/emtree.js
/finder/find/_site.xconf
 Modified: Tue May 05 23:52:52 CST 2026 Size: 17.36 KB **/

$(function () {
	lQuery(".emtree-widget ul li div .cat-arrow").livequery(
		"click",
		function (e) {
			e.stopPropagation();
			toggleExpandNode.call(this);
		},
	);
	function toggleExpandNode(selecting = false) {
		//console.log($(this), selecting);
		var tree = $(this).closest(".emtree");
		var node = $(this).closest(".noderow");
		var iscurrent = $(this).hasClass("cat-current");
		var nodeid = node.data("nodeid");
		var depth = node.data("depth");
		tree.find("ul li div").removeClass("selected");

		var home = $(this).closest(".emtree").data("home");

		if ($(this).find(".cat-arrow").hasClass("down")) {
			$(this).find(".cat-arrow").removeClass("down");
		} else {
			//Open it. add a UL
			$(this).find(".cat-arrow").addClass("down");
		}

		tree.find(nodeid + "_add").remove();
		node.load(
			home +
				"/components/emtree/tree.html?toggle=true&treename=" +
				tree.data("treename") +
				"&nodeID=" +
				nodeid +
				"&depth=" +
				depth +
				"&canupload=" +
				tree.data("canupload") +
				(selecting ? "&selecting=true" : "") +
				(iscurrent ? "&currentnodeid=" + nodeid : ""),
			function () {
				$(window).trigger("resize");
			},
		);
	}

	lQuery(".emtree-widget ul li div .cat-name").livequery(
		"click",
		function (event) {
			event.stopPropagation();
			if (
				$(this).hasClass("cat-leaf") &&
				!$(this).parent().hasClass("expanded")
			) {
				toggleExpandNode.call($(this).siblings(".cat-arrow"), true);
			}
			var tree = $(this).closest(".emtree");
			var node = $(this).closest(".noderow");
			$("ul li div.cat-current", tree).addClass("categorydroparea");
			$("ul li div", tree).removeClass("selected cat-current");
			$("div:first", node)
				.addClass("cat-current")
				.removeClass("categorydroparea");
			var nodeid = node.data("nodeid");
			tree.data("currentnodeid", nodeid);
			var prefix = tree.data("urlprefix");

			//Regular Tree
			var options = [];
			var resultsdiv = tree.closest(".resultsdiv");
			if (resultsdiv.length) {
				resultsdiv.data("categoryid", nodeid);
				resultsdiv.data("nodeID", nodeid);
			}
			gotopage(tree, node, prefix, options);

			var event = $.Event("emtreeselect");
			event.tree = tree;
			event.nodeid = nodeid;
			$(document).trigger(event);

			var $contextMenu = $(".treecontext");
			if ($contextMenu.length > 0) {
				$contextMenu.hide();
			}
		},
	);

	lQuery(".treerow.cat-current").livequery(function () {
		console.log(this);
		$(this).scrollIntoView({
			offset: -50,
			container: ".searchcategories-tree",
		});
	});

	gotopage = function (tree, node, prefix, inOptions) {
		//postfix not used

		var treeholder = $("div#categoriescontent");
		var toplocation = parseInt(treeholder.scrollTop());
		var leftlocation = parseInt(treeholder.scrollLeft());
		var nodeid = node.data("nodeid");
		var home = tree.data("home");
		var depth = node.data("depth");
		var collectionid = node.data("collectionid");
		var reloadurl = "";
		var appnavtab = $("#appnavtab").data("openmodule");
		if (prefix == undefined || prefix == "") {
			//Asset Module
			reloadurl =
				home +
				"/views/modules/asset/editors/viewfilescategory/" +
				nodeid +
				"/" +
				node.data("categorynameesc") +
				".html";
			prefix = reloadurl;
		} else {
			var customprefix = tree.data("customurlprefix");
			if (customprefix) {
				reloadurl = customprefix;
			} else {
				reloadurl = prefix;
				reloadurl = reloadurl + "?nodeID=" + nodeid;
			}
		}

		var resultsdiv = tree.closest(".assetresults");
		if (!resultsdiv) {
			resultsdiv = $("#resultsdiv");
		}
		var hitssessionid = resultsdiv.data("hitssessionid");
		if (hitssessionid) {
			reloadurl = reloadurl + "?hitssessionid=" + hitssessionid;
		}

		var options = tree.cleandata();

		//includeeditcontext
		if (
			options["includeeditcontext"] === undefined ||
			options["includeeditcontext"] === true
		) {
			var editdiv = tree.closest(".editdiv"); //This is used for lightbox tree opening
			if (editdiv.length > 0) {
				var otherdata = editdiv.cleandata();
				options = {
					...otherdata,
					...options,
				};
			} else {
				//console.warn("No editdiv found for includeeditcontext");
			}
		}

		options["nodeID"] = nodeid;
		options["treetoplocation"] = toplocation;
		options["treeleftlocation"] = leftlocation;
		options["depth"] = depth;
		options["categoryid"] = nodeid;
		options["rootcategory"] = tree.data("rootnodeid");
		options["hitssessionid"] = hitssessionid;

		if (collectionid) {
			options.collectionid = collectionid;
		}
		var searchchildren = tree.data("searchchildren");
		if (appnavtab == "asset") {
			//for now
			searchchildren = true;
		}
		options.searchchildren = searchchildren;

		if (inOptions["oemaxlevel"]) {
			options.oemaxlevel = inOptions["oemaxlevel"];
		}

		//jQuery.get(prefix + nodeid + postfix,
		jQuery.get(
			prefix,
			{
				...options,
			},
			function (data) {
				//data = $(data);

				var targetdiv = tree.data("targetdivinner");
				var onpage;
				if (targetdiv) {
					var cell = jQuery("#" + targetdiv);
					onpage = cell;
					cell.html(data);
				} else {
					targetdiv = tree.data("targetdiv");
					if (targetdiv) {
						var cell = jQuery("#" + targetdiv);
						onpage = cell.parent();
						cell.replaceWith(data);
					}
				}

				cell = findClosest(onpage, "#" + targetdiv);

				$(window).trigger("setPageTitle", [cell]);

				if (
					typeof global_updateurl !== "undefined" &&
					global_updateurl == false
				) {
					//globaly disabled updateurl
				} else {
					//Update Address Bar
					if (tree.data("updateaddressbar")) {
						history.pushState($("#application").html(), null, reloadurl);
					}
				}

				$(window).trigger("resize");
			},
		);
	};

	var treeTop = $(".cat-current");
	if (treeTop.length) {
		$("div#treeholder").scrollTop(parseInt(treeTop.offset().top));
	}

	//need to init this with the tree
	lQuery("div#treeholder").livequery(function () {
		var treeholder = $(this);
		var top = treeholder.data("treetoplocation");
		if (top) {
			var left = treeholder.data("treeleftlocation");
			var catcontent = $("div#categoriescontent");
			catcontent.scrollTop(parseInt(top));
			catcontent.scrollLeft(parseInt(left));
		}
	});

	lQuery("#treeholder input").livequery("click", function (event) {
		event.stopPropagation();
	});

	lQuery("#treeholder input").livequery("keyup", function (event) {
		var input = $(this);
		var node = input.closest(".noderow");
		var tree = input.closest(".emtree");
		var value = input.val();
		//console.log("childnode",node);
		var nodeid = node.data("nodeid");
		if (event.keyCode == 13) {
			//13 represents Enter key
			var action = input.data("action");
			if (action != "create") {
				action = "rename";
			}
			var rootid = tree.data("treename") + "root";
			var link =
				tree.data("home") +
				"/components/emtree/savenode.html?action=" +
				action +
				"&treename=" +
				tree.data("treename") +
				"&" +
				rootid +
				"=" +
				tree.data("rootnodeid") +
				"&depth=" +
				node.data("depth");

			var targetdiv = tree.closest("#treeholder");

			if (action == "rename" && nodeid != undefined) {
				link = link + "&nodeID=" + nodeid;
				link = link + "&adding=true";

				targetdiv = node;
			} else {
				node = node.parent(".noderow");
				nodeid = node.data("nodeid");
				if (nodeid != undefined) {
					link = link + "&parentNodeID=" + nodeid;
				}
				var currentnodeid = tree.data("currentnodeid");
				if (currentnodeid) {
					link = link + "&currentnodeID=" + currentnodeid;
				}
			}
			//tree.closest("#treeholder").load(link, {edittext: value}, function() {
			var options = tree.data();
			options["edittext"] = value;
			$.get(
				link,
				{
					...options,
				},
				function (data) {
					targetdiv.replaceWith(data);
					//Reload tree in case it moved order
					//repaintEmTree(tree);
				},
			);
		} else if (event.keyCode === 27) {
			//esc
			input.closest(".createnodetree").hide();
		}
	});

	getNode = function (clickedon) {
		var clickedon = $(clickedon);
		var contextmenu = $(clickedon.closest(".treecontext"));
		var node = contextmenu.data("selectednoderow");
		if (!node) {
			node = $(clickedon).closest(".noderow");
		}
		contextmenu.hide();
		return node;
	};
	lQuery(".treecontext #nodeproperties").livequery("click", function (event) {
		event.stopPropagation();
		var node = getNode(this);
		var tree = node.closest(".emtree");
		var nodeid = node.data("nodeid");
		var link =
			tree.data("home") +
			"/views/modules/category/editors/data/editdialog.html?categoryid=" +
			nodeid +
			"&id=" +
			nodeid +
			"&viewid=categorygeneral";
		$(this).attr("href", link);
		$(this).data("dialogid", "categoryproperties");
		$(this).emDialog();
		return false;
	});

	lQuery(".treedesktopdownload").livequery("click", function (event) {
		event.stopPropagation();
		var node = getNode(this);
		var categoryid = node.data("nodeid");
		if (categoryid == null) {
			categoryid = $(this).data("categoryid");
		}
		listCategoryAssets($(this), event, categoryid);
	});

	lQuery(".treecontext #addmedia, .cat-uploadfromtree").livequery(
		"click",
		function (event) {
			event.stopPropagation();
			var node = getNode(this);
			var nodeid = node.data("nodeid");
			var tree = node.closest(".emtree");

			var collectionid = node.data("collectionid");
			var postfix = "";

			//clear other entities on Upload Form
			var options = [];
			//debugger;
			var customurladdmedia = tree.data("customurladdmedia");
			if (customurladdmedia) {
				var url = customurladdmedia;
				options["oemaxlevel"] = tree.data("uploadmaxlevel");
				gotopage(tree, node, url, options);
			} else {
				var url =
					tree.data("home") +
					"/views/modules/asset/editors/assetupload/index.html";
				//options["oemaxlevel"] = $(this).data("oemaxlevel");
				options["oemaxlevel"] = tree.data("uploadmaxlevel");
				options["updateurl"] = "false";
				gotopage(tree, node, url, options);
			}
			$(".treerow").removeClass("cat-current").addClass("categorydroparea");
			$("#" + nodeid + "_row > .treerow")
				.addClass("cat-current")
				.removeClass("categorydroparea");

			return false;
		},
	);

	lQuery(".addtomodule").livequery("click", function (event) {
		event.stopPropagation();

		var link = $(this);
		var node = getNode(this);
		var nodeid = node.data("nodeid");
		var tree = node.closest(".emtree");

		link.data("copyingcategoryid", nodeid);

		link.emDialog();

		return false;
	});

	lQuery(".treecontext #renamenode").livequery("click", function (event) {
		event.stopPropagation();
		var node = getNode(this);
		var tree = node.closest(".emtree");
		var nodeid = node.data("nodeid");
		var link =
			tree.data("home") +
			"/components/emtree/rename.html?treename=" +
			tree.data("treename") +
			"&nodeID=" +
			nodeid +
			"&depth=" +
			node.data("depth");
		node.find("> .treerow").load(link, function () {
			node.find("input").select().focus();
		});
		return false;
	});
	lQuery(".treecontext #deletenode").livequery("click", function (event) {
		event.stopPropagation();
		var node = getNode(this);
		var tree = node.closest(".emtree");
		var nodeid = node.data("nodeid");
		var agree = confirm("Are you sure you want to delete?");
		if (agree) {
			//console.log("removing",node, nodeid);
			var link =
				tree.data("home") +
				"/components/emtree/delete.html?treename=" +
				tree.data("treename") +
				"&nodeID=" +
				nodeid +
				"&depth=" +
				node.data("depth");
			var options = tree.data();
			$.get(
				link,
				{
					...options,
				},
				function (data) {
					//tree.closest("#treeholder").replaceWith(data);
					//Reload tree in case it moved order
					repaintEmTree(tree);
				},
			);
		}
		return false;
	});
	lQuery(".treecontext #createnode").livequery("click", function (event) {
		event.stopPropagation();
		var node = getNode(this);
		var tree = node.closest(".emtree");
		var link =
			tree.data("home") +
			"/components/emtree/create.html?treename=" +
			tree.data("treename") +
			"&depth=" +
			node.data("depth");
		$.get(link, function (data) {
			node.append(data);
			var theinput = $("#treeaddnodeinput");
			if (theinput.length > 0) {
				theinput.focus({ preventScroll: false });
				//theinput.trigger("select");
				//theinput.focus();
			}
			$(document).trigger("domchanged");
		});
		return false;
	});

	lQuery(".createfoldertree").livequery("click", function (event) {
		event.stopPropagation();
		var link = $(this);

		var tree = $("#" + link.data("tree"));

		var node = tree.find(".rootnoderow");

		var link =
			tree.data("home") +
			"/components/emtree/create.html?treename=" +
			tree.data("treename") +
			"&depth=" +
			node.data("depth");
		$.get(link, function (data) {
			node.append(data);
			var theinput = node.find("input");
			theinput.focus({ preventScroll: false });
			//theinput.trigger("select");
			theinput.trigger("focus");
			$(document).trigger("domchanged");
		});
		return false;
	});

	lQuery(".treecontext #createcollection").livequery("click", function (event) {
		event.stopPropagation();
		var node = getNode(this);
		var nodeid = node.data("nodeid");

		var tree = node.closest(".emtree");
		var link =
			tree.data("home") +
			"/views/modules/librarycollection/createcollection.html";

		var catoptions = node.data();

		link =
			link +
			"?oemaxlevel=3&field=rootcategory&rootcategory.value=" +
			catoptions.nodeid +
			"&field=name&name.value=" +
			catoptions.categoryname;

		var targetdiv = "application";

		$.get(link, function (data) {
			//var cell = jQuery("#" + targetdiv);
			//cell.replaceWith(data);

			customToast("Collection Created");
		});
		return false;
	});

	lQuery(".treecontext #togglefeatured").livequery("click", function (event) {
		event.stopPropagation();
		var node = getNode(this);
		var nodeid = node.data("nodeid");
		var catoptions = node.data();
		var tree = node.closest(".emtree");
		var link = tree.data("home") + "/components/emtree/togglefeatured.html";

		link = link + "?categoryid=" + catoptions.nodeid;

		var targetdiv = "application";

		$.get(link, function (data) {
			//var cell = jQuery("#" + targetdiv);
			//cell.replaceWith(data);
			if (node.data("isfeatured")) {
				node.data("isfeatured", false);
				customToast("Category Removed from Featured");
			} else {
				node.data("isfeatured", true);
				customToast("Category Marked as Featured");
			}
		});
		return false;
	});

	lQuery(".treecontext #downloadnode").livequery("click", function (event) {
		event.stopPropagation();
		var node = getNode(this);
		var nodeid = node.data("nodeid");
		var catname = node.data("categorynameesc");

		var tree = node.closest(".emtree");

		var link =
			tree.data("home") +
			"/views/modules/asset/downloads/bycategory/" +
			nodeid +
			"/" +
			catname +
			".zip";
		window.location.href = link;
		return false;
	});

	function getPosition(e) {
		var posx = 0;
		var posy = 0;

		if (!e) var e = window.event;

		if (e.clientX || e.clientY) {
			posx = e.clientX;
			posy = e.clientY;
		} else if (e.pageX || e.pageY) {
			posx = e.pageX;
			posy = e.pageY;
		}

		return {
			x: posx,
			y: posy,
		};
	}

	var contextmenu = function (item, e) {
		var noderow = item;
		//var noderow = $(this); // LI is the think that has context .find("> .noderow");
		$(".categorydroparea").removeClass("selected");
		noderow.find("> .categorydroparea").addClass("selected");
		var emtreediv = noderow.closest(".emtree");
		var treename = emtreediv.data("treename");
		var contextMenu = $("#" + treename + "contextMenu");
		if (contextMenu.length > 0) {
			e.preventDefault();
			var pos = getPosition(e);
			var xPos = pos.x;
			if (xPos < 16) {
				xPos = xPos + 16;
			}
			var yPos = pos.y;

			contextMenu.data("selectednoderow", noderow);
			var iscollection = noderow.data("collectionid");
			$("#" + treename + "contextMenu #createcollection").show();
			if (iscollection != null && iscollection != "") {
				$("#" + treename + "contextMenu #createcollection").hide();
			}
			var isfeatured = noderow.data("isfeatured");
			var menuitem = $("#" + treename + "contextMenu #togglefeatured");
			if (isfeatured) {
				menuitem.text(menuitem.data("removefeatured"));
			} else {
				menuitem.text(menuitem.data("addfeatured"));
			}

			contextMenu.css({
				display: "block",
				left: xPos,
				top: yPos,
			});
			e.stopPropagation();
			return false;
		}
	};

	$("body").on("contextmenu", ".noderow", function (e) {
		contextmenu($(this), e);
	});
	lQuery(".cat-menu").livequery("click", function (e) {
		contextmenu($(this).closest(".noderow"), e);
	});

	lQuery("body").livequery("click", function () {
		var $contextMenu = $(".treecontext");
		if ($contextMenu.length > 0) {
			$contextMenu.hide();
			$(".categorydroparea").removeClass("selected");
		}
	});

	$(document).on("keydown", function (e) {
		switch (e.which) {
			case 27: // esc
				var $contextMenu = $(".treecontext");
				if ($contextMenu.length) {
					$contextMenu.hide();
					$(".categorydroparea").removeClass("selected");
					e.preventDefault();
				}
				break;
			default:
				return; // exit this handler for other keys
		}
	});

	jQuery(document).on("emtreeselect", function (event) {
		var treename = event.tree.data("treename");
		if (treename == "sidebarCategories") {
			var selectednode = event.nodeid;
			$("#parentfilter").val(selectednode);

			$("#autosubmitfilter").trigger("submit");
		}
		return false;
	});

	function repaintEmTree(tree) {
		var home = tree.data("home");
		var link = home + "/components/emtree/tree.html";
		var options = tree.data();
		options["treename"] = tree.data("treename"); //why?
		$.get(
			link,
			{
				...options,
			},
			function (data) {
				tree.closest("#treeholder").replaceWith(data);
				$(document).trigger("domchanged");
			},
		);
	}
});




//Ended: /${applicationid}/components/javascript/emedia/emtree.js Size: 17.36 KB


/** 
 EnterMediaDB javascriptGenerator : /${applicationid}/components/javascript/emedia/results.js
/finder/find/_site.xconf
 Modified: Tue May 05 23:52:52 CST 2026 Size: 55.72 KB **/

jQuery(document).ready(function (url, params) {
	var appdiv = $("#application");
	var apphome = appdiv.data("apphome");
	var siteroot = appdiv.data("siteroot") + appdiv.data("apphome");
	var componenthome = appdiv.data("siteroot") + appdiv.data("componenthome");

	var header = $("#header");

	var headerHeight = header.outerHeight(true);
	if (!headerHeight) {
		headerHeight = 0;
	}

	lQuery("div.masonry-grid").livequery(function () {
		$(this).brick();
	});

	lQuery("div.brickvertical").livequery(function () {
		$(this).brickvertical();
	});

	lQuery("#entityNavBarContainer").livequery(function () {
		var _top = headerHeight;
		$(this).css("top", _top + "px");
		_top += $(this).outerHeight(true);
		var breadCrumbContainer = $("#breadCrumbContainer");
		if (breadCrumbContainer) {
			breadCrumbContainer.css("top", _top + "px");
		}
		_top += breadCrumbContainer.outerHeight(true);
		var defaultExpandedModule = $("#defaultExpandedModule");
		if (defaultExpandedModule.length) {
			defaultExpandedModule.css("top", _top + "px");
		}
		var assetresultscontainer = $("#assetresultscontainer");
		if (assetresultscontainer.length) {
			assetresultscontainer.css("top", _top + "px");
		}
	});

	lQuery("#assetlocked").livequery("change", function () {
		var locked = $(this).prop("checked");
		var uncheckedlabel = $(this).data("uncheckedlabel");
		var checkedlabel = $(this).data("checkedlabel");
		var user = $(this).data("user");
		if (locked) {
			$(this)
				.next("label")
				.html(checkedlabel + " <strong>" + user + "</strong>");
		} else {
			$(this).next("label").text(uncheckedlabel);
		}
	});

	var refreshdiv = function (targetdiv, url, params) {
		jQuery.ajax({
			url: url,
			async: false,
			data: params,
			success: function (data) {
				targetdiv.replaceWith(data);
			},
			xhrFields: {
				withCredentials: true,
			},
			crossDomain: true,
		});
	};

	$(".emlogo").on("click", function (e) {
		e.preventDefault();
		var href = $(this).attr("href");
		var url = window.location.href;
		if (href === url) {
			if (window.screenY > 0) {
				window.focus();
				window.scrollTo({
					top: 0,
					behavior: "smooth",
				});
			}
			$("#emselectable").animate({ scrollTop: 0 }, 500);
		} else {
			window.location.href = href;
		}
	});

	lQuery(".formatDate").livequery(function () {
		var _this = $(this);
		_this.each(function () {
			var datetype = _this.data("datetype");
			var fDate = [];
			var dates = [];
			if (datetype === "betweendates") {
				dates = _this.text().split("-");
			} else {
				dates.push(_this.text());
			}
			dates.forEach(function (date) {
				var d = new Date(date).toDateString();
				if (d == "Invalid Date") {
					return;
				}
				var date = d.substring(4, 15);
				fDate.push(date);
			});
			_this.html(fDate.join(" &mdash; "));
		});
	});

	lQuery("select#selectresultview").livequery(function () {
		var select = $(this);
		var resultsdiv = select.closest(".resultsdiv");

		select.on("change", function () {
			var searchhome = resultsdiv.data("searchhome");
			var href = searchhome + "/changeresultview.html";
			resultsdiv.data("url", href);

			var resultviewselected = select.val();
			resultsdiv.data("changeresultview", resultviewselected);

			if (
				resultviewselected == "stackedgallery" ||
				resultviewselected == "brickvertical"
			) {
				resultsdiv.data("page", "1");
			}
			resultsdiv.data("no-toast", true);
			resultsdiv.runAjax();
		});
	});

	lQuery(".hitsperpagechange").livequery(function () {
		var select = $(this);

		var resultsdiv = select.closest(".resultsdiv");

		select.on("change", function () {
			if (resultsdiv.data("oemaxlevel") === undefined) {
				resultsdiv.data("oemaxlevel", "1");
			}

			var searchhome = resultsdiv.data("searchhome");
			resultsdiv.data("url", searchhome + "/changehitsperpage.html");
			resultsdiv.data("hitsperpage", select.val());
			resultsdiv.data("no-toast", true);
			resultsdiv.runAjax();
		});
	});

	lQuery("input#jumptopageresults").livequery(function () {
		var input = $(this);
		input.on("keydown", function (e) {
			if (e.key === "Enter" || e.keyCode === 13) {
				var page = input.val();
				var maxpage = input.data("maxpage");

				if (!page || page < 1 || page > maxpage) {
					alert(
						"Invalid page number. Enter a number between 1 - " + maxpage + ".",
					);
					input.val("");
					return;
				}
				var url = input.data("url");
				input.data("url", url + page);
				var urlbar = input.data("urlbar");
				if (urlbar !== undefined) {
					input.data("urlbar", urlbar + page);
				}
				input.data("no-toast", true);
				input.runAjax();
			}
		});
	});

	lQuery(".filterschangesort").livequery("click", function (e) {
		// debugger;
		e.preventDefault();
		var sortbyfield = $(this).data("sortbyfield");
		var dropdown = $("#" + sortbyfield);
		var up = dropdown.data("sortup");
		var selected = dropdown.find(":selected");
		var id = selected.data("detailid");
		var icon = $(this).find("i");
		if (up) {
			selected.attr("value", id + "Down");
			//icon.removeClass("fa-sort-alpha-down");
			//icon.addClass("fa-sort-alpha-up");

			dropdown.data("sortup", false);
		} else {
			selected.attr("value", id + "Up");
			//icon.removeClass("fa-sort-alpha-up");
			//icon.addClass("fa-sort-alpha-down");

			dropdown.data("sortup", true);
		}
		var form = selected.closest("form");
		form.trigger("submit");
		
		return false;
	});

	//clearfiltersearch

	lQuery(".clearfiltersearch").livequery("click", function (e) {
		// debugger;
		var box = $(this).parent().find(".inlinefiltersearch");
		if (box.length) {
			box.val("");
		}
		$(this).hide();
	});

	lQuery(".inlinefiltersearch").livequery("keydown", function (e) {
		if (e.which == 13) {
			$(".clearfiltersearch").show();
		}
	});

	lQuery("a.clearsearchbar").livequery("click", function () {
		$("#mainsearchvalue").val("");
	});

	lQuery(".resultsheader").livequery(function () {
		if ($(this).hasClass("hasselections")) {
			$(this).parent().find("#unselectall").show();
		} else {
			$(this).parent().find("#unselectall").hide();
		}
	});

	lQuery(".gallery-checkbox input").livequery("click", function () {
		if ($(this).is(":checked")) {
			$(this).closest(".emthumbbox").addClass("selected");
		} else {
			$(this).closest(".emthumbbox").removeClass("selected");
		}
	});

	lQuery(".moduleselectionbox").livequery("click", function (e) {
		e.stopPropagation();

		var dataid = $(this).data("dataid");
		var sessionid = $(this).data("hitssessionid");

		$.get(componenthome + "/moduleresults/selections/toggle.html", {
			dataid: dataid,
			hitssessionid: sessionid,
		});

		return;
	});

	$.fn.exists = function () {
		return this.length !== 0;
	};

	getCurrentAssetId = function () {
		var assetdialog = $("#main-media-viewer");
		return assetdialog.data("assetid");
	};

	function enable(inData, inSpan) {
		if (inData == "") {
			$(inSpan).addClass("arrowdisabled");
			$(inSpan).data("enabled", "false");
			$(inSpan).attr("data-enabled", "false");
		} else {
			$(inSpan).addClass("arrowenabled");
			$(inSpan).data("enabled", "true");
			$(inSpan).attr("data-enabled", "true");
		}
	}

	showAsset = function (link, assetid, pagenum) {
		if (link.length > 0) {
			link.data("includeeditcontext", true);
			link.data("includesearchcontext", true);
			link.data("dialogid", "mediaviewer");
			link.data("edithome", "");
			//var url = link.attr("href");
			link.data(
				"url",
				apphome +
					"/views/modules/asset/mediaviewer/fullscreen/currentasset.html",
			);
			if (assetid === undefined) {
				assetid = link.data("assetid");
			}

			var editdiv = link.closest(".editdiv");
			if (editdiv.hasClass("faceprofileassetsearch")) {
				var entityid = editdiv.data("entityid");
				link.data("showfaceprofileid", entityid);
			}

			if (assetid !== undefined) {
				link.data("assetid", assetid);
				var url = window.location.href;
				const urlObj = new URL(url, window.location.origin);
				urlObj.searchParams.set("assetid", assetid);
				url = urlObj.toString();
				history.pushState($("#application").html(), null, url);
				//window.location.hash = "asset-" + assetid;
				link.emDialog();
			}
		}
	};

	hideMediaViewer = function () {
		var assetdialog = $("#mediaviewer");
		if (assetdialog.length) {
			closeemdialog(assetdialog);
		}
	};

	refreshresults = function () {
		var resultsdiv = $("#resultsdiv");
		if (resultsdiv.length) {
			var href = siteroot + "/views/search/index.html";
			var searchdata = resultsdiv.data();
			searchdata.oemaxlevel = 1;
			searchdata.cache = false;
			$.ajax({
				url: href,
				async: false,
				data: searchdata,
				success: function (data) {
					$("#filteredresults").html(data);
					$(window).trigger("resize");
				},
			});
		}
	};

	lQuery("#jumptoform .jumpto-left").livequery("click", function (e) {
		e.preventDefault();
		var input = $("#jumptoform #pagejumper");
		var current = input.val();
		current = parseInt(current);
		current--;
		if (current > 0) {
			input.val(current);
			$("#jumptoform").submit();
		} else {
			$("#jumptoform .jumpto-left").addClass("invisible");
		}

		$("#jumptoform .jumpto-right").removeClass("invisible");
	});

	lQuery("#jumptoform .jumpto-right").livequery("click", function (e) {
		e.preventDefault();
		var input = $("#jumptoform #pagejumper");
		var current = input.val();
		current = parseInt(current);
		current++;
		var totalpages = $("#jumptoform").data("totalpages");
		totalpages = parseInt(totalpages);
		if (current <= totalpages) {
			input.val(current);
			$("#jumptoform").submit();
		}
		if (current >= totalpages) {
			$("#jumptoform .jumpto-right").addClass("invisible");
		}
		$("#jumptoform .jumpto-left").removeClass("invisible");
	});

	lQuery(".gotoarrows").livequery("click", function (e) {
		e.preventDefault();
		var link = $(this);
		var id = link.data("assetid");
		var enabled = link.hasClass("arrowenabled");
		if (id && enabled) {
			showAsset(link, id);
		}
	});

	lQuery(".carousel-indicators li#leftpage").livequery("click", function (e) {
		e.preventDefault();
		var div = $("#main-media-viewer");
		var id = div.data("previouspage");
		if (id) {
			showAsset($(this), id);
		}
	});
	lQuery(".carousel-indicators li#rightpage").livequery("click", function (e) {
		e.preventDefault();
		var div = $("#main-media-viewer");
		var id = div.data("nextpage");
		if (id) {
			showAsset($(this), id);
		}
	});

	lQuery("#main-media").livequery("swipeleft", function () {
		var div = $("#main-media-viewer");
		var id = div.data("next");
		if (id) {
			showAsset($(this), id);
		}
	});
	lQuery("#main-media").livequery("swiperight", function () {
		var div = $("#main-media-viewer");
		var id = div.data("previous");
		if (id) {
			showAsset($(this), id);
		}
	});

	// Select multiple assets with Shift+Mouse
	var isMouseDown = false;
	var currentCol;
	lQuery(".stackedplayertable td").livequery("mousedown", function (e) {
		isMouseDown = true;
		if (e.shiftKey) {
			var row = $(this).closest("tr");
			currentCol = row.data("rowid");
			if (currentCol) {
				// row.toggleClass("emrowselected");
				var isHighlighted = row.hasClass("emrowselected");
				var chkbox = row.find(".selectionbox");
				$(chkbox).prop("checked", true);
				$(chkbox).trigger("change");
			}
		}
		return false; // Prevent text selection
	});

	lQuery(".stackedplayertable td").livequery("mouseover", function (e) {
		if (isMouseDown && e.shiftKey) {
			// Mouse + Shift Key
			var row = $(this).closest("tr");
			var currentColDown = row.data("rowid");
			var isHighlighted = row.hasClass("emrowselected");
			if (currentColDown && !isHighlighted) {
				// row.toggleClass("emrowselected", isHighlighted);
				var chkbox = row.find(".selectionbox");
				$(chkbox).prop("checked", true);
				$(chkbox).trigger("change");
			}
		}
	});

	$(window).on("mouseup", function () {
		isMouseDown = false;
	});

	lQuery(".stackedplayer").livequery("click", function (e) {
		var clicked = $(this);
		var pickerresults = clicked.closest(
			".clickableresultlist, .clickableresultlistinline, .pickerpickasset",
		);

		if (pickerresults.length > 0) {
			return;
		}

		e.preventDefault();
		e.stopPropagation();
		var link = $(this);
		showAsset(link);

		return false;
	});

	// Click on asset
	var selectStart = null;

	lQuery(".mediavieweropener table.emresultstable tr td").livequery(
		"click",
		function (e) {
			var clicked = $(this);
			var pickerresults = clicked.closest(".clickableresultlist");
			if (pickerresults.length > 0) {
				return;
			}

			clicked = clicked.closest("tr");
			if ($(e.target).is("input") || $(e.target).is("a")) {
				return true;
			}
			// click+ctrl
			if (e.ctrlKey) {
				var chkbox = clicked.find(".selectionbox");
				if (chkbox) {
					var ischecked = $(chkbox).prop("checked");
					if (!ischecked || ischecked == "true") {
						$(chkbox).prop("checked", true);
					} else {
						$(chkbox).prop("checked", false);
					}
					$(chkbox).trigger("change");
				}
				return false;
			}
			// click+shift
			if (e.shiftKey) {
				if (selectStart == null) {
					selectStart = clicked;
				} else {
					var selectEnd = clicked;
					if (selectStart) {
						$(selectStart)
							.nextUntil($(selectEnd))
							.each(function () {
								var chkbox = $(this).find(".selectionbox");
								if (chkbox) {
									var ischecked = $(chkbox).prop("checked");
									if (!ischecked || ischecked == "true") {
										$(chkbox).prop("checked", true);
									} else {
										$(chkbox).prop("checked", false);
									}
									$(chkbox).trigger("change");
								}
							});
						selectStart = null;
						selectEnd = null;
					}
				}
				return false;
			}

			e.preventDefault();
			e.stopPropagation();
			var assetid = clicked.data("dataid");
			showAsset(clicked, assetid);
		},
	);
	// Gallery clicking
	lQuery(".emgallery .emthumbimage").livequery("click", function (e) {
		var clicked = $(this);
		var ctrlPressed = e.ctrlKey || e.metaKey;
		if (ctrlPressed) {
			var chkbox = clicked.closest(".emboxthumb").find(".selectionbox");
			if (chkbox) {
				var ischecked = $(chkbox).prop("checked");
				if (!ischecked || ischecked == "true") {
					$(chkbox).prop("checked", true);
				} else {
					$(chkbox).prop("checked", false);
				}
				$(chkbox).trigger("change");
			}
			e.preventDefault();
			e.stopPropagation();
			return false;
		}
		// click+shift
		if (e.shiftKey) {
			if (selectStart == null) {
				selectStart = $(clicked).closest(".emboxthumb");
			} else {
				var selectEnd = $(clicked).closest(".emboxthumb");
				if (selectStart) {
					$(selectStart)
						.nextUntil($(selectEnd))
						.each(function () {
							var chkbox = $(this).find(".selectionbox");
							if (chkbox) {
								var ischecked = $(chkbox).prop("checked");
								if (!ischecked || ischecked == "true") {
									$(chkbox).prop("checked", true);
								} else {
									$(chkbox).prop("checked", false);
								}
								$(chkbox).trigger("change");
							}
						});
					selectStart = null;
					selectEnd = null;
				}
			}
			e.preventDefault();
			e.stopPropagation();
			return false;
		}
	});

	//Launch the Dialog? Use a parmeter check instead
	openEntity = function () {
		var entity = $(".showentity");
		if (entity.length) {
			var resultsdiv = $(".resultsdiv");
			var moduleid = resultsdiv.data("moduleid");
			var searchhome = resultsdiv.data("searchhome");
			if (searchhome) {
				var url = searchhome + "/tabs/index.html";
				entity.data("targetlink", url);
				entity.data("updateurl", true);
				entity.data("urlbar", window.location.href);
				var currenturl = window.location.origin + window.location.pathname;
				history.pushState($("#application").html(), null, currenturl);

				entity.emDialog();
			}
		}
	};

	// Selections
	function handRowSelection(clicked) {
		var resultsdiv = clicked.closest(".resultsdiv");

		if (resultsdiv.length) {
			var ischecked = clicked.prop("checked");
			if (ischecked == true) {
				clicked.closest(".resultsassetcontainer").addClass("emrowselected");
			} else {
				clicked.closest(".resultsassetcontainer").removeClass("emrowselected");
			}

			var resultsheader = resultsdiv.find(".resultsheader");
			clicked.data("selectedtargetdiv", resultsheader);
			clicked.data("oemaxlevel", "1");
			clicked.data("includesearchcontext", true);
			clicked.data("includeeditcontext", true);

			var searchhome = resultsdiv.data("searchhome");
			clicked.data("url", searchhome + "/toggle.html");
			clicked.data("no-toast", true);
			clicked.runAjax();

			//$(".assetproperties").trigger("click");
		}
	}

	lQuery("input.resultsselection.selectionbox").livequery(
		"change",
		function () {
			handRowSelection($(this));
		},
	);

	lQuery("input[name=pagetoggle]").livequery("click", function () {
		var clicked = $(this);
		var resultsdiv = clicked.closest(".resultsdiv");

		var status = clicked.is(":checked");

		var searchhome = resultsdiv.data("searchhome");

		var action;
		if (status) {
			action = "page";
			$(".selectionbox", resultsdiv).prop("checked", true);
		} else {
			action = "pagenone";
			$(".selectionbox", resultsdiv).prop("checked", false);
		}

		var resultsheader = resultsdiv.find(".resultsheader");

		clicked.data("selectedtargetdiv", resultsheader);
		clicked.data("action", action);
		clicked.data("oemaxlevel", "1");
		clicked.data("includesearchcontext", true);
		clicked.data("includeeditcontext", true);
		clicked.data("url", searchhome + "/togglepage.html");
		clicked.data("no-toast", true);
		clicked.runAjax();
	});

	lQuery("a.selectpage").livequery("click", function (e) {
		e.preventDefault();
		e.stopPropagation();
		var selectpage = $(this);
		var resultsdiv = selectpage.closest(".resultsdiv");
		if (!resultsdiv.length) {
			resultsdiv = $("#resultsdiv");
		}
		var action = selectpage.data("action");

		$(".selectionbox", resultsdiv).prop("checked", action != "none");
		$("input[name=pagetoggle]", resultsdiv).prop("checked", action != "none");
		var resultsheader = resultsdiv.find(".resultsheader");
		selectpage.data("selectedtargetdiv", resultsheader);

		selectpage.data("oemaxlevel", "1");
		selectpage.runAjax();
	});

	lQuery(".showasset").livequery("click", function (e) {
		var clicked = $(this);
		if (clicked.attr("noclick") == "true") {
			return true;
		}

		e.preventDefault();
		e.stopPropagation();

		var assetid = clicked.data("assetid");
		showAsset(clicked, assetid);
	});

	lQuery("a#multiedit-menu").livequery("click", function (e) {
		e.preventDefault();
		var link = $(this);
		var assetid = link.data("assetid");
		showAsset(link, assetid, 1);
		return false;
	});

	lQuery("#hiddenoverlay .overlay-popup span").livequery("click", function (e) {
		e.preventDefault();
		// editor/viewer/index.html?hitssessionid=${hits.getSessionId()}&assetid=${hit.id}
		var hitssessionid = $("#resultsdiv").data("hitssessionid");
		var href =
			home +
			"/views/modules/asset/editor/viewer/index.html?hitssessionid=" +
			hitssessionid +
			"&assetid=" +
			getCurrentAssetId();
		window.location = href;
	});

	lQuery(".tableresultsaddcolumn").livequery("change", function () {
		var selector = $(this);
		var targetdiv = selector.data("targetdiv");
		var selectedval = $(this).val();
		if (selectedval) {
			var link = selector.data("componenthome");
			var args = {
				addcolumn: selectedval,
				oemaxlevel: selector.data("oemaxlevel"),
			};

			// jQuery("#"+targetdiv).load(link);
			$.get(link, args, function (data) {
				$("#" + targetdiv).replaceWith(data);
				$(window).trigger("resize");
			});
		}
	});

	function quickPaintFace() {
		var facebox = $(this).data("facebox");
		var originalwidth = $(this).data("originalwidth");
		if (facebox && originalwidth) {
			$(this).find("canvas").remove();
			var canv = document.createElement("canvas");
			canv.className = "faceboxpreview";

			var imgWidth = 0;
			var imgHeight = 0;

			var masonry = $(this).closest(".masonry-grid-cell");
			if (masonry.length) {
				imgWidth = $(this).closest(".masonry-grid-cell").width();
				imgHeight = $(this).closest(".masonry-grid-cell").height();
			} else {
				var stackedplayer = $(this).closest(".stackedplayer");
				if (stackedplayer.length) {
					imgWidth = $(this).find("img.imagethumb").width();
					imgHeight = $(this).find("img.imagethumb").height();
				}
			}

			if (imgWidth == 0 || imgHeight == 0) {
				return;
			}

			canv.width = imgWidth;
			canv.height = imgHeight;
			var ctx = canv.getContext("2d");
			this.appendChild(canv);
			ctx.clearRect(0, 0, canv.width, canv.height);
			ctx.strokeStyle = "rgba(180, 255, 180)";
			ctx.lineWidth = 2;
			var [x, y, width, height] = facebox;
			var scale = imgWidth / originalwidth;
			x *= scale;
			y *= scale;
			width *= scale;
			height *= scale;
			ctx.strokeRect(x, y, width, height);
			ctx.fillStyle = "rgba(180, 255, 180, 0.1)";
			ctx.fillRect(x, y, width, height);
		}
	}

	$(window).on("resultsgenerated", function (event, resultsarea) {
		//console.log("on(resultsgenerated", resultsarea);
		setTimeout(function () {
			$(".renderfaceboxes", resultsarea).each(function () {
				quickPaintFace.apply(this);
			});
		});
	});

	$(document).on("resize", function () {
		setTimeout(function () {
			$(".resultsarea .renderfaceboxes").each(function () {
				//console.log("on(renderfaceboxes");
				quickPaintFace.apply(this);
			});
		});
	});

	/*
  $(window).on("renderfaceboxes", function (_, target) {
    if (!target) return;
	console.log("on(renderfaceboxes");

    quickPaintFace.apply(target);
  });
*/

	lQuery(".emgallery").livequery(function () {
		//TODO: Use width and height instead
		$(window).trigger("resultsgenerated", [$(this)]);
	});

	$(window).on("hideMediaViewer", function () {
		hideMediaViewer();
	});

	var faceCanvas,
		faceCanvasCtx,
		faceEventsRegistered = false;

	const faceCanvasResizeObserver = new ResizeObserver(() => {
		if (faceCanvas) {
			faceCanvas.remove();
		}
	});

	function paintFaceBoxes(image, facesData) {
		var imageContainer = image.closest(".imagethumbholder");
		if (!imageContainer.length) {
			return;
		}
		faceCanvasResizeObserver.observe(imageContainer.get(0));
		faceCanvas = imageContainer.find("canvas.face-canvas").get(0);
		if (!faceCanvas) {
			faceCanvas = document.createElement("canvas");
			faceCanvas.classList.add("face-canvas");
			faceCanvas.width = image.width();
			faceCanvas.height = image.height();
			imageContainer.append(faceCanvas);
			faceCanvasCtx = faceCanvas.getContext("2d");
			faceEventsRegistered = true;
		}
		if (!faceCanvasCtx) return;
		faceCanvasCtx.clearRect(0, 0, faceCanvas.width, faceCanvas.height);

		var imgWidth = image.width();

		var nameRectBound = {};

		facesData.forEach(function (faceData) {
			var id = faceData.faceembeddingid;
			var mainImgWidth = faceData.originalwidth;
			var scale = imgWidth / mainImgWidth;

			var [boxLeft, boxTop, boxWidth, boxHeight] = faceData.location;

			boxLeft *= scale;
			boxTop *= scale;
			boxWidth *= scale;
			boxHeight *= scale;

			faceCanvasCtx.lineWidth = 2;

			if (!faceData.facename) {
				faceCanvasCtx.fillStyle = "rgba(255, 255, 255, 0.05)";
				faceCanvasCtx.strokeStyle = "rgba(255, 255, 255, 0.5)";
			} else {
				faceCanvasCtx.fillStyle = "rgba(255, 255, 255, 0.08)";
				faceCanvasCtx.strokeStyle = "rgba(255, 255, 255, 0.5)";
			}
			if (facesData.length === 1) {
				faceCanvasCtx.setLineDash([8, 5]);
			} else {
				faceCanvasCtx.setLineDash([5, 5]);
			}
			faceCanvasCtx.fillRect(boxLeft, boxTop, boxWidth, boxHeight);
			faceCanvasCtx.strokeRect(boxLeft, boxTop, boxWidth, boxHeight);

			var facename = "Assign Person";
			if (faceData.facename) {
				facename = faceData.facename.trim();
			}
			faceCanvasCtx.font = "16px Arial";
			faceCanvasCtx.textAlign = "center";

			var textWidth = faceCanvasCtx.measureText(facename).width;

			nameRectBound[id] = {
				x: boxLeft + boxWidth / 2 - textWidth / 2 - 6,
				y: boxTop + boxHeight + 12,
				width: textWidth + 12,
				height: 24,
			};

			faceCanvasCtx.fillStyle = "rgba(0, 0, 0, 0.75)";
			faceCanvasCtx.roundRect(
				nameRectBound[id].x,
				nameRectBound[id].y,
				nameRectBound[id].width,
				nameRectBound[id].height,
				6,
			);
			faceCanvasCtx.fill();

			faceCanvasCtx.beginPath();
			faceCanvasCtx.moveTo(boxLeft + boxWidth / 2 + 6, boxTop + boxHeight + 12);
			faceCanvasCtx.lineTo(boxLeft + boxWidth / 2, boxTop + boxHeight + 2);
			faceCanvasCtx.lineTo(boxLeft + boxWidth / 2 - 6, boxTop + boxHeight + 12);
			faceCanvasCtx.closePath();
			faceCanvasCtx.fill();

			faceCanvasCtx.fillStyle = "white";
			faceCanvasCtx.fillText(
				facename,
				boxLeft + boxWidth / 2,
				boxTop + boxHeight + 30,
			);

			faceCanvas.style.opacity = 1;
		});

		function isNameRect(bound, clientX, clientY) {
			if (!bound || !faceCanvas) return false;
			var rect = faceCanvas.getBoundingClientRect();
			var mouseX = clientX - rect.left;
			var mouseY = clientY - rect.top;

			if (
				mouseX >= bound.x &&
				mouseX <= bound.x + bound.width &&
				mouseY >= bound.y &&
				mouseY <= bound.y + bound.height
			) {
				return true;
			}
			return false;
		}

		if (faceEventsRegistered) {
			faceEventsRegistered = false;
			faceCanvas.addEventListener("click", function (e) {
				Object.keys(nameRectBound).forEach(function (id) {
					if (isNameRect(nameRectBound[id], e.clientX, e.clientY)) {
						var facebtn = $(`.showimagebox[data-faceembeddingid="${id}"]`);
						if (!facebtn.data("facename")) {
							facebtn = $(`.face-assign-btn[data-faceembeddingid="${id}"]`);
						}
						facebtn.emDialog();
					}
				});
			});

			faceCanvas.addEventListener("mousemove", function (e) {
				var hover = false;
				Object.keys(nameRectBound).forEach(function (id) {
					if (isNameRect(nameRectBound[id], e.clientX, e.clientY)) {
						hover = true;
					}
				});

				faceCanvas.style.cursor = hover ? "pointer" : "default";
			});
		}
	}

	function seekToVJS(vjs, sec) {
		var vjsPlayer = videojs.getPlayer(vjs);
		if (!vjsPlayer) return;
		vjsPlayer.play();
		vjsPlayer.currentTime(parseFloat(sec));
		vjsPlayer.pause();
	}

	var faceTO;

	lQuery(".showimagebox").livequery("mouseover", function () {
		if (faceTO) clearTimeout(faceTO);
		var link = $(this);
		var mediaplayer = link.data("showboxtarget");
		var showboxtarget = $("#" + mediaplayer);
		if (showboxtarget) {
			var image = showboxtarget.find(".imagethumb");
			if (image.length > 0) {
				paintFaceBoxes(image, [link.data()]);
			} else {
				var vjs = link.data("vjs");
				var seekto = link.data("seekto");
				if (vjs && seekto !== undefined) {
					seekToVJS(vjs, seekto);
				}
			}
		}
	});

	lQuery(".showimagebox").livequery("mouseleave", function () {
		if (faceTO) clearTimeout(faceTO);
		$(".face-canvas").css("opacity", 0);
	});

	lQuery("#mediaplayer .imagethumbholder").livequery("mouseover", function (e) {
		if ($(e.target).is(".imagethumb") || $(e.target).is(".face-canvas")) {
			if (faceTO) clearTimeout(faceTO);
			var data = [];
			$(".showimagebox").each(function () {
				data.push($(this).data());
			});

			var thumbimage = $("#mediaplayer").find(".imagethumb");
			if (thumbimage.length > 0) {
				paintFaceBoxes(thumbimage, data);
			}
		}
	});

	lQuery("#mediaplayer .imagethumbholder").livequery("mouseleave", function () {
		if (faceTO) clearTimeout(faceTO);
		$(".face-canvas").css("opacity", 0);
	});

	lQuery("#mediaplayer .imagethumbholder").livequery("mousemove", function (e) {
		if (!$(e.target).is(".face-canvas")) {
			if (faceTO) clearTimeout(faceTO);
			$(".face-canvas").css("opacity", 0);
		}
	});

	lQuery(".facepf").livequery(function () {
		var [boxLeft, boxTop, boxWidth, boxHeight] = $(this).data("location");
		if (boxLeft < 0) {
			boxLeft = 0;
		}

		if (boxTop < 0) {
			boxTop = 0;
		}

		var thumbHeight = 80;
		var thumbWidth = 80 * (boxWidth / boxHeight);
		$(this).width(thumbWidth);

		var width = $(this).data("originalwidth");
		var height = $(this).data("originalheight");

		var scale = thumbHeight / boxHeight;

		var w = Math.ceil(width * scale);
		var h = Math.ceil(height * scale);
		var x = Math.ceil(boxLeft * scale);
		var y = Math.ceil(boxTop * scale);

		$(this).css({
			backgroundSize: `${w}px ${h}px`,
			backgroundPosition: `-${x}px -${y}px`,
		});
	});

	var manualCanvas = null;

	function positionManualAddBtn(coords) {
		$(".manual-canvas div.controls").css({
			width: coords.width + "px",
			top: coords.top + coords.height + "px",
			left: coords.left + coords.width + "px",
		});
	}
	var addicon =
		"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='32' height='32' viewBox='0 0 16 16'%3E%3Cpath fill='%23eeeeee' d='M0 2a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2z'/%3E%3Cpath  fill='%2328a745' d='M2 0a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V2a2 2 0 0 0-2-2zm6.5 4.5v3h3a.5.5 0 0 1 0 1h-3v3a.5.5 0 0 1-1 0v-3h-3a.5.5 0 0 1 0-1h3v-3a.5.5 0 0 1 1 0'/%3E%3C/svg%3E";
	cancelicon =
		"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='32' height='32' fill='%23eeeeee' viewBox='0 0 16 16'%3E%3Cpath d='M4.646 4.646a.5.5 0 0 1 .708 0L8 7.293l2.646-2.647a.5.5 0 0 1 .708.708L8.707 8l2.647 2.646a.5.5 0 0 1-.708.708L8 8.707l-2.646 2.647a.5.5 0 0 1-.708-.708L7.293 8 4.646 5.354a.5.5 0 0 1 0-.708'/%3E%3C/svg%3E";
	function renderIcn(icn) {
		var icon = document.createElement("img");
		icon.src = icn === "add" ? addicon : cancelicon;
		const size = 32;
		return function (ctx, left, top, _, fabricObject) {
			ctx.save();
			ctx.translate(left, top);
			ctx.drawImage(icon, -size / 2, -size / 2, size, size);
			ctx.restore();
		};
	}
	function getAddControl() {
		return new fabric.Control({
			x: 0,
			y: 0.5,
			offsetY: 32,
			offsetX: -32,
			cursorStyle: "pointer",
			mouseUpHandler: () => {
				$("#addManually").trigger("click");
			},
			render: renderIcn("add"),
		});
	}
	function getCancelControl() {
		return new fabric.Control({
			x: 0,
			y: 0.5,
			offsetY: 32,
			offsetX: 32,
			cursorStyle: "pointer",
			mouseDownHandler: () => {
				if (!manualCanvas) return;
				manualCanvas.dispose();
				manualCanvas = null;
				$(".manual-canvas").remove();
			},
			render: renderIcn("cancel", "#ffffff"),
		});
	}
	var controls = fabric.controlsUtils.createObjectDefaultControls();
	delete controls.mt;
	delete controls.ml;
	delete controls.mr;
	delete controls.mb;
	fabric.InteractiveFabricObject.ownDefaults.controls = {
		...controls,
		addControl: getAddControl(),
		cancelControl: getCancelControl(),
	};
	fabric.Canvas.prototype.getAbsoluteCoords = function (object) {
		return {
			left: object.left + this._offset.left,
			top: object.top + this._offset.top,
			width: object.getScaledWidth(),
			height: object.getScaledHeight(),
		};
	};
	fabric.Object.prototype.transparentCorners = false;
	fabric.Object.prototype.originX = "center";
	fabric.Object.prototype.originY = "center";
	lQuery(".manual-fp").livequery("click", function (e) {
		e.preventDefault();
		e.stopPropagation();
		if (manualCanvas) {
			manualCanvas.dispose();
			manualCanvas = null;
		}
		var thumbholder = $("#mediaplayer").find(".imagethumbholder");
		var canvasContainer = thumbholder.find(".manual-canvas");
		if (canvasContainer.length) {
			canvasContainer.remove();
		}
		canvasContainer = $(`<div class="manual-canvas">
      <div class="controls">
        <button id="addManually">Add</button>
        <button id="cancelAddManually">Cancel</button>
      </div>
    </div>`);
		var canvas = $("<canvas></canvas>");
		canvasContainer.append(canvas);
		thumbholder.append(canvasContainer);

		manualCanvas = new fabric.Canvas(canvas[0]);
		var thumb = thumbholder.find("img");
		var width = thumb.width();
		var height = thumb.height();

		manualCanvas.setWidth(width);
		manualCanvas.setHeight(height);
		manualCanvas.selection = false;

		var rectWidth = Math.max(width * 0.15, 150);
		var manualRect = new fabric.Rect({
			left: width / 2 - rectWidth / 2,
			top: height / 2 - rectWidth / 2,
			width: rectWidth,
			height: rectWidth,
			fill: "rgba(160,32,240,0.1)",
			id: "manualRect",
		});
		manualRect.setControlVisible("mtr", false);

		manualCanvas.add(manualRect);

		setTimeout(function () {
			manualCanvas.setActiveObject(manualRect);
			manualCanvas.renderAll();
			manualRect.on("moving", function () {
				if (manualRect.top < 0) {
					manualRect.top = 0;
				}
				if (manualRect.left < 0) {
					manualRect.left = 0;
				}
				if (
					manualRect.top + manualRect.getScaledHeight() >
					manualCanvas.height
				) {
					manualRect.top = manualCanvas.height - manualRect.getScaledHeight();
				}
				if (
					manualRect.left + manualRect.getScaledWidth() >
					manualCanvas.width
				) {
					manualRect.left = manualCanvas.width - manualRect.getScaledWidth();
				}
				manualCanvas.renderAll();
				positionManualAddBtn(manualCanvas.getAbsoluteCoords(manualRect));
			});

			manualRect.on("scaling", function () {
				bound = manualRect.getBoundingRect();
				if (bound.width < 50) {
					manualRect.scaleX = 1;
					manualRect.width = 50;
				}
				if (manualRect.getScaledHeight() < 50) {
					manualRect.scaleY = 1;
					manualRect.height = 50;
				}
				if (
					manualRect.top + manualRect.getScaledHeight() >
					manualCanvas.height
				) {
					manualRect.height = manualCanvas.height - manualRect.top;
				}
				if (
					manualRect.left + manualRect.getScaledWidth() >
					manualCanvas.width
				) {
					manualRect.width = manualCanvas.width - manualRect.left;
				}
				manualCanvas.renderAll();
				positionManualAddBtn(manualCanvas.getAbsoluteCoords(manualRect));
			});

			positionManualAddBtn(manualCanvas.getAbsoluteCoords(manualRect));
		});

		manualCanvas.on("selection:cleared", function () {
			setTimeout(function () {
				manualCanvas.setActiveObject(manualRect);
				manualCanvas.requestRenderAll();
			});
		});
	});
	lQuery("#addManually").livequery("click", function (e) {
		e.preventDefault();
		e.stopPropagation();
		var btn = $(this);
		var manualRect = manualCanvas.getActiveObject();
		var location = manualRect.getBoundingRect();
		console.log(location);
		btn.data("boxlocation", JSON.stringify(location));

		var faceprofileedithome = $(this)
			.closest("#mediaplayer")
			.data("faceprofileedithome");

		btn.data("url", faceprofileedithome + "/addmanualfaceprofile.html");
		btn.data("includeeditcontext", true);
		btn.data("thumbwidth", manualCanvas.width);
		btn.data("targetdiv", "main-media-container");
		btn.data("toastmessage", "Adding manual face profile");
		btn.data("toastsuccess", "Face profile added");
		btn.data("toasterror", "Error adding face profile");

		btn.runAjax();

		manualCanvas.dispose();
		manualCanvas = null;
		$(".manual-canvas").remove();
	});

	lQuery("select.addremovecolumns").livequery("change", function () {
		var selectedval = $(this).val();
		var resultsdiv = $(this).data("targetdiv");

		if (resultsdiv) {
			resultsdiv = $("#" + resultsdiv);
		} else {
			resultsdiv = $(this).closest("#resultsdiv");
		}

		var options = resultsdiv.data();
		var searchhome = resultsdiv.data("searchhome");
		$.get(
			searchhome +
				"/addremovecolumns.html?oemaxlevel=1&editheader=true&addcolumn=" +
				selectedval,
			options,
			function (data) {
				resultsdiv.html(data);
			},
		);
	});

	lQuery("th.sortable").livequery("click", function () {
		//.emselectable ?
		var column = $(this);
		var id = column.data("sortby");

		var resultsdiv = column.closest(".resultsdiv");
		if (resultsdiv.length <= 0) {
			return;
		}
		var searchhome = resultsdiv.data("searchhome");

		resultsdiv.data("url", searchhome + "/columnsort.html");
		resultsdiv.data("includeeditcontext", true);

		if (column.hasClass("currentsort")) {
			if (column.hasClass("up")) {
				resultsdiv.data("sortby", id + "Down");
			} else {
				resultsdiv.data("sortby", id + "Up");
			}
		} else {
			$("th.sortable").removeClass("currentsort");
			column.addClass("currentsort");
			resultsdiv.data("sortby", id + "Down");
		}
		resultsdiv.runAjax();
	});

	var hash = window.location.hash;
	//var hidemediaviewer = $("body").data("hidemediaviewer");

	if (
		hash &&
		hash.startsWith("#asset-") &&
		//!hidemediaviewer &&
		!$("#main-media-viewer").length
	) {
		var assetid = hash.substring(7, hash.length);
		if (assetid) {
			showAsset($("#showasset"), assetid);
		}
	}

	// TODO: remove this. using ajax Used for modules
	togglehits = function (action) {
		var data = $("#resultsdiv").data();
		data.oemaxlevel = 1;
		data.action = action;

		$.get(componenthome + "/moduleresults/selections/togglepage.html", data);
		if (action == "all" || action == "page") {
			$(".moduleselectionbox").attr("checked", "checked");
		} else {
			$(".moduleselectionbox").removeAttr("checked");
		}
		return false;
	};

	function updateentities(element) {
		// get form fields as data
		var data = $(element)
			.serializeArray()
			.reduce(function (obj, item) {
				obj[item.name] = item.value;
				return obj;
			}, {});
		//or get data from element (<a>)
		if (data.constructor === Object && Object.keys(data).length === 0) {
			data = element.data();
		}
		if (data.id && data.searchtype) {
			var entitycontainerclass = "entity" + data.searchtype + data.id;
			$("." + entitycontainerclass).each(function () {
				$(this).trigger("reload");
			});
		}

		$(window).trigger("ajaxautoreload", {
			eventtype: "entitysave",
			moduleid: data.searchtype,
		});
	}

	lQuery(".entitycontainer").livequery(function (e) {
		// debugger;
		var entity = $(this);
		entity.on("reload", function (e) {
			var entityparent = entity.closest(".entitiescontainer");
			var entityreloadurl = entityparent.data("entityrenderurl");
			if (entityreloadurl != null) {
				var options = {};
				var targetdiv = entity.closest(".emgridcell");
				options = entity.data();
				$.ajax({
					url: entityreloadurl,
					data: options,
					success: function (data) {
						targetdiv.replaceWith(data);
						$(window).trigger("resize");
					},
				});
			}
		});
	});

	lQuery("div.assetpreview").livequery("click", function (e) {
		e.preventDefault();
		$(".assettabnav").removeClass("tabselected");
		$(this).closest(".assettabnav").addClass("tabselected");
		var div = $("#main-media-viewer");
		var assetid = div.data("assetid");
		showAsset($(this), assetid);
		saveProfileProperty("assetopentab", "viewpreview", function () {});
	});

	lQuery(".auto-remove").livequery("click", function () {
		var catid = $(this).data("categoryid");
		if (catid) {
			$("#auto-" + catid)
				.parent()
				.remove();
		}
	});

	//FUSE Library
	var Fuse;
	eval(
		`function I(e){return Array.isArray?Array.isArray(e):ft(e)==="[object Array]"}var Mt=1/0;function yt(e){if(typeof e=="string")return e;let t=e+"";return t=="0"&&1/e==-Mt?"-0":t}function _t(e){return e==null?"":yt(e)}function E(e){return typeof e=="string"}function lt(e){return typeof e=="number"}function Et(e){return e===!0||e===!1||wt(e)&&ft(e)=="[object Boolean]"}function ut(e){return typeof e=="object"}function wt(e){return ut(e)&&e!==null}function M(e){return e!=null}function H(e){return!e.trim().length}function ft(e){return e==null?e===void 0?"[object Undefined]":"[object Null]":Object.prototype.toString.call(e)}var At="Incorrect 'index' type",It=e=>"Invalid value for key "+e,St=e=>"Pattern length exceeds max of "+e+".",Lt=e=>"Missing "+e+" property in key",xt=e=>"Property 'weight' in key '"+e+"' must be a positive integer",it=Object.prototype.hasOwnProperty,U=class{constructor(t){this._keys=[],this._keyMap={};let s=0;t.forEach(n=>{let r=dt(n);this._keys.push(r),this._keyMap[r.id]=r,s+=r.weight}),this._keys.forEach(n=>{n.weight/=s})}get(t){return this._keyMap[t]}keys(){return this._keys}toJSON(){return JSON.stringify(this._keys)}};function dt(e){let t=null,s=null,n=null,r=1,i=null;if(E(e)||I(e))n=e,t=ct(e),s=V(e);else{if(!it.call(e,"name"))throw new Error(Lt("name"));let c=e.name;if(n=c,it.call(e,"weight")&&(r=e.weight,r<=0))throw new Error(xt(c));t=ct(c),s=V(c),i=e.getFn}return{path:t,id:s,weight:r,src:n,getFn:i}}function ct(e){return I(e)?e:e.split(".")}function V(e){return I(e)?e.join("."):e}function Rt(e,t){let s=[],n=!1,r=(i,c,o)=>{if(M(i))if(!c[o])s.push(i);else{let h=c[o],l=i[h];if(!M(l))return;if(o===c.length-1&&(E(l)||lt(l)||Et(l)))s.push(_t(l));else if(I(l)){n=!0;for(let a=0,f=l.length;a<f;a+=1)r(l[a],c,o+1)}else c.length&&r(l,c,o+1)}};return r(e,E(t)?t.split("."):t,0),n?s:s[0]}var bt={includeMatches:!1,findAllMatches:!1,minMatchCharLength:1},Nt={isCaseSensitive:!1,includeScore:!1,keys:[],shouldSort:!0,sortFn:(e,t)=>e.score===t.score?e.idx<t.idx?-1:1:e.score<t.score?-1:1},kt={location:0,threshold:.6,distance:100},Ot={useExtendedSearch:!1,getFn:Rt,ignoreLocation:!1,ignoreFieldNorm:!1,fieldNormWeight:1},u={...Nt,...bt,...kt,...Ot},_dt=/[^ ]+/g;function Ct(e=1,t=3){let s=new Map,n=Math.pow(10,t);return{get(r){let i=r.match(_dt).length;if(s.has(i))return s.get(i);let c=1/Math.pow(i,.5*e),o=parseFloat(Math.round(c*n)/n);return s.set(i,o),o},clear(){s.clear()}}}var _d=class{constructor({getFn:t=u.getFn,fieldNormWeight:s=u.fieldNormWeight}={}){this.norm=Ct(s,3),this.getFn=t,this.isCreated=!1,this.setIndexRecords()}setSources(t=[]){this.docs=t}setIndexRecords(t=[]){this.records=t}setKeys(t=[]){this.keys=t,this._keysMap={},t.forEach((s,n)=>{this._keysMap[s.id]=n})}create(){this.isCreated||!this.docs.length||(this.isCreated=!0,E(this.docs[0])?this.docs.forEach((t,s)=>{this._addString(t,s)}):this.docs.forEach((t,s)=>{this._addObject(t,s)}),this.norm.clear())}add(t){let s=this.size();E(t)?this._addString(t,s):this._addObject(t,s)}removeAt(t){this.records.splice(t,1);for(let s=t,n=this.size();s<n;s+=1)this.records[s].i-=1}getValueForItemAtKeyId(t,s){return t[this._keysMap[s]]}size(){return this.records.length}_addString(t,s){if(!M(t)||H(t))return;let n={v:t,i:s,n:this.norm.get(t)};this.records.push(n)}_addObject(t,s){let n={i:s,_d:{}};this.keys.forEach((r,i)=>{let c=r.getFn?r.getFn(t):this.getFn(t,r.path);if(M(c)){if(I(c)){let o=[],h=[{nestedArrIndex:-1,value:c}];for(;h.length;){let{nestedArrIndex:l,value:a}=h.pop();if(M(a))if(E(a)&&!H(a)){let f={v:a,i:l,n:this.norm.get(a)};o.push(f)}else I(a)&&a.forEach((f,d)=>{h.push({nestedArrIndex:d,value:f})})}n._d[i]=o}else if(E(c)&&!H(c)){let o={v:c,n:this.norm.get(c)};n._d[i]=o}}}),this.records.push(n)}toJSON(){return{keys:this.keys,records:this.records}}};function gt(e,t,{getFn:s=u.getFn,fieldNormWeight:n=u.fieldNormWeight}={}){let r=new _d({getFn:s,fieldNormWeight:n});return r.setKeys(e.map(dt)),r.setSources(t),r.create(),r}function Tt(e,{getFn:t=u.getFn,fieldNormWeight:s=u.fieldNormWeight}={}){let{keys:n,records:r}=e,i=new _d({getFn:t,fieldNormWeight:s});return i.setKeys(n),i.setIndexRecords(r),i}function v(e,{errors:t=0,currentLocation:s=0,expectedLocation:n=0,distance:r=u.distance,ignoreLocation:i=u.ignoreLocation}={}){let c=t/e.length;if(i)return c;let o=Math.abs(n-s);return r?c+o/r:o?1:c}function vt(e=[],t=u.minMatchCharLength){let s=[],n=-1,r=-1,i=0;for(let c=e.length;i<c;i+=1){let o=e[i];o&&n===-1?n=i:!o&&n!==-1&&(r=i-1,r-n+1>=t&&s.push([n,r]),n=-1)}return e[i-1]&&i-n>=t&&s.push([n,i-1]),s}var N=32;function Ft(e,t,s,{location:n=u.location,distance:r=u.distance,threshold:i=u.threshold,findAllMatches:c=u.findAllMatches,minMatchCharLength:o=u.minMatchCharLength,includeMatches:h=u.includeMatches,ignoreLocation:l=u.ignoreLocation}={}){if(t.length>N)throw new Error(St(N));let a=t.length,f=e.length,d=Math.max(0,Math.min(n,f)),g=i,p=d,m=o>1||h,R=m?Array(f):[],A;for(;(A=e.indexOf(t,p))>-1;){let y=v(t,{currentLocation:A,expectedLocation:d,distance:r,ignoreLocation:l});if(g=Math.min(y,g),p=A+a,m){let L=0;for(;L<a;)R[A+L]=1,L+=1}}p=-1;let k=[],b=1,C=a+f,mt=1<<a-1;for(let y=0;y<a;y+=1){let L=0,x=C;for(;L<x;)v(t,{errors:y,currentLocation:d+x,expectedLocation:d,distance:r,ignoreLocation:l})<=g?L=x:C=x,x=Math.floor((C-L)/2+L);C=x;let nt=Math.max(1,d-x+1),W=c?f:Math.min(d+x,f)+a,O=Array(W+2);O[W+1]=(1<<y)-1;for(let _=W;_>=nt;_-=1){let T=_-1,rt=s[e.charAt(T)];if(m&&(R[T]=+!!rt),O[_]=(O[_+1]<<1|1)&rt,y&&(O[_]|=(k[_+1]|k[_])<<1|1|k[_+1]),O[_]&mt&&(b=v(t,{errors:y,currentLocation:T,expectedLocation:d,distance:r,ignoreLocation:l}),b<=g)){if(g=b,p=T,p<=d)break;nt=Math.max(1,2*d-p)}}if(v(t,{errors:y+1,currentLocation:d,expectedLocation:d,distance:r,ignoreLocation:l})>g)break;k=O}let K={isMatch:p>=0,score:Math.max(.001,b)};if(m){let y=vt(R,o);y.length?h&&(K.indices=y):K.isMatch=!1}return K}function jt(e){let t={};for(let s=0,n=e.length;s<n;s+=1){let r=e.charAt(s);t[r]=(t[r]||0)|1<<n-s-1}return t}var F=class{constructor(t,{location:s=u.location,threshold:n=u.threshold,distance:r=u.distance,includeMatches:i=u.includeMatches,findAllMatches:c=u.findAllMatches,minMatchCharLength:o=u.minMatchCharLength,isCaseSensitive:h=u.isCaseSensitive,ignoreLocation:l=u.ignoreLocation}={}){if(this.options={location:s,threshold:n,distance:r,includeMatches:i,findAllMatches:c,minMatchCharLength:o,isCaseSensitive:h,ignoreLocation:l},this.pattern=h?t:t.toLowerCase(),this.chunks=[],!this.pattern.length)return;let a=(d,g)=>{this.chunks.push({pattern:d,alphabet:jt(d),startIndex:g})},f=this.pattern.length;if(f>N){let d=0,g=f%N,p=f-g;for(;d<p;)a(this.pattern.substr(d,N),d),d+=N;if(g){let m=f-N;a(this.pattern.substr(m),m)}}else a(this.pattern,0)}searchIn(t){let{isCaseSensitive:s,includeMatches:n}=this.options;if(s||(t=t.toLowerCase()),this.pattern===t){let p={isMatch:!0,score:0};return n&&(p.indices=[[0,t.length-1]]),p}let{location:r,distance:i,threshold:c,findAllMatches:o,minMatchCharLength:h,ignoreLocation:l}=this.options,a=[],f=0,d=!1;this.chunks.forEach(({pattern:p,alphabet:m,startIndex:R})=>{let{isMatch:A,score:k,indices:b}=Ft(t,p,m,{location:r+R,distance:i,threshold:c,findAllMatches:o,minMatchCharLength:h,includeMatches:n,ignoreLocation:l});A&&(d=!0),f+=k,A&&b&&(a=[...a,...b])});let g={isMatch:d,score:d?f/this.chunks.length:1};return d&&n&&(g.indices=a),g}},w=class{constructor(t){this.pattern=t}static isMultiMatch(t){return ot(t,this.multiRegex)}static isSingleMatch(t){return ot(t,this.singleRegex)}search(){}};function ot(e,t){let s=e.match(t);return s?s[1]:null}var B=class extends w{constructor(t){super(t)}static get type(){return"exact"}static get multiRegex(){return/^="(.*)"$/}static get singleRegex(){return/^=(.*)$/}search(t){let s=t===this.pattern;return{isMatch:s,score:s?0:1,indices:[0,this.pattern.length-1]}}},Y=class extends w{constructor(t){super(t)}static get type(){return"inverse-exact"}static get multiRegex(){return/^!"(.*)"$/}static get singleRegex(){return/^!(.*)$/}search(t){let n=t.indexOf(this.pattern)===-1;return{isMatch:n,score:n?0:1,indices:[0,t.length-1]}}},G=class extends w{constructor(t){super(t)}static get type(){return"prefix-exact"}static get multiRegex(){return/^\^"(.*)"$/}static get singleRegex(){return/^\^(.*)$/}search(t){let s=t.startsWith(this.pattern);return{isMatch:s,score:s?0:1,indices:[0,this.pattern.length-1]}}},z=class extends w{constructor(t){super(t)}static get type(){return"inverse-prefix-exact"}static get multiRegex(){return/^!\^"(.*)"$/}static get singleRegex(){return/^!\^(.*)$/}search(t){let s=!t.startsWith(this.pattern);return{isMatch:s,score:s?0:1,indices:[0,t.length-1]}}},Q=class extends w{constructor(t){super(t)}static get type(){return"suffix-exact"}static get multiRegex(){return/^"(.*)"\$$/}static get singleRegex(){return/^(.*)\$$/}search(t){let s=t.endsWith(this.pattern);return{isMatch:s,score:s?0:1,indices:[t.length-this.pattern.length,t.length-1]}}},X=class extends w{constructor(t){super(t)}static get type(){return"inverse-suffix-exact"}static get multiRegex(){return/^!"(.*)"\$$/}static get singleRegex(){return/^!(.*)\$$/}search(t){let s=!t.endsWith(this.pattern);return{isMatch:s,score:s?0:1,indices:[0,t.length-1]}}},j=class extends w{constructor(t,{location:s=u.location,threshold:n=u.threshold,distance:r=u.distance,includeMatches:i=u.includeMatches,findAllMatches:c=u.findAllMatches,minMatchCharLength:o=u.minMatchCharLength,isCaseSensitive:h=u.isCaseSensitive,ignoreLocation:l=u.ignoreLocation}={}){super(t),this._bitapSearch=new F(t,{location:s,threshold:n,distance:r,includeMatches:i,findAllMatches:c,minMatchCharLength:o,isCaseSensitive:h,ignoreLocation:l})}static get type(){return"fuzzy"}static get multiRegex(){return/^"(.*)"$/}static get singleRegex(){return/^(.*)$/}search(t){return this._bitapSearch.searchIn(t)}},P=class extends w{constructor(t){super(t)}static get type(){return"include"}static get multiRegex(){return/^'"(.*)"$/}static get singleRegex(){return/^'(.*)$/}search(t){let s=0,n,r=[],i=this.pattern.length;for(;(n=t.indexOf(this.pattern,s))>-1;)s=n+i,r.push([n,s-1]);let c=!!r.length;return{isMatch:c,score:c?0:1,indices:r}}},J=[B,P,G,z,X,Q,Y,j],ht=J.length,Pt=/ +(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)/,Dt="|";function Kt(e,t={}){return e.split(Dt).map(s=>{let n=s.trim().split(Pt).filter(i=>i&&!!i.trim()),r=[];for(let i=0,c=n.length;i<c;i+=1){let o=n[i],h=!1,l=-1;for(;!h&&++l<ht;){let a=J[l],f=a.isMultiMatch(o);f&&(r.push(new a(f,t)),h=!0)}if(!h)for(l=-1;++l<ht;){let a=J[l],f=a.isSingleMatch(o);if(f){r.push(new a(f,t));break}}}return r})}var Wt=new Set([j.type,P.type]),Z=class{constructor(t,{isCaseSensitive:s=u.isCaseSensitive,includeMatches:n=u.includeMatches,minMatchCharLength:r=u.minMatchCharLength,ignoreLocation:i=u.ignoreLocation,findAllMatches:c=u.findAllMatches,location:o=u.location,threshold:h=u.threshold,distance:l=u.distance}={}){this.query=null,this.options={isCaseSensitive:s,includeMatches:n,minMatchCharLength:r,findAllMatches:c,ignoreLocation:i,location:o,threshold:h,distance:l},this.pattern=s?t:t.toLowerCase(),this.query=Kt(this.pattern,this.options)}static condition(t,s){return s.useExtendedSearch}searchIn(t){let s=this.query;if(!s)return{isMatch:!1,score:1};let{includeMatches:n,isCaseSensitive:r}=this.options;t=r?t:t.toLowerCase();let i=0,c=[],o=0;for(let h=0,l=s.length;h<l;h+=1){let a=s[h];c.length=0,i=0;for(let f=0,d=a.length;f<d;f+=1){let g=a[f],{isMatch:p,indices:m,score:R}=g.search(t);if(p){if(i+=1,o+=R,n){let A=g.constructor.type;Wt.has(A)?c=[...c,...m]:c.push(m)}}else{o=0,i=0,c.length=0;break}}if(i){let f={isMatch:!0,score:o/i};return n&&(f.indices=c),f}}return{isMatch:!1,score:1}}},q=[];function Ht(...e){q.push(...e)}function tt(e,t){for(let s=0,n=q.length;s<n;s+=1){let r=q[s];if(r.condition(e,t))return new r(e,t)}return new F(e,t)}var D={AND:"$and",OR:"$or"},et={PATH:"$path",PATTERN:"$val"},st=e=>!!(e[D.AND]||e[D.OR]),Ut=e=>!!e[et.PATH],Vt=e=>!I(e)&&ut(e)&&!st(e),at=e=>({[D.AND]:Object.keys(e).map(t=>({[t]:e[t]}))});function pt(e,t,{auto:s=!0}={}){let n=r=>{let i=Object.keys(r),c=Ut(r);if(!c&&i.length>1&&!st(r))return n(at(r));if(Vt(r)){let h=c?r[et.PATH]:i[0],l=c?r[et.PATTERN]:r[h];if(!E(l))throw new Error(It(h));let a={keyId:V(h),pattern:l};return s&&(a.searcher=tt(l,t)),a}let o={children:[],operator:i[0]};return i.forEach(h=>{let l=r[h];I(l)&&l.forEach(a=>{o.children.push(n(a))})}),o};return st(e)||(e=at(e)),n(e)}function Bt(e,{ignoreFieldNorm:t=u.ignoreFieldNorm}){e.forEach(s=>{let n=1;s.matches.forEach(({key:r,norm:i,score:c})=>{let o=r?r.weight:null;n*=Math.pow(c===0&&o?Number.EPSILON:c,(o||1)*(t?1:i))}),s.score=n})}function Yt(e,t){let s=e.matches;t.matches=[],M(s)&&s.forEach(n=>{if(!M(n.indices)||!n.indices.length)return;let{indices:r,value:i}=n,c={indices:r,value:i};n.key&&(c.key=n.key.src),n.idx>-1&&(c.refIndex=n.idx),t.matches.push(c)})}function Gt(e,t){t.score=e.score}function zt(e,t,{includeMatches:s=u.includeMatches,includeScore:n=u.includeScore}={}){let r=[];return s&&r.push(Yt),n&&r.push(Gt),e.map(i=>{let{idx:c}=i,o={item:t[c],refIndex:c};return r.length&&r.forEach(h=>{h(i,o)}),o})}Fuse=class{constructor(t,s={},n){this.options={...u,...s},this.options.useExtendedSearch,this._keyStore=new U(this.options.keys),this.setCollection(t,n)}setCollection(t,s){if(this._docs=t,s&&!(s instanceof _d))throw new Error(At);this._myIndex=s||gt(this.options.keys,this._docs,{getFn:this.options.getFn,fieldNormWeight:this.options.fieldNormWeight})}add(t){M(t)&&(this._docs.push(t),this._myIndex.add(t))}remove(t=()=>!1){let s=[];for(let n=0,r=this._docs.length;n<r;n+=1){let i=this._docs[n];t(i,n)&&(this.removeAt(n),n-=1,r-=1,s.push(i))}return s}removeAt(t){this._docs.splice(t,1),this._myIndex.removeAt(t)}getIndex(){return this._myIndex}search(t,{limit:s=-1}={}){let{includeMatches:n,includeScore:r,shouldSort:i,sortFn:c,ignoreFieldNorm:o}=this.options,h=E(t)?E(this._docs[0])?this._searchStringList(t):this._searchObjectList(t):this._searchLogical(t);return Bt(h,{ignoreFieldNorm:o}),i&&h.sort(c),lt(s)&&s>-1&&(h=h.slice(0,s)),zt(h,this._docs,{includeMatches:n,includeScore:r})}_searchStringList(t){let s=tt(t,this.options),{records:n}=this._myIndex,r=[];return n.forEach(({v:i,i:c,n:o})=>{if(!M(i))return;let{isMatch:h,score:l,indices:a}=s.searchIn(i);h&&r.push({item:i,idx:c,matches:[{score:l,value:i,norm:o,indices:a}]})}),r}_searchLogical(t){let s=pt(t,this.options),n=(o,h,l)=>{if(!o.children){let{keyId:f,searcher:d}=o,g=this._findMatches({key:this._keyStore.get(f),value:this._myIndex.getValueForItemAtKeyId(h,f),searcher:d});return g&&g.length?[{idx:l,item:h,matches:g}]:[]}let a=[];for(let f=0,d=o.children.length;f<d;f+=1){let g=o.children[f],p=n(g,h,l);if(p.length)a.push(...p);else if(o.operator===D.AND)return[]}return a},r=this._myIndex.records,i={},c=[];return r.forEach(({_d:o,i:h})=>{if(M(o)){let l=n(s,o,h);l.length&&(i[h]||(i[h]={idx:h,item:o,matches:[]},c.push(i[h])),l.forEach(({matches:a})=>{i[h].matches.push(...a)}))}}),c}_searchObjectList(t){let s=tt(t,this.options),{keys:n,records:r}=this._myIndex,i=[];return r.forEach(({_d:c,i:o})=>{if(!M(c))return;let h=[];n.forEach((l,a)=>{h.push(...this._findMatches({key:l,value:c[a],searcher:s}))}),h.length&&i.push({idx:o,item:c,matches:h})}),i}_findMatches({key:t,value:s,searcher:n}){if(!M(s))return[];let r=[];if(I(s))s.forEach(({v:i,i:c,n:o})=>{if(!M(i))return;let{isMatch:h,score:l,indices:a}=n.searchIn(i);h&&r.push({score:l,key:t,value:i,idx:c,norm:o,indices:a})});else{let{v:i,n:c}=s,{isMatch:o,score:h,indices:l}=n.searchIn(i);o&&r.push({score:h,key:t,value:i,norm:c,indices:l})}return r}};Fuse.createIndex=gt;Fuse.parseIndex=Tt;Fuse.config=u;Fuse.parseQuery=pt;Ht(Z);`,
	);

	lQuery(".icons-picker").livequery(function () {
		var iconListContainer = document.querySelector("#icons-list");
		var iconElements = iconListContainer.children;
		var iconElementList = Array.from(iconElements);
		var iconDataList = iconElementList.map((element) => {
			return {
				name: element.dataset.name,
				categories: element.dataset.categories.split(" "),
				tags: element.dataset.tags.split(" "),
			};
		});

		var fuse = new Fuse(iconDataList, {
			ignoreLocation: true,
			useExtendedSearch: true,
			shouldSort: false,
			keys: ["name", "categories", "tags"],
			threshold: 0,
		});

		function search(searchTerm) {
			var trimmedSearchTerm = searchTerm ? searchTerm.trim() : "";

			iconListContainer.innerHTML = "";
			if (trimmedSearchTerm.length > 0) {
				var searchResult = fuse.search(trimmedSearchTerm);
				var resultElements = searchResult.map(
					(result) => iconElementList[result.refIndex],
				);
				iconListContainer.append(...resultElements);
				if (resultElements.length == 0) {
					iconListContainer.innerHTML =
						"<p class='text-muted my-2'>No results found</p>";
				}
			} else {
				iconListContainer.append(...iconElementList);
			}
		}

		var searchInput = $("#icon-search");
		var timeout;
		searchInput.keydown(function () {
			clearTimeout(timeout);
			timeout = setTimeout(() => {
				search(searchInput.val());
			}, 250);
		});
	});

	lQuery(".forceresize").livequery(function () {
		$(window).trigger("resize");
	});

	updatebasket = function (e) {
		var action = $(this).data("action");
		if (action == "addtocart" || action == "remove") {
			var nextpage = $(this).attr("href");
			var targetDiv = $(this).data("targetdiv");
			if (!targetDiv) {
				targetDiv = $(this).attr("targetdiv");
			}
			targetDiv = targetDiv.replace(/\//g, "\\/");

			$("#" + targetDiv).load(nextpage, function () {
				var url = apphome + "/components/basket/menuitem.html";
				$.ajax({
					xhrFields: {
						withCredentials: true,
					},
					crossDomain: true,
					url: url,
					success: function (data) {
						$("#basket-paint").replaceWith(data);
						if (action == "remove") {
							var checkoutpage = $("#collectionbasket");
							if (checkoutpage.length > 0) {
								window.location.reload();
							}
						}
						customToast("Added to cart!");
					},
				});
			});
		}
		return false;
	};

	updatebasketmediaviewer = function (e) {
		var nextpage = $(this).attr("href");
		var targetDiv = $(this).data("targetdiv");
		var action = $(this).data("action");
		var alerttxt = $(this).data("alerttxt");
		$("#" + targetDiv).load(nextpage, function () {
			$("#basket-paint").load(apphome + "/components/basket/menuitem.html");
			customToast(alerttxt);
		});
		if ($(this).closest(".dropdown-menu").length !== 0) {
			$(this).closest(".dropdown-menu").removeClass("show");
		}
		e.preventDefault();
		return false;
	};
}); // document ready




//Ended: /${applicationid}/components/javascript/emedia/results.js Size: 55.72 KB


/** 
 EnterMediaDB javascriptGenerator : /${applicationid}/components/javascript/emedia/brick.js
/finder/find/_site.xconf
 Modified: Tue May 05 23:52:52 CST 2026 Size: 7.43 KB **/

(function ($) {
	var stopautoscroll = false;
	var gridcurrentpageviewport = 1;
	var gridlastscroll = 0;

	function gridResize(grid) {
		//TODO: Put these on grid.data()
		stopautoscroll = false;
		gridcurrentpageviewport = 1;
		gridlastscroll = 0;

		if (!grid) {
			return;
		}

		if (!grid.is(":visible")) {
			return;
		}
		var fixedheight = grid.data("maxheight");
		if (fixedheight == null || fixedheight.length == 0) {
			fixedheight = 200;
		}
		fixedheight = parseInt(fixedheight);

		var totalheight = fixedheight;
		var rownum = 0;
		var totalavailablew = grid.width();

		// Two loops, one to make rows and one to render cells
		var sofarusedw = 0;
		var sofarusedh = 0;

		var row = new Array();
		var rows = new Array();
		rows.push(row);
		$(grid)
			.find(".masonry-grid-cell")
			.each(function () {
				var cell = $(this);
				var w = cell.data("width");
				var h = cell.data("height");
				w = parseInt(w);
				h = parseInt(h);
				if (w == 0) {
					w = fixedheight;
					h = fixedheight;
				}
				var a = 1;
				if (w >= h) {
					a = w / h;
				} else {
					a = h / w;
				}
				cell.data("aspect", a);
				var neww = Math.floor(a * fixedheight);
				cell.data("targetw", Math.ceil(neww));
				var isover = sofarusedw + neww;
				if (isover > totalavailablew) {
					// Just to make a row
					// Process previously added cell
					var newheight = trimRowToFit(grid, row);
					totalheight = totalheight + newheight + 8;
					row = new Array();
					rows.push(row);
					sofarusedw = 0;
					rownum = rownum + 1;
				}
				sofarusedw = sofarusedw + neww;
				row.push(cell);
				cell.data("rownum", rownum);
			});

		if (row.length > 0) {
			trimRowToFit(grid, row);
			//if( makebox && makebox == true && rownum >= 3)
			{
				grid.css("height", totalheight + "px");
				//grid.css("overflow","hidden");
			}
		}

		$.each(rows, function () {
			var row = $(this);
			trimRowToFit(grid, row);
		});
		checkScroll(grid);
	}

	function trimRowToFit(grid, row) {
		var totalwidthused = 0;
		var targetheight = grid.data("maxheight");
		$.each(row, function () {
			var div = this;
			var usedw = div.data("targetw");
			totalwidthused = totalwidthused + usedw;
		});

		var totalavailablew = grid.width();
		var existingaspect = targetheight / totalwidthused; // Existing aspec ratio
		var overwidth = Math.abs(totalwidthused - totalavailablew);
		var changeheight = existingaspect * overwidth;
		var fixedheight = Math.floor(targetheight + changeheight);

		if (fixedheight > targetheight * 1.7) {
			fixedheight = targetheight;
		}

		var totalwused = 0;
		$.each(row, function () {
			var div = this;
			var image = $("img.imagethumb", div);
			// div.css("line-height",fixedheight + "px");
			div.css("height", fixedheight + "px");
			//image.height(fixedheight);
			image.data("fixedheight", fixedheight);

			var a = div.data("aspect");
			var neww = fixedheight * a;

			neww = Math.floor(neww); // make sure we dont round too high across lots
			// of widths
			div.css("width", neww + "px");
			image.width(neww);
			totalwused = totalwused + neww;
		});

		totalavailablew = grid.width();
		if (totalwused != totalavailablew && fixedheight != targetheight) {
			var toadd = totalavailablew - totalwused;
			var div = row[row.length - 1];
			if (div) {
				var w = div.width();
				w = w + toadd;
				div.css("width", w + "px");
				var image = $("img.imagethumb", div);
				image.width(w);
			}
		}
		return fixedheight;
	}

	function isInViewport(cell) {
		const rect = cell.getBoundingClientRect();
		var isin =
			rect.top >= 0 &&
			rect.left >= 0 &&
			rect.bottom <=
				(window.innerHeight || document.documentElement.clientHeight) &&
			rect.right <= (window.innerWidth || document.documentElement.clientWidth);
		return isin;
	}

	function replaceelement(url, div, options, callback) {
		jQuery.ajax({
			url: url,
			async: false,
			data: options,
			success: function (data) {
				div.replaceWith(data);

				if (callback && typeof callback === "function") {
					//make sure it exists and it is a function
					callback(); //execute it
				}
			},
			xhrFields: {
				withCredentials: true,
			},
			crossDomain: true,
		});
	}

	function gridupdatepositions(grid) {
		var resultsdiv = grid.closest(".resultsdiv");
		if (!resultsdiv) {
			resultsdiv = grid.closest(".resultsdiv");
		}

		var positionsDiv = resultsdiv.find(".resultspositions");

		if (positionsDiv.length > 0) {
			var oldpage = grid.data("currentpagenum");

			$(".masonry-grid-cell", grid).each(function (index, cell) {
				var elementviewport = isInViewport(cell);
				if (elementviewport) {
					var pagenum = $(cell).data("pagenum");
					if (pagenum != oldpage) {
						grid.data("currentpagenum", pagenum);
						positionsDiv.data("currentpagenum", pagenum);
						var url = positionsDiv.data("url");
						var options = positionsDiv.data();
						replaceelement(url, positionsDiv, options);
					}
					return false;
				}
			});
		}
	}

	function checkScroll(grid) {
		if (!grid) {
			return;
		}
		if (grid.data("singlepage") == true) {
			return;
		}

		var resultsdiv = $(grid).closest(".resultsdiv");
		if (stopautoscroll) {
			// ignore scrolls
			// if (typeof getOverlay === "function" && getOverlay().is(":visible")) {
			// 	var lastscroll = getOverlay().data("lastscroll");

			// 	if (Math.abs(lastscroll - currentscroll) > 50) {
			// 		$(window).scrollTop(lastscroll);
			// 	}
			// }
			return;
		}

		var gridcells = $(".masonry-grid-cell", resultsdiv);
		if (gridcells.length == 0) {
			return; //No results?
		}

		gridupdatepositions(grid);

		var page = parseInt(resultsdiv.data("pagenum"));
		if (isNaN(page)) {
			page = 1;
		}

		var total = parseInt(resultsdiv.data("totalpages"));
		if (isNaN(total)) {
			total = 1;
		}
		if (page == total) {
			return;
		}

		var lastcell = gridcells.last().get(0);
		if (!isInViewport(lastcell)) {
			return; //not yet at bottom
		}

		stopautoscroll = true;
		page = page + 1;
		resultsdiv.data("pagenum", page);

		var link = grid.data("stackedviewpath");
		if (link == undefined) {
			console.log("No stackedviewpath defined");
			return;
		}

		var params = resultsdiv.cleandata();
		params.page = page;
		params.oemaxlevel = 1;

		console.log("Loading page: #" + page + " - " + link);

		$.ajax({
			url: link,
			xhrFields: {
				withCredentials: true,
			},
			cache: false,
			data: params,
			success: function (data) {
				var jdata = $(data);
				var code = $(".masonry-grid", jdata).html();
				grid.append(code);
				gridResize(grid); //was resize event
				$(window).trigger("resultsgenerated", [grid]);

				stopautoscroll = false;
			},
		});
	}

	var methods = {
		init: function (options) {
			//Any details?
			var grid = $(this);
			document.addEventListener("touchmove", function (e) {
				checkScroll(grid);
			});

			gridResize(grid);

			jQuery(window).on("resize", function () {
				gridResize(grid);
			});

			lQuery(".scrollview").livequery("scroll", function () {
				checkScroll(grid);
			});
			$(window).trigger("resultsgenerated", [grid]);
		},
		render: function () {
			gridResize($(this));
		},
	}; //Methods end

	$.fn.brick = function (methodOrOptions) {
		//Generic brick caller
		if (methods[methodOrOptions]) {
			return methods[methodOrOptions].apply(
				this,
				Array.prototype.slice.call(arguments, 1)
			);
		} else if (typeof methodOrOptions === "object" || !methodOrOptions) {
			// Default to "init"
			return methods.init.apply(this, arguments);
		} else {
			$.error(
				"Method " + methodOrOptions + " does not exist on jQuery.tooltip"
			);
		}
	};
})(jQuery);




//Ended: /${applicationid}/components/javascript/emedia/brick.js Size: 7.43 KB


/** 
 EnterMediaDB javascriptGenerator : /${applicationid}/components/javascript/emedia/brickvertical.js
/finder/find/_site.xconf
 Modified: Tue May 05 23:52:52 CST 2026 Size: 7.59 KB **/

(function ($) {
	var stopautoscroll = false;

	function verticalGridResize(grid) {
		//TODO: Put these on grid.data()
		stopautoscroll = false;
		gridcurrentpageviewport = 1;
		gridlastscroll = 0;

		if (!grid) {
			return;
		}

		if (!grid.is(":visible")) {
			return;
		}

		var minwidth = grid.data("minwidth");
		if (minwidth == null || minwidth.length == 0) {
			minwidth = 180;
		}
		var totalavailablew = grid.width() - 15;

		var maxcols = totalavailablew / minwidth; //Ideally
		var eachwidth = 0;

		maxcols = Math.round(maxcols);
		while (eachwidth < minwidth) {
			//Divide evenly
			eachwidth = totalavailablew / maxcols;
			maxcols--;
		}
		maxcols++;

		if (maxcols == 0) {
			maxcols = 1;
		}
		var colheight = {};
		for (let col = 0; col < maxcols; col++) {
			colheight[col] = 0;
		}

		var autosort = true;

		eachwidth -= 8;
		var colwidthpx = totalavailablew / maxcols;
		var colnum = 0;
		var gridminheight = 1;
		grid.css("height", gridminheight + "px");
		$(grid)
			.find(".masonry-grid-cell")
			.each(function () {
				var cell = $(this);
				var embrickcontent = cell.find(".embrickcontent");
				var imgw = embrickcontent.data("imgwidth");
				var imgh = embrickcontent.data("imgheight");
				imgw = parseInt(imgw);
				imgh = parseInt(imgh);
				var a = imgw / imgh;
				var newheight = Math.floor(eachwidth / a);
				if (embrickcontent.hasClass("nothumb")) {
					newheight = Math.max(140, newheight);
				}
				embrickcontent
					.find(".emcategory-thumb")
					.attr("data-height", newheight)
					.height(newheight)
					.css("height", newheight + "px"); //need both height and css("height") cz jquery hates us

				//w = colwidthpx - 8;
				var textcontent = embrickcontent.find(".embricktext");
				if (textcontent.length) {
					embrickcontent.attr(
						"data-textcontent-height",
						textcontent.outerHeight()
					);
					newheight = newheight + textcontent.outerHeight();
				}
				//if (!embrickcontent.data("hasheight")) {
				//	newheight = embrickcontent.height();
				//}

				if (autosort) {
					colnum = shortestColumn(colheight, colnum);
				}
				cell.data("colnum", colnum);
				var runningtotal = colheight[colnum];
				runningtotal = runningtotal + 8;
				var currenth = runningtotal + newheight;
				if (isNaN(currenth)) {
					// debugger;
				}
				colheight[colnum] = currenth;

				cell.css("top", runningtotal + "px");
				cell.width(eachwidth);
				cell.height(newheight);

				var colx = colwidthpx * colnum;
				cell.css("left", colx + "px");
				if (colheight[colnum] > gridminheight) {
					grid.css("height", colheight[colnum] + "px");
					gridminheight = colheight[colnum];
				}
				colnum++;
				if (colnum >= maxcols) {
					colnum = 0;
				}
			});
		checkScroll(grid);
	}

	function shortestColumn(colheight, defaultcolumn) {
		var shortColumn = 0;
		var shortColumnHeight = -1;
		for (let column in Object.keys(colheight)) {
			var onecolheight = colheight[column];
			if (shortColumnHeight == -1 || onecolheight < shortColumnHeight) {
				shortColumnHeight = onecolheight;
				shortColumn = column;
			}
		}
		//	return shortColumn;

		//Only change if its over 50px in diference
		var defaulttop = colheight[defaultcolumn];
		var shortesttop = colheight[shortColumn];
		if (defaulttop - shortesttop > 175) {
			return shortColumn;
		}

		return defaultcolumn;
	}

	function isInViewport(cell) {
		const rect = cell.getBoundingClientRect();
		var top = rect.top;
		top = top - 600;
		var isin =
			top <= (window.innerHeight || document.documentElement.clientHeight);
		return isin;
	}

	function gridupdatepositions(grid) {
		var resultsdiv = grid.closest(".lightboxresults");
		if (resultsdiv.length < 1) {
			resultsdiv = grid.closest(".resultsdiv");
		}

		var positionsDiv = resultsdiv.find(".resultspositions");

		if (positionsDiv.length > 0) {
			var oldpage = grid.data("currentpagenum");

			$(".masonry-grid-cell", grid).each(function (index, cell) {
				var elementviewport = isInViewport(cell);
				if (elementviewport) {
					var pagenum = $(cell).data("pagenum");
					if (pagenum != oldpage) {
						grid.data("currentpagenum", pagenum);
						positionsDiv.data("currentpagenum", pagenum);
						var url = positionsDiv.data("url");
						var options = positionsDiv.data();
						replaceelement(url, positionsDiv, options);
					}
					return false;
				}
			});
		}
	}

	function checkScroll(grid) {
		var currentscroll = $(".scrollview").scrollTop();

		var gridcells = $(".masonry-grid-cell", grid);
		if (gridcells.length == 0) {
			return; //No results?
		}

		//From the top to this height. Set the src
		$(grid)
			.find(".masonry-grid-cell")
			.each(function () {
				var cell = $(this);
				if (isInViewport(cell.get(0))) {
					var image = cell.find("img");
					if (image.prop("src") == undefined || image.prop("src") == "") {
						image.prop("src", image.data("imagesrc"));
						image.show();
					}
				}
			});

		var resultsdiv = grid.closest(".lightboxresults");
		if (resultsdiv.length < 1) {
			resultsdiv = grid.closest(".resultsdiv");
		}

		if (stopautoscroll) {
			// ignore scrolls
			// if (typeof getOverlay === "function" && getOverlay().is(":visible")) {
			// 	var lastscroll = getOverlay().data("lastscroll");

			// 	if (Math.abs(lastscroll - currentscroll) > 50) {
			// 		$(window).scrollTop(lastscroll);
			// 	}
			// }
			return;
		}

		gridupdatepositions(grid);

		var page = parseInt(resultsdiv.data("pagenum"));
		if (isNaN(page)) {
			page = 1;
		}

		var total = parseInt(resultsdiv.data("totalpages"));
		if (isNaN(total)) {
			total = 1;
		}
		if (page == total) {
			return;
		}

		var lastcell = gridcells.last().get(0);
		if (!isInViewport(lastcell)) {
			return; //not yet at bottom
		}

		stopautoscroll = true;
		page = page + 1;
		resultsdiv.data("pagenum", page);

		// if (!stackedviewpath) {
		// 	stackedviewpath = "/brickvertical.html";
		// }

		// var searchhome = resultsdiv.data("searchhome");
		// debugger;
		var link = grid.data("stackedviewpath");
		if (link == undefined) {
			console.log("No stackedviewpath defined");
			return;
		}

		var params = resultsdiv.cleandata();
		params.page = page;
		params.oemaxlevel = 1;

		$.ajax({
			url: link,
			xhrFields: {
				withCredentials: true,
			},
			cache: false,
			data: params,
			success: function (data) {
				var jdata = $(data);
				var code = $(".brickvertical", jdata).html();
				grid.append(code);
				grid.brickvertical("resize");
				$(window).trigger("resultsgenerated", [grid]);
				stopautoscroll = false;
			},
		});
	}

	var methods = {
		init: function (options) {
			var grid = $(this);
			verticalGridResize(grid);
			jQuery(window).on("resize", function () {
				verticalGridResize(grid);
			});

			jQuery(window).on("scroll", function () {
				checkScroll(grid);
			});

			grid.parents().filter(function () {
				var element = jQuery(this);
				if (
					element.css("overflow-y") == "auto" ||
					element.css("overflow") == "auto"
				) {
					element.on("scroll", function () {
						checkScroll(grid);
					});
				}
			});
			grid.removeClass("uninitialized");
			$(window).trigger("resultsgenerated", [grid]);
		},
		resize: function () {
			var grid = $(this);
			verticalGridResize(grid);
			grid.removeClass("uninitialized");
		},
	}; //Methods end

	$.fn.brickvertical = function (methodOrOptions) {
		//Generic brick caller
		if (methods[methodOrOptions]) {
			return methods[methodOrOptions].apply(
				this,
				Array.prototype.slice.call(arguments, 1)
			);
		} else if (typeof methodOrOptions === "object" || !methodOrOptions) {
			// Default to "init"
			return methods.init.apply(this, arguments);
		} else {
			$.error(
				"Method " + methodOrOptions + " does not exist on jQuery.tooltip"
			);
		}
	};
})(jQuery);




//Ended: /${applicationid}/components/javascript/emedia/brickvertical.js Size: 7.59 KB

