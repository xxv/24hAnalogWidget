stripped_jar: jsuntimes-1.0.jar
	rm -rf $@
	mkdir -p $@
	unzip -d $@ $<
	find $@ -type f -not -name \*.class -print0 | xargs -0 rm
	rm -rf $@/doc
	rm $@/uk/me/jstott/sun/SunTimesPanel.class
	rm $@/uk/me/jstott/sun/SunTimesApplet.class
	rm $@/uk/me/jstott/sun/SunTimesApp.class
	rm $@/uk/me/jstott/sun/SunTimesPanel\$$1.class
	rm -rf $@/uk/me/jstott/sun/junit
	jar cvf jsuntimes-1.0.stripped.jar -C $@ .
	rm -rf $@
