<%@ include file="./common/header.jsp"%>
<%@ include file="./common/navigation.jsp"%>
<style type="text/css">
</style>
<script type="text/javascript">
var txtCnt = 1;
function addMoreText() {
	$("#checks").append("<br><input type='text' id='check" + (++txtCnt) + "'/>");
}
function submitForm(e) {
	e.preventDefault();
	var selVal = $("#selPolicy").val();
	$.ajax({
		url : "/rule/create",
		type : 'POST',
		contentType : "application/json",
		data : JSON.stringify({
			name: $("#name").val(),
			entity: $("#entity").val(),
			description: $("#description").val(),
			rules: txtRules
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
		<h3>Policy Executor</h3>
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
				<label for="entity">Entity:</label>
				<input type="text" id="entity" placeholder="Enter entity name..."/>
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
				<label for="checks">Set checks...</label>
				<div id="checks">
					<input type="text" id="check1"/>
				</div>
				<input type="button" value="Add More..."/>
			</td>
		</tr>
	</table>

	<div style="text-align: center">
		<input id="submit" type="button" value="Translate"
			onclick="submitForm(event)" />
	</div>
	</form:form>
	<div class="msg">
		<label for="query">Rule Create Result:</label>
		<textarea id="output" style="width: 100%" id="query" rows="10"
			placeholder="Your Result goes here..." readonly="readonly"></textarea>
	</div>
</div>
