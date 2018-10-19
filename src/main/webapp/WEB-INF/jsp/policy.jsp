<%@ include file="./common/header.jsp"%>
<%@ include file="./common/navigation.jsp"%>
<style type="text/css">
</style>
<script type="text/javascript">
$(document).ready(
	function() {
		$.ajax({
			type : "GET",
			url : "/rule/listAll",
			success : function(data) {
				setTimeout(function() {
					var select = $('#rules');
					$.each(data.values, function(i, item) {
						var rule = "{ id: '" + item.id + "'}";
						select.append("<option value='"
								+ rule + "'>" + item.name + "</option>");
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
		url : "/policy/create",
		type : 'POST',
		contentType : "application/json",
		data : JSON.stringify({
			name: $("#name").val(),
			description: $("#description").val(),
			rules: $("#rules").val()
		}),
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
		<h3>Create New Policy</h3>
	</div>
	<form:form autocomplete="off">
	<table>
		<tr>
			<td>
				<label for="name">Name:</label>
				<input type="text" id="name" placeholder="Enter your full name..."/>
			</td>
		</tr>
		<tr>
			<td>
				<label for="description">Description:</label>
				<input type="text" id="description" placeholder="Enter description..."/>
			</td>
		</tr>
		<tr>
			<td>
				<label for="rules">Select Rules...</label>
				<select id="rules" size="5" multiple="multiple">
				</select>
			</td>
		</tr>
	</table>

	<div style="text-align: center">
		<input id="submit" type="button" value="Save"
			onclick="submitForm(event)" />
	</div>
	</form:form>
	<div class="msg">
		<label for="query">Policy Create Result:</label>
		<textarea id="output" style="width: 100%;" id="query" rows="3"
			placeholder="Your Result goes here..." readonly="readonly"></textarea>
	</div>
</div>
