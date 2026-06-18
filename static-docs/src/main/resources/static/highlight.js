document.querySelectorAll("pre code").forEach((block) => {
	block.innerHTML = block.innerHTML
		.replace(/\b(public|private|protected|class|record|interface|enum|static|final|void|return|new|if|else|for|while|import|package)\b/g, "<span class=\"tok-kw\">$1</span>")
		.replace(/(&quot;[^&]*?&quot;)/g, "<span class=\"tok-str\">$1</span>");
});
