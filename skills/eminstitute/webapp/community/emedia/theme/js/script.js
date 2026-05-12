$(document).ready(function () {
  $(".prop-pse .nav-link").on("click", function (e) {
    e.preventDefault();
    $(this).tab("show");
    var target = $(this).data("target");
    var propPse = $(this).closest(".prop-pse");
    if (target === "problem") {
      propPse.removeClass("emp");
      propPse.addClass("prb");
    } else {
      propPse.removeClass("prb");
      propPse.addClass("emp");
    }
  });

  AOS.init({
    duration: 600,
    once: true,
  });
});
