$(function() {
	$('[highlighter]').addClass('pointer-cursor');
	$('[highlighter]').click(
	  function(event) {
	  	$('.highlighted').removeClass('highlighted');
	  	$('[highlighter="'+event.target.getAttribute('highlighter')+'"]').addClass('highlighted');
	  }
	);
});