<%@ include file="./common/header.jsp"%>
<%@ include file="./common/navigation.jsp"%>
<style type="text/css">
	#suggesstions {
		float: left;
		list-style: none;
		margin-top: -3px;
		padding: 0;
		position: absolute;
	}
	
	#suggesstions li {
		padding: 10px;
		background: #f0f0f0;
		border-bottom: #bbb9b9 1px solid;
	}
	
	#suggesstions li:hover {
		background: #ece3d2;
		cursor: pointer;
	}
</style>
<div class="container">
	<div style="text-align: center">
		<h3>Welcome Query Translator</h3>
	</div>
	<div>
		<form:form autocomplete="off">
			<div>
				<label for="query">Query:</label>
				<input type="text" id="query"
					style="width: 100%" placeholder="Enter your query string..."/>
				<div id="suggesstion-box"></div>
			</div>

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
	$(document).ready(
		function() {
			$("#query").keyup(
				function() {
					var val = getQuery($(this).val());
					if (val && (val === "" || val.length < 2)) {
						return;
					}
					$("#query").css("background",
							"#FFF url(images/loaderIcon.gif) no-repeat right center");
					$.ajax({
						type : "POST",
						url : "/suggestKey",
						data : 'query=' + val,
						success : function(data) {
							setTimeout(function() {
								$("#suggesstion-box").show();
								setSuggestions(data);
								$("#query").css("background", "#FFF");
							}, 1000);
						}
					});
				});
		});

	function getQuery(val) {
		var res = val.trim();
		if (val && val.length > 0) {
			var arr = val.split(" ");
			if (arr) {
				var len = arr.length;
				if (len > 1) {
					var cur = arr[len - 1];
					var prev = arr[len - 2].toLowerCase();
					if (prev && prev.indexOf(",") === (prev.length - 1)){
						for (var i = len - 2; i >=0; i --) {
							var pVal = arr[i];
							if (pVal && pVal.indexOf("[") == 0) {
								res = cur;
								break;
							} else if (pVal.indexOf(",") === (pVal.length - 1)) {
								continue;
							} else {
								break;
							}
						}
					} else {
						switch(prev) {
						case 'has':
						case 'and':
						case 'or':
						case '[':
							res = cur;
							break;
						default:
							res = "";
						}
					}
				}
			}
		}
		if (res && res !== "" &&
				(res.indexOf("[") == 0 || res.indexOf("(") == 0)) {
			res = res.substring(1);
		}
		return res;
	}
	function setSuggestions(data) {
		if (data && Array.isArray(data)) {
			var html = "<ul id='suggesstions'>";
			for (var i = 0; i < data.length; i++) {
				var key = data[i];
				html += "<li onclick=\"selectSuggestion('" + key + "')\">" + key + "</li>";
			}
			html += "</ul>";
			$("#suggesstion-box").html(html);
		}
	}
	function selectSuggestion(val) {
		var prev = $("#query").val().trim();
		var res = getQuery(prev);
		var sel = prev.replace(new RegExp(res + '$'), val);
		$("#query").val(sel);
		$("#suggesstion-box").hide();
	}
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
