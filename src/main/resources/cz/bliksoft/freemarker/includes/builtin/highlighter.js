$(function() {
	$('[highlighter]').addClass('pointer-cursor');
	$('[highlighter]').click(
	  function(event) {
	  	if(event.target.classList.contains('highlighted')) {
	  		$('.highlighted').removeClass('highlighted');
	  	} else {
	  		$('.highlighted').removeClass('highlighted');
	  		$('[highlighter="'+event.target.getAttribute('highlighter')+'"]').addClass('highlighted');
	  	}
	  }
	);
});