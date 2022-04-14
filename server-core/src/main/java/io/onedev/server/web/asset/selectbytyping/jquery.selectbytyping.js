/*
 * 2022, Robin Shen
 */
(function ( $ ) {
 
    $.fn.selectByTyping = function(container) {
    	var $input = jQuery(this);

		function onReturn() {
			$(container).find(".active.selectable").find("a").addBack("a").click();
		}
		
		function onKeyup(e) {
			var $container = $(container);
			e.preventDefault();
			var $active = $container.find(".active.selectable");
			var $selectables = $container.find(".selectable");
			var index = $selectables.index($active);
			if (index > 0) {
				index--;
				var $prev = $selectables.eq(index);
				$active.removeClass("active");
				$prev.addClass("active");
				$prev.scrollIntoView();
			}
		};
		
		function onKeydown(e) {
			var $container = $(container);
			e.preventDefault();
			var $active = $container.find(".active.selectable");
			var $selectables = $container.find(".selectable");
			var index = $selectables.index($active);
			if (index < $selectables.length-1) {
				index++;
				var $next = $selectables.eq(index);
				$active.removeClass("active");
				$next.addClass("active");
				$next.scrollIntoView();
			}
		};
		
		$input.bind("keydown", "return", onReturn)
				.bind("keydown", "up", onKeyup)
				.bind("keydown", "down", onKeydown);		    	
		
    	return this;
    };
 
}( jQuery ));
