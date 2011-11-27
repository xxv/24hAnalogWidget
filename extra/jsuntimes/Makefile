stripped_jar: jsuntimes-1.0.jar
	mkdir -p $@
	unzip -d $@ $<
	find $@ -type f -not -name \*.class -print0 | xargs -0 rm
	rm -rf $@/doc
	jar cvf jsuntimes-1.0.stripped.jar -C $@ .
	rm -rf $@
