.PHONY: config compile subjects dict detect cluster suggest

MIN_MEM := 4096m
MAX_MEM := 4096m
JAVA := java -XX:-UseGCOverheadLimit -Xms$(MIN_MEM) -Xmx$(MAX_MEM) -cp target/classes:target/dependency/*

config:
	vim src/main/resources/peta.properties

compile:
	mvn clean compile test-compile dependency:copy-dependencies

subjects:
	rm -rf subjects
	mkdir subjects
	cd subjects && git clone https://github.com/mzw/vtr-example
	
dict:
	$(JAVA) jp.mzw.vtr.git.DictionaryMaker &

detect:
	$(JAVA)  jp.mzw.vtr.detect.EachTestMethodRunner &
	
suggest:
	java -XX:-UseGCOverheadLimit -Xms4096m -Xmx12g -cp target/classes:target/dependency/* jp.mzw.vtr.suggest.Suggester &
	
