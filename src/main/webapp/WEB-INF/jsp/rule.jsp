<%@ include file="./common/header.jsp"%>
<%@ include file="./common/navigation.jsp"%>
<style type="text/css">
</style>
<script type="text/javascript">
var txtCnt = 1;
function addMoreText() {
	$("#checks").append("<br>");
	$("#checks").append("<input type='text' id='check"
			+ (++txtCnt) + "' size='80'/>");
}
function submitForm(e) {
	e.preventDefault();
	var txtRules = [];
	for (var i = 1; i <= txtCnt; i++) {
		var val = $("#check" + i).val();
		if (val && val.trim() !== "") {
			txtRules.push(val);
		}
	}
	$.ajax({
		url : "/rule/create",
		type : 'POST',
		contentType : "application/json",
		data : JSON.stringify({
			name: $("#name").val(),
			entity: $("#entity").val(),
			description: $("#description").val(),
			checks: txtRules
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
		<h3>Create New Rule</h3>
	</div>
	<form:form autocomplete="off">
	<table style="width: 100%;">
		<tr>
			<td>
				<label for="name">Name:</label>
			</td>
			<td>
				<input type="text" id="name" placeholder="Enter rule name..."/>
			</td>
		</tr>
		<tr>
			<td>
				<label for="entity">Entity:</label>
			</td>
			<td>
				<input type="text" id="entity" placeholder="Enter entity name..."/>
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
				<label for="checks">Set checks...</label><br/>
				<label style="font-size: smaller; font-style: italic; color: #CCCCCC;">
					Use <a href="/">query translator</a> page to prepare a check
				</label>
			</td>
			<td>
				<div id="checks">
					<input type="text" id="check1" size="80"/>
				</div>
				<input type="button" value="Add More..." onclick="addMoreText()"/>
			</td>
		</tr>
	</table>

	<div style="text-align: center">
		<input id="submit" type="button" value="Save" onclick="submitForm(event)" />
	</div>
	</form:form>
	<div class="msg">
		<label for="query">Rule Create Result:</label>
		<textarea id="output" style="width: 100%" id="query" rows="10"
			placeholder="Your Result goes here..." readonly="readonly"></textarea>
	</div>
</div>
