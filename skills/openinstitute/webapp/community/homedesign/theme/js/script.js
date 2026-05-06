AOS.init({
	duration: 600,
	easing: "ease-in-out",
	once: true,
	mirror: false,
});

$(".faq-item h3, .faq-item .faq-toggle").click(function () {
	var isActive = $(this).parent().hasClass("faq-active");
	$(".faq-item").removeClass("faq-active");
	if (!isActive) {
		$(this).parent().toggleClass("faq-active");
	}
});

lQuery(".video-card").livequery("click", function () {
	var href = $(this).data("href");
	window.location.href = href;
});
