# MiniJava Semantic Analyzer - build script
#
#   make          generate the parser (JTB + JavaCC), compile everything, build the jar
#   make test     build and run the full regression suite
#   make run FILES="a.java b.java"
#   make clean    remove every generated file

JAVA      ?= java
JAVAC     ?= javac
JAR_TOOL  ?= jar
RELEASE   ?= 11

BUILD_DIR := build
GEN_DIR   := $(BUILD_DIR)/generated
CLASS_DIR := $(BUILD_DIR)/classes
JAR       := $(BUILD_DIR)/minijava-checker.jar

GRAMMAR   := grammar/minijava.jj
JTB       := lib/jtb133di.jar
JAVACC    := lib/javacc5.jar

SOURCES   := $(shell find src -name '*.java')

GEN_STAMP   := $(GEN_DIR)/.generated
CLASS_STAMP := $(CLASS_DIR)/.compiled

.PHONY: all test run clean

all: $(JAR)

# 1. JTB annotates the grammar and emits the syntax tree + visitor classes,
#    then JavaCC turns the annotated grammar into the parser.
$(GEN_STAMP): $(GRAMMAR) $(JTB) $(JAVACC)
	@rm -rf $(GEN_DIR) && mkdir -p $(GEN_DIR)
	@cp $(GRAMMAR) $(GEN_DIR)/minijava.jj
	@echo "[jtb]    $(GRAMMAR)"
	@cd $(GEN_DIR) && $(JAVA) -jar $(CURDIR)/$(JTB) -o minijava-jtb.jj minijava.jj > jtb.log 2>&1 \
		|| (cat jtb.log; exit 1)
	@echo "[javacc] minijava-jtb.jj"
	@cd $(GEN_DIR) && $(JAVA) -jar $(CURDIR)/$(JAVACC) minijava-jtb.jj > javacc.log 2>&1 \
		|| (cat javacc.log; exit 1)
	@touch $@

# 2. Compile generated code together with the analyzer sources.
$(CLASS_STAMP): $(GEN_STAMP) $(SOURCES)
	@rm -rf $(CLASS_DIR) && mkdir -p $(CLASS_DIR)
	@echo "[javac]  $(words $(SOURCES)) source files + generated parser"
	@$(JAVAC) --release $(RELEASE) -encoding UTF-8 -nowarn -Xlint:none -d $(CLASS_DIR) \
		$$(find $(GEN_DIR) -name '*.java') $(SOURCES)
	@touch $@

# 3. Package a runnable jar:  java -jar build/minijava-checker.jar file.java ...
$(JAR): $(CLASS_STAMP)
	@echo "[jar]    $@"
	@$(JAR_TOOL) --create --file $@ --main-class Main -C $(CLASS_DIR) .

test: all
	@./tests/run_tests.sh

run: all
	@$(JAVA) -jar $(JAR) $(FILES)

clean:
	rm -rf $(BUILD_DIR)
