$(document).ready(function(){

  $('#submit_button').on('click',
    {  },
    function(eventObject) {

	    $.ajax({

	      url: '/login',
	      type: 'GET',
	      // username: 'admin',
	      // password: $('#auth_field').val(),

          headers: {
		    'Authorization': 'Basic ' + btoa('admin:' + $('#auth_field').val())
		  },

	      success: function() {
      		window.location.href = '/index.html';
	      },

	      error: function(xhr, status, errorThrown) {
	      	$('status').html('Error logging in.');
	      }
	    });

    });

});