$(document).ready(function() {
	console.log("doc ready");
	$('body').on('click', '.dropdown', function() {
		console.log("dropdown was clicked");
		$(this).toggleClass('active');
	});
});