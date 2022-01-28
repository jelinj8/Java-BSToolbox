function loadURI(uri) {
	function okResponse() {
		   console.log(this.responseText);//should be return value of 1
		}

	function errorResponse() {
		   console.log('Call failed. Is the apropriate environment running?\n' + this.responseText);
		   window.alert('Call failed. Is the apropriate environment running?');
		}

	var oReq = new XMLHttpRequest();
	oReq.onload = okResponse;
	oReq.onerror = errorResponse;
	oReq.open("get", uri, true);
	oReq.send();
}