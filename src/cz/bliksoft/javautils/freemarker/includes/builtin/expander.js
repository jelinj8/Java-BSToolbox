function expand(identifier) {
	$(identifier).show("slow");
}

function collapse(identifier) {
	$(identifier).hide("slow");
}

function expand(identifier, srcId) {
	$(identifier).show("slow");
	$(srcId).html("&minusb;");
}

function collapse(identifier, srcId) {
	$(identifier).hide("slow");
	$(srcId).html("&plusb;");
}

function toggle_collapse(identifier) {
	if($(identifier).css('display') == 'none'){
		$(identifier).show("fast");
	}else{
		$(identifier).hide("fast");
	}
}
