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
					$.each(data, function(i, item) {
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
	<table style="width: 100%;">
		<tr>
			<td>
				<label for="name">Name:</label>
			</td>
			<td>
				<input type="text" id="name" placeholder="Enter policy name..."/>
			</td>
		</tr>
		<tr>
			<td>
				<label for="description">Description:</label>
			</td>
			<td>
				<input type="text" id="description" placeholder="Enter description..."/>
			</td>
		</tr>
		<tr>
			<td>
				<label for="rules">Select Rules...</label>
			</td>
			<td>
				<select id="rules" size="5" multiple="multiple">
				</select>
			</td>
		</tr>
	</table>

	<div style="text-align: center">
		<input id="submit" type="button" value="Save" onclick="submitForm(event)" />
	</div>
	</form:form>
	<div class="msg">
		<label for="query">Policy Create Result:</label>
		<textarea id="output" style="width: 100%;" id="query" rows="3"
			placeholder="Your Result goes here..." readonly="readonly"></textarea>
	</div>
</div>
