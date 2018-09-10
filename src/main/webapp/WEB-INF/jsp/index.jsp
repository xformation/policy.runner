<%@ include file="./common/header.jsp"%>
<%@ include file="./common/navigation.jsp"%>
<div class="container">
	<div style="text-align: center">
		<h3>Welcome Query Parsing</h3>
	</div>
	<div>
		<form:form>
			<p>
				<label for="query">Query:</label>
				<textarea id="query" style="width: 100%" id="query" rows="3"
					placeholder="Enter your query string..."></textarea>
			</p>
			<div style="text-align: center">
				<input id="submit" type="button" value="Translate"
					onclick="submitForm(event)" />
			</div>
		</form:form>
	</div>
	<div class="msg">
		<label for="query">Elasticsearch DSL query:</label>
		<textarea id="output" style="width: 100%" id="query" rows="10"
			placeholder="Your translation goes here..." readonly="readonly"></textarea>
	</div>
</div>

<script>
	function submitForm(e) {
		e.preventDefault();
		$.ajax({
			url : "/translate",
			type : 'POST',
			contentType : "application/json",
			data : JSON.stringify({
				"query" : $("#query").val()
			}),
			dataType : 'json',
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

<%@ include file="./common/footer.jsp"%>
