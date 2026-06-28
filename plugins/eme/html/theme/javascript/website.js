$(function () {
	$("#filterChips").on("click", ".chip", function () {
		const $chip = $(this);
		$("#filterChips .chip").removeClass("active");
		$chip.addClass("active");
	});

	$("#searchInput").on("input", function () {});
});
