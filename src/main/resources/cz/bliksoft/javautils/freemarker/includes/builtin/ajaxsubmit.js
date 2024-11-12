$(() => {
	/* attach a submit handler to the form */
	$(".ajaxsubmit").submit(function(event) {

		/* stop form from submitting normally */
		event.preventDefault();

		/* get the action attribute from the <form action=""> element */
		var form = $(this);
		var url = form.attr('action');

		$.ajax({
			type: "POST",
			url: url,
			data: form.serialize(),
			success: function(data) {
				console.log('POST OK');
			},
			error: function(data) {
				console.log('POST ERROR');
				alert("POST ERROR");
			}
		});
	});
});