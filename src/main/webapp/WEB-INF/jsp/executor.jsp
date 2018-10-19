<%@ include file="./common/header.jsp"%>
<%@ include file="./common/navigation.jsp"%>
<style type="text/css">
</style>
<script type="text/javascript">
$(document).ready(
	function() {
		$.ajax({
			type : "GET",
			url : "/policy/listAll",
			success : function(data) {
				setTimeout(function() {
					var select = $('#selPolicy');
					$.each(data.values, function(i, item) {
						select.append("<option value='"
								+ item.id + "'>" + item.name + "</option>");
					});
				}, 1000);
			}
		});
	}
);
function submitForm(e) {
	e.preventDefault();
	var selVal = $("#selPolicy").val();
	$.ajax({
		url : "/execute",
		type : 'POST',
		contentType : "application/json",
		data : "policyId=" + selVal,
		success : function(json) {
			var pretty = JSON.stringify(json, undefined, 4);
			$("#output").val(pretty);
		},
		error : function(err) {
			alert('error', err);
		}
	});
}
</script>
<div class="container">
	<div style="text-align: center">
		<h3>Policy Executor</h3>
	</div>
	<form:form autocomplete="off">
	<div>
		<select id="selPolicy">
			<option value="" selected="selected">Select A Policy...</option>
		</select>
	</div>

	<div style="text-align: center">
		<input id="submit" type="button" value="Translate"
			onclick="submitForm(event)" />
	</div>
	</form:form>
	<div class="msg">
		<label for="query">Policy Run Result:</label>
		<textarea id="output" style="width: 100%" id="query" rows="10"
			placeholder="Your Result goes here..." readonly="readonly"></textarea>
	</div>
</div>
