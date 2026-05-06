var canvas = document.createElement("canvas");
var ctx = canvas.getContext("2d");
var gradient = ctx.createLinearGradient(0, 0, 0, canvas.height * 1.35);
gradient.addColorStop(0, "#656666");
gradient.addColorStop((canvas.height * 0.7) / canvas.height, "#656666");
gradient.addColorStop((canvas.height * 0.7 + 1) / canvas.height, "#ffffff");
gradient.addColorStop((canvas.height * 0.7 + 2) / canvas.height, "#ffffff");
gradient.addColorStop((canvas.height * 0.7 + 3) / canvas.height, "#B1B1B1");
gradient.addColorStop(1, "#B1B1B1");

var progressGradient = ctx.createLinearGradient(0, 0, 0, canvas.height * 1.35);
progressGradient.addColorStop(0, "#EE772F");
progressGradient.addColorStop((canvas.height * 0.7) / canvas.height, "#EB4926");
progressGradient.addColorStop(
	(canvas.height * 0.7 + 1) / canvas.height,
	"#ffffff"
);
progressGradient.addColorStop(
	(canvas.height * 0.7 + 2) / canvas.height,
	"#ffffff"
);
progressGradient.addColorStop(
	(canvas.height * 0.7 + 3) / canvas.height,
	"#F6B094"
);
progressGradient.addColorStop(1, "#F6B094");

var wavesurfer = WaveSurfer.create({
	container: "#waveform",
	waveColor: gradient,
	progressColor: progressGradient,
	barWidth: 3,
	barRadius: 3,
	barGap: 1,
	height: 80,
	minPxPerSec: 50,
	hideScrollbar: true,
});

var formatTime = (seconds) => {
	var minutes = Math.floor(seconds / 60);
	var secondsRemainder = Math.round(seconds) % 60;
	var paddedSeconds = `0${secondsRemainder}`.slice(-2);
	return `${minutes}:${paddedSeconds}`;
};

$(document).ready(function () {
	$("#waveform").on("pointermove", (e) => $("#waveformhover").width(e.offsetX));
	wavesurfer.on("decode", (duration) =>
		$("#waveformduration").text(formatTime(duration))
	);
	wavesurfer.on("timeupdate", (currentTime) =>
		$("#waveformtime").text(formatTime(currentTime))
	);

	function playWaveSurfer(id = null) {
		wavesurfer.play();
		$("#waveformart").addClass("playing");
		$("#waveformcontainer").fadeIn("fast", function () {
			document.body.style.paddingBottom = "96px";
			if (id) $("#" + id).addClass("playing");
		});
	}
	function stopWaveSurfer(id = null) {
		wavesurfer.pause();
		$("#waveformart").removeClass("playing");
		if (id) {
			$("#" + id).removeClass("playing");
			return;
		}
		$("#waveformcontainer").fadeOut("fast", function () {
			document.body.style.paddingBottom = "0px";
			$(".song-controls").removeClass("playing");
		});
	}

	wavesurfer.on("finish", function () {
		stopWaveSurfer();
	});

	lQuery(".video-card").livequery("click", function () {
		var href = $(this).data("href");
		window.location.href = href;
	});

	lQuery(".song-card").livequery("click", function (e) {
		e.preventDefault();
		e.stopPropagation();
		var href = $(this).data("href");
		if (!href) return;
		var target = $(e.target);
		if (target.hasClass(".song-controls")) {
			return;
		} else if (target.closest(".song-controls").length) {
			return;
		}
		var anchor = document.createElement("a");
		anchor.href = href;
		document.body.appendChild(anchor);
		$(anchor).data("targetdivinner", "pageContent");
		$(anchor).data("oemaxlevel", "1");
		$(anchor).data("updateurl", "true");
		$(anchor).runAjax();
		anchor.remove();
	});

	lQuery(".song-controls").livequery("click", function () {
		var src = $(this).data("src");
		var songCard = $(this).closest(".song-card");
		var title = songCard.find(".card-title").text();
		var artist = songCard.find(".card-subtitle").text();
		var art = songCard.find(".album-art img").clone();
		var isPlaying = $(this).hasClass("playing");
		if (wavesurfer.isPlaying()) {
			wavesurfer.pause();
		}
		if (isPlaying) {
			$(this).removeClass("playing").addClass("paused");
			$("#waveformart").removeClass("playing");
		} else {
			if ($(this).hasClass("paused")) {
				$(this).removeClass("paused");
				playWaveSurfer();
			} else {
				wavesurfer.load(src).then(function () {
					playWaveSurfer();
				});
			}
			$("#waveformcontainer").data("id", $(this).attr("id"));
			$("#waveformalbum").find("img").replaceWith(art);
			$("#waveformalbum").find("h5").text(title);
			$("#waveformalbum").find("p").text(artist);
			$(".song-controls").removeClass("playing");
			$(this).addClass("playing");
		}
	});

	lQuery("#song-list").livequery(function () {
		var id = $("#waveformcontainer").data("id");
		if (wavesurfer.isPlaying() && id) {
			$("#" + id).addClass("playing");
		}
	});
	lQuery("#song-expanded").livequery(function () {
		var id = $("#waveformcontainer").data("id");
		if (wavesurfer.isPlaying() && id) {
			$("#" + id).addClass("playing");
		}
	});

	lQuery("#waveformclose").livequery("click", function () {
		stopWaveSurfer();
	});

	lQuery("#waveformart").livequery("click", function () {
		var id = $("#waveformcontainer").data("id");
		if (id) {
			$("#" + id).addClass("playing");
		}
		if (wavesurfer.isPlaying()) {
			stopWaveSurfer(id);
		} else {
			playWaveSurfer(id);
		}
	});
});
