require('./wdk');

(function($) {
  $(wdk.init);
  $(wdk.user.init);
  $(wdk.favorite.init);

  if (/showApplication\.do/.test(location.pathname)) {
    $(wdk.step.init);
    $(wdk.strategy.controller.init);
    $(wdk.wordCloud.init);
  }
})(jQuery);
