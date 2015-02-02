$(document).ready(function(){

	$('.search-bar-section').height(screen.height * 0.9);

    /*set page scroll navigation function */
    $('.scroll-link').on('click', function(event){
            event.preventDefault();
            var sectionID = $(this).attr("data-id");
            scrollToID('#' + sectionID, 750);
    });

	// From http://www.whatwg.org/specs/web-apps/current-work/multipage/states-of-the-type-attribute.html#e-mail-state-%28type=email%29
	emailRegex = /^[a-zA-Z0-9.!#$%&'*+\/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$/;
	emailBox = $("#email-box");

    $('#email-form').submit(function(){
        emailBox.removeClass( "field-error" );
        emailBox.focus()
        $(this).serializeArray().map(function (e){if(e.name=="email"){emailText = e.value}})
        $(this).serializeArray().map(function (e){if(e.name=="csrfmiddlewaretoken"){csrfToken = e.value}})
		var valid = checkRegexp(emailText, emailRegex);
		if ( valid ) {
            submitEmail(emailText, csrfToken);
            $('#email-error').hide();
            $("#submit-email").hide();
            $("#email-form").hide("slide", { direction: "down" }, 500);
            $("#signed-up").slideDown();
		} else {
            emailBox.addClass( "field-error" );
            $('#email-error').show();
        }
        event.preventDefault();
    })

	function checkRegexp( text, regexp) {
		if ( !( regexp.test( text ) ) ) {
			return false;
		} else {
			return true;
		}
	}

	function submitEmail(emailText, csrfToken) {
        $.ajax({
            type: "POST",
            url: "/signup/",
            data: {
                email: emailText,
                csrfmiddlewaretoken: csrfToken
            }
        }).done(function() {

        });
	}

    $("#sign-up-again").on('click', function(){
        $("#email-box").val("");
        $("#signed-up").hide();
        $("#email-form").show("slide", { direction: "up" }, 500);
        $("#submit-email").delay(600).show(0);

    })

    // scroll function
    function scrollToID(id, speed){
        var offSet = 0;
        var targetOffset = $(id).offset().top - offSet;
        var mainNav = $('#main-nav');
        $('html,body').animate({scrollTop:targetOffset}, speed);
    }

    $(window).scroll(function() {
        if($(window).scrollTop() + $(window).height() == ($(document).height())) {
            $('#scroll-to-top').show("slide", { direction: "down" }, 500);
        }
    });
});
