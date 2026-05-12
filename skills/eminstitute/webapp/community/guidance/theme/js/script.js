AOS.init({
	duration: 600,
	easing: "ease-in-out",
	once: true,
	mirror: false,
});

$(document).ready(function () {
	if ($("#projectCarousel").length > 0) {
		new bootstrap.Carousel("#projectCarousel");
	}
});
