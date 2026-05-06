$(document).ready(function () {
	lQuery(".property-container").livequery("click", function (e) {
		if (e.target.nodeName === "SPAN" || e.target.nodeName === "BUTTON") {
			return;
		}
		window.location.href = $(this).data("href");
	});

	var dateFormat = "D MMM, YYYY";

	var blockeddates = $("#checkOut").data("disableddates");
	if (!blockeddates) {
		blockeddates = "";
	}
	blockeddates = blockeddates.split(",");

	var config = {
		autoClose: true,
		singleDate: true,
		hoveringTooltip: false,
		showShortcuts: false,
		singleMonth: true,
		startDate: moment().format(dateFormat),
		format: dateFormat,
		beforeShowDay: function (t) {
			var m = moment(t.toUTCString());
			var formated = m.format("YYYY-MM-DD");
			if (blockeddates.length > 0 && blockeddates.includes(formated)) {
				var _tooltip = "This date is taken";
				return [false, "", _tooltip];
			}
			return [true, "", ""];
		},
	};

	lQuery("#checkOut").livequery(function () {
		$(this)
			.dateRangePicker(config)
			.bind("datepicker-open", function () {
				$("#gp-mask").show();
			})
			.bind("datepicker-close", function () {
				$("#gp-mask").hide();
			});
	});

	lQuery("#checkIn").livequery(function () {
		if ($(this).data("saveformat") !== undefined && $(this).val() != "") {
			var formatedval = moment(
				$(this).val(),
				$(this).data("saveformat")
			).format(dateFormat);
			$(this).val(formatedval);
		}

		$(this)
			.dateRangePicker(config)
			.bind("datepicker-change", function (event, obj) {
				var target = $(this).data("saveto");
				if (target !== undefined) {
					var formatedval = moment($(this).val(), dateFormat).format(
						$(this).data("saveformat")
					);
					$("#" + target).val(formatedval);
				}
				//$("#checkOut").  this.element.trigger('show.daterangepicker', this);
				// $("#checkOut").data("dateRangePicker").destroy();

				config.startDate = moment($(this).val(), dateFormat)
					.add(1, "days")
					.format(dateFormat);

				// $("#checkOut").dateRangePicker(config);
				if (
					moment($(this).val(), dateFormat).isAfter(
						moment($("#checkOut").val(), dateFormat)
					)
				) {
					$("#checkOut").val(config.startDate);
				}
				config.startDate = moment().format(dateFormat);
			})
			.bind("datepicker-open", function () {
				$("#gp-mask").show();
			})
			.bind("datepicker-close", function () {
				$("#gp-mask").hide();
			});
		var val = $(this).val();
		if (val.includes("-")) {
			$(this).val(moment(val).format(dateFormat));
		}
	});

	lQuery("form[name='startinvoice']").livequery("submit", function (e) {
		e.preventDefault();

		var checkIn = moment($("#checkIn").val(), dateFormat).format("YYYY-MM-DD");
		var checkOut = moment($("#checkOut").val(), dateFormat).format(
			"YYYY-MM-DD"
		);

		var form = $(this).get(0);
		form.checkIn.value = checkIn;
		form.checkOut.value = checkOut;
		form.submit();
	});

	$("#guests").click(function () {
		$("#guestsPicker").addClass("open");
		$("#gp-mask").show();
	});

	$("#gp-mask").click(function () {
		$("#guestsPicker").removeClass("open");
		$("#gp-mask").hide();
	});

	var curGuestVal = {
		guest: 1,
		pet: 0,
	};

	function updateGuests(gt, f = 1) {
		if (gt == "adult" || gt == "children") {
			curGuestVal.guest += f;
		} else if (gt == "pet") {
			curGuestVal.pet += f;
		}

		var guestStr = curGuestVal.guest + "" + (curGuestVal.guest > 1 ? "" : "");
		if (curGuestVal.pet > 0) {
			guestStr += ", " + curGuestVal.pet + "" + (curGuestVal.pet > 1 ? "" : "");
		}

		$("#guests").val(guestStr);
	}

	$(".g-plus").click(function () {
		var gt = $(this).data("gt");
		var val = parseInt($(this).siblings("strong").text());
		if (val <= 6) {
			val++;
			$(this).siblings("strong").text(val);
			updateGuests(gt);
		}
	});
	$(".g-minus").click(function () {
		var gt = $(this).data("gt");

		var val = parseInt($(this).siblings("strong").text());
		var min = 0;
		if (gt == "adult") {
			min = 1;
		}
		if (val > min) {
			val--;
			$(this).siblings("strong").text(val);
			updateGuests(gt, -1);
		}
	});
	$(".previewImg").click(function () {
		$("#carouselMax").carousel($(this).data("index"));
		$("#triggerPreview").trigger("click");
	});
	$("#shareProperty").click(function () {
		var url = window.location.href;
		var dummy = $("<input>").val(url).appendTo("body").select();
		document.execCommand("copy");
		dummy.remove();
		$(this).addClass("text-success");
		$(this).find("span").text("Link copied");
		$(this).find("i").removeClass("fa-share-alt").addClass("fa-check");
		setTimeout(() => {
			$(this).find("i").removeClass("fa-check").addClass("fa-share-alt");
			$(this).removeClass("text-success");
			$(this).find("span").text("Share");
		}, 2000);
	});

	$(".owner-btn").click(function (e) {
		e.stopPropagation();
		var reviews = $(this).data("reviews");
		var rating = $(this).data("rating");
		var rank = $(this).data("rank");
		var duration = $(this).data("duration");
		var location = $(this).data("location");
		var description = $(this).data("description");
		var name = $(this).data("name");
		var img = $(this).find("img").attr("src");

		var modelEl = $("#ownerPreview");
		modelEl.find(".reviews strong").text(reviews);
		modelEl.find(".rating strong").text(rating);
		modelEl.find(".duration strong").text(duration);
		modelEl.find(".location").text(location);
		modelEl.find(".description").text(description);
		modelEl.find(".name").text(name);
		modelEl.find(".rank").text(rank);
		modelEl.find("img").attr("src", img);

		var modal = new bootstrap.Modal(modelEl);
		modal.show();
	});
	$(".btn-close").click(function () {
		var modal = bootstrap.Modal.getInstance(
			document.querySelector("#ownerPreview")
		);
		modal.hide();
	});
});
