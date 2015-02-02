/**
 * Functions for handling a search of financial reports
 */
$(document).ready(function(){
	$('#refleco-search').submit(function(){
		$.ajax({
			url: '/search/',
			type: 'POST',
			dataType: 'html',
			data: {
                csrfmiddlewaretoken: $("input[name=csrfmiddlewaretoken]").val(),
                queryText: $('#refleco-search-box').val()
            },
			success: searchSuccess
		});
		return false;
	});


});
function searchSuccess (data){
	$('#refleco-search-results').replaceWith(data);
     window.location.hash = $('refleco-search-box').id();
}
$(document).on("click", ".nav-tab", function(){
    $('.nav-tab.active').addClass('inactive');
    $('.nav-tab.active').removeClass('active');
    $(this).addClass('active');
    $(this).removeClass('inactive');
    if ($(this).hasClass('balance')){
        $('.statement-result.active').addClass('inactive');
        $('.statement-result.active').removeClass('active');
        $('.statement-result.balance').addClass('active');
        $('.statement-result.balance').removeClass('inactive');
    };
    if ($(this).hasClass('income')){
        $('.statement-result.active').addClass('inactive');
        $('.statement-result.active').removeClass('active');
        $('.statement-result.income').addClass('active');
        $('.statement-result.income').removeClass('inactive');
    };
    if ($(this).hasClass('cash')){
        $('.statement-result.active').addClass('inactive');
        $('.statement-result.active').removeClass('active');
        $('.statement-result.cash').addClass('active');
        $('.statement-result.cash').removeClass('inactive');
    };
});