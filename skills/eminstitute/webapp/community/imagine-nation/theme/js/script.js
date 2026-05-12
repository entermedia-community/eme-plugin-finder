$(function () {
	lQuery("#homegallery").livequery(function () {
		var $grid = $(this).masonry({
			itemSelector: ".gallery-item",
			columnWidth: 240,
			isAnimated: true,
			isFitWidth: true,
			gutter: 10,
		});

		$grid.imagesLoaded().progress(function () {
			$grid.masonry("layout");
		});
	});

	lQuery("a.ajax").livequery("click", function (e) {
		e.stopPropagation();
		e.preventDefault();
		$(this).runAjax();
	});
	lQuery("a.emdialog").livequery("click", function (e) {
		e.stopPropagation();
		e.preventDefault();
		$(this).emDialog();
		$("#__flair").remove();
	});
	lQuery(".jp-play").livequery("click", function () {
		var player = $(this).closest(".jp-audio").find(".jp-jplayer");
		var url = player.data("url");
		var containerid = player.data("container");

		player.jPlayer({
			ready: function (event) {
				player
					.jPlayer("setMedia", {
						mp3: url,
					})
					.jPlayer("play");
			},
			play: function () {
				// To avoid both jPlayers playing together.
				player.jPlayer("pauseOthers");
			},
			swfPath: $("#application").data("apphome") + "/components/javascript",
			supplied: "mp3",
			wmode: "window",
			cssSelectorAncestor: "#" + containerid,
		});

		// player.jPlayer("play");
	});
});
