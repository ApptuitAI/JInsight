# Copyright 2017 Agilx, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

ifeq ($(PACKAGE_VERSION),)
    PACKAGE_VERSION := 1.0.0
    PACKAGE_REVISION := 0.1.SNAPSHOT
    JAR_VERSION := 1.0.0-SNAPSHOT
else
    JAR_VERSION := $(PACKAGE_VERSION)
    PACKAGE_REVISION := 1
endif

# Our "source tree"
REPO_ROOT := ../..
WORK_DIR := $(REPO_ROOT)/pkg/build/deb
DEST_DIR := $(REPO_ROOT)/pkg/dist

# What / where do we build.
DEB_TARGET := all
BASENAME := jinsight_$(PACKAGE_VERSION)-$(PACKAGE_REVISION)
DEB := $(BASENAME)_$(DEB_TARGET).deb

all: clean deb

deb:
	mkdir -p $(DEST_DIR)/
	mkdir -p $(WORK_DIR)/DEBIAN/
	mkdir -p $(WORK_DIR)/usr/share/java/
	mkdir -p $(WORK_DIR)/etc/jinsight/

	chmod -R 755 $(WORK_DIR)/*

	install -m 644 $(REPO_ROOT)/target/jinsight-$(JAR_VERSION).jar $(WORK_DIR)/usr/share/java/
	ln -rs $(WORK_DIR)/usr/share/java/jinsight-$(JAR_VERSION).jar $(WORK_DIR)/usr/share/java/jinsight.jar
	install -m 644 $(REPO_ROOT)/pkg/common/jinsight-config.properties $(WORK_DIR)/etc/jinsight

	install -m 644 DEBIAN/conffiles $(WORK_DIR)/DEBIAN/conffiles
	set -e; { \
	  sed \
            -e 's/@PACKAGE_VERSION@/$(PACKAGE_VERSION)/' \
            -e 's/@DEB_TARGET@/$(DEB_TARGET)/' \
            -e 's/@PACKAGE_REVISION@/$(PACKAGE_REVISION)/' \
	    DEBIAN/control; \
        } >$(WORK_DIR)/DEBIAN/control

	cd $(WORK_DIR); find . -type f ! -regex '.*?DEBIAN.*' -printf '%P ' | xargs md5sum > DEBIAN/md5sums
	chmod 644 $(WORK_DIR)/DEBIAN/md5sums

	sudo chown -R root:root $(WORK_DIR)
	dpkg-deb -b $(WORK_DIR) $(DEST_DIR)/$(DEB)

lint:
	lintian --no-tag-display-limit $(DEST_DIR)/$(DEB)

clean:
	sudo rm -rf $(WORK_DIR) $(DEST_DIR)/*.deb

.PHONY: all deb clean
.SUFFIXES: .deb
