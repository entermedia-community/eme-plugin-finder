// File for your custom JavaScript

$(document).ready(function () {
  new HSHeader($("#header")).init();
  $(".js-slick-carousel").each(function () {
    $.HSCore.components.HSSlickCarousel.init($(this));
  });
  $(".js-go-to").each(function () {
    new HSGoTo($(this)).init();
  });
  $(".js-validate").each(function () {
    $.HSCore.components.HSValidation.init($(this));
  });
  $(".up-login").click(function () {
    $(this).parent().parent().find(".collapse").toggle(); // toggle collapse
  });

  AOS.init({
    duration: 600,
    once: true,
  });
});
